/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
@file:Suppress("removal")

package app.campfire.config

import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64
import java.util.Properties
import java.util.Timer
import java.util.TimerTask
import java.util.TreeMap
import java.util.prefs.AbstractPreferences
import java.util.prefs.BackingStoreException
import java.util.prefs.InvalidPreferencesFormatException
import java.util.prefs.Preferences
import kotlin.concurrent.Volatile
import kotlin.math.max

internal interface PlatformLogger {
  fun warning(message: String)
  fun info(message: String)
}

internal object BarkPlatformLogger : PlatformLogger {
  override fun warning(message: String) {
    bark(LogPriority.WARN, "FileSystemPreferences") { message }
  }

  override fun info(message: String) {
    bark(LogPriority.INFO, "FileSystemPreferences") { message }
  }
}

internal interface Serializer {
  @Throws(InvalidPreferencesFormatException::class)
  fun read(fis: FileInputStream, map: MutableMap<String, String>)

  @Throws(IOException::class)
  fun write(fos: FileOutputStream, map: Map<String, String>)
}

internal object PropertiesSerializer : Serializer {

  override fun read(fis: FileInputStream, map: MutableMap<String, String>) {
    val props = Properties().apply {
      load(fis)
    }
    props.keys.map { key ->
      map[key.toString()] = props.getProperty(key.toString())
    }
  }

  override fun write(fos: FileOutputStream, map: Map<String, String>) {
    Properties().apply {
      putAll(map)
      store(fos, null)
    }
  }
}

data class NodeConfig(
  val fileName: String,
  val applicationDir: String,
)

/**
 * Preferences implementation for Unix.  Preferences are stored in the file
 * system, with one directory per preferences node.  All of the preferences
 * at each node are stored in a single file.  Atomic file system operations
 * (e.g. File.renameTo) are used to ensure integrity.  An in-memory cache of
 * the "explored" portion of the tree is maintained for performance, and
 * written back to the disk periodically.  File-locking is used to ensure
 * reasonable behavior when multiple VMs are running at the same time.
 * (The file lock is obtained only for sync(), flush() and removeNode().)
 *
 * @author Josh Bloch
 * @see Preferences
 *
 * @since 1.4
 */
internal class FileSystemPreferences : AbstractPreferences {
  /**
   * The directory representing this preference node.  There is no guarantee
   * that this directory exits, as another VM can delete it at any time
   * that it (the other VM) holds the file-lock.  While the root node cannot
   * be deleted, it may not yet have been created, or the underlying
   * directory could have been deleted accidentally.
   */
  private val dir: File?

  /**
   * The file representing this preference node's preferences.
   * The file format is undocumented, and subject to change
   * from release to release, but I'm sure that you can figure
   * it out if you try real hard.
   */
  private val prefsFile: File

  /**
   * A temporary file used for saving changes to preferences.  As part of
   * the sync operation, changes are first saved into this file, and then
   * atomically renamed to prefsFile.  This results in an atomic state
   * change from one valid set of preferences to another.  The
   * the file-lock is held for the duration of this transformation.
   */
  private val tmpFile: File

  /**
   * Locally cached preferences for this node (includes uncommitted
   * changes).  This map is initialized with from disk when the first get or
   * put operation occurs on this node.  It is synchronized with the
   * corresponding disk file (prefsFile) by the sync operation.  The initial
   * value is read *without* acquiring the file-lock.
   */
  private var prefsCache: MutableMap<String, String>? = null

  /**
   * The last modification time of the file backing this node at the time
   * that prefCache was last synchronized (or initially read).  This
   * value is set *before* reading the file, so it's conservative; the
   * actual timestamp could be (slightly) higher.  A value of zero indicates
   * that we were unable to initialize prefsCache from the disk, or
   * have not yet attempted to do so.  (If prefsCache is non-null, it
   * indicates the former; if it's null, the latter.)
   */
  private var lastSyncTime: Long = 0

  /**
   * A list of all uncommitted preference changes.  The elements in this
   * list are of type PrefChange.  If this node is concurrently modified on
   * disk by another VM, the two sets of changes are merged when this node
   * is sync'ed by overwriting our prefsCache with the preference map last
   * written out to disk (by the other VM), and then replaying this change
   * log against that map.  The resulting map is then written back
   * to the disk.
   */
  val changeLog: MutableList<Change?> = ArrayList()

  /**
   * Represents a change to a preference.
   */
  abstract inner class Change {
    /**
     * Reapplies the change to prefsCache.
     */
    abstract fun replay()
  }

  /**
   * Represents a preference put.
   */
  private inner class Put(var key: String, var value: String) : Change() {
    override fun replay() {
      prefsCache!![key] = value
    }
  }

  /**
   * Represents a preference remove.
   */
  private inner class Remove(var key: String) : Change() {
    override fun replay() {
      prefsCache!!.remove(key)
    }
  }

  /**
   * Represents the creation of this node.
   */
  inner class NodeCreate : Change() {
    /**
     * Performs no action, but the presence of this object in changeLog
     * will force the node and its ancestors to be made permanent at the
     * next sync.
     */
    override fun replay() {
    }
  }

  /**
   * NodeCreate object for this node.
   */
  var nodeCreate: NodeCreate? = null

  /**
   * Replay changeLog against prefsCache.
   */
  private fun replayChanges() {
    var i: Int = 0
    val n: Int = changeLog.size
    while (i < n) {
      changeLog[i]!!.replay()
      i++
    }
  }

  private val isUserNode: Boolean

  private val config: NodeConfig

  /**
   * Special constructor for roots (both user and system).  This constructor
   * will only be called twice, by the static initializer.
   */
  private constructor(user: Boolean, nodeConfig: NodeConfig) : super(null, "") {
    isUserNode = user
    config = nodeConfig
    dir = (if (user) userRootDir else systemRootDir)
    prefsFile = File(dir, config.fileName)
    tmpFile = File(dir, "${config.fileName}.tmp")
  }

  /**
   * Construct a new FileSystemPreferences instance with the specified
   * parent node and name.  This constructor, called from childSpi,
   * is used to make every node except for the two //roots.
   */
  private constructor(parent: FileSystemPreferences, name: String) : super(parent, name) {
    isUserNode = parent.isUserNode
    config = parent.config
    dir = File(parent.dir, dirName(name))
    prefsFile = File(dir, config.fileName)
    tmpFile = File(dir, "${config.fileName}.tmp")
    newNode = !dir.exists()
    if (newNode) {
      // These 2 things guarantee node will get wrtten at next flush/sync
      prefsCache = TreeMap()
      nodeCreate = NodeCreate()
      changeLog.add(nodeCreate)
    }
  }

  override fun isUserNode(): Boolean {
    return isUserNode
  }

  override fun putSpi(key: String, value: String) {
    initCacheIfNecessary()
    changeLog.add(Put(key, value))
    prefsCache!![key] = value
  }

  override fun getSpi(key: String): String {
    initCacheIfNecessary()
    return prefsCache!![key]!!
  }

  override fun removeSpi(key: String) {
    initCacheIfNecessary()
    changeLog.add(Remove(key))
    prefsCache!!.remove(key)
  }

  /**
   * Initialize prefsCache if it has yet to be initialized.  When this method
   * returns, prefsCache will be non-null.  If the data was successfully
   * read from the file, lastSyncTime will be updated.  If prefsCache was
   * null, but it was impossible to read the file (because it didn't
   * exist or for any other reason) prefsCache will be initialized to an
   * empty, modifiable Map, and lastSyncTime remain zero.
   */
  private fun initCacheIfNecessary() {
    if (prefsCache != null) return

    try {
      loadCache()
    } catch (e: Exception) {
      // assert lastSyncTime == 0;
      prefsCache = TreeMap()
    }
  }

  /**
   * Attempt to load prefsCache from the backing store.  If the attempt
   * succeeds, lastSyncTime will be updated (the new value will typically
   * correspond to the data loaded into the map, but it may be less,
   * if another VM is updating this node concurrently).  If the attempt
   * fails, a BackingStoreException is thrown and both prefsCache and
   * lastSyncTime are unaffected by the call.
   */
  @Throws(BackingStoreException::class)
  private fun loadCache() {
    var m: MutableMap<String, String> = TreeMap()
    var newLastSyncTime: Long = 0
    try {
      newLastSyncTime = prefsFile.lastModified()
      FileInputStream(prefsFile).use { fis ->
        serializer.read(fis, m)
      }
    } catch (e: Exception) {
      if (e is InvalidPreferencesFormatException) {
        logger.warning(
          "Invalid preferences format in " +
            prefsFile.path,
        )
        prefsFile.renameTo(
          File(
            prefsFile.parentFile,
            "IncorrectFormatPrefs.xml",
          ),
        )
        m = TreeMap()
      } else if (e is FileNotFoundException) {
        logger.warning(
          "Prefs file removed in background " +
            prefsFile.path,
        )
      } else {
        throw BackingStoreException(e)
      }
    }
    // Attempt succeeded; update state
    prefsCache = m
    lastSyncTime = newLastSyncTime
  }

  /**
   * Attempt to write back prefsCache to the backing store.  If the attempt
   * succeeds, lastSyncTime will be updated (the new value will correspond
   * exactly to the data thust written back, as we hold the file lock, which
   * prevents a concurrent write.  If the attempt fails, a
   * BackingStoreException is thrown and both the backing store (prefsFile)
   * and lastSyncTime will be unaffected by this call.  This call will
   * NEVER leave prefsFile in a corrupt state.
   */
  @Throws(BackingStoreException::class)
  private fun writeBackCache() {
    try {
      if (!dir!!.exists() && !dir.mkdirs()) throw BackingStoreException("$dir create failed.")
      FileOutputStream(tmpFile).use { fos ->
        serializer.write(fos, prefsCache!!)
      }
      if (!tmpFile.renameTo(prefsFile)) {
        throw BackingStoreException(
          "Can't rename " +
            tmpFile + " to " + prefsFile,
        )
      }
    } catch (e: Exception) {
      if (e is BackingStoreException) throw e
      throw BackingStoreException(e)
    }
  }

  override fun keysSpi(): Array<String> {
    initCacheIfNecessary()
    return prefsCache!!.keys.toTypedArray<String>()
  }

  override fun childrenNamesSpi(): Array<String> {
    val result: MutableList<String> = ArrayList()
    val dirContents: Array<File>? = dir!!.listFiles()
    if (dirContents != null) {
      for (i in dirContents.indices) if (dirContents[i].isDirectory) {
        result.add(
          nodeName(dirContents[i].name),
        )
      }
    }
    return result.toTypedArray()
  }

  override fun childSpi(name: String): AbstractPreferences {
    return FileSystemPreferences(this, name)
  }

  @Throws(BackingStoreException::class)
  override fun removeNode() {
    onLockFile {
      super.removeNode()
    }
  }

  /**
   * Called with file lock held (in addition to node locks).
   */
  @Throws(BackingStoreException::class)
  override fun removeNodeSpi() {
    try {
      if (changeLog.contains(nodeCreate)) {
        changeLog.remove(nodeCreate)
        nodeCreate = null
        return
      }
      if (!dir!!.exists()) return
      prefsFile.delete()
      tmpFile.delete()
      // dir should be empty now.  If it's not, empty it
      val junk: Array<out File> = dir.listFiles() ?: return
      if (junk.isNotEmpty()) {
        logger.warning(
          "Found extraneous files when removing node: " +
            listOf(*junk),
        )
        for (i in junk.indices) junk[i].delete()
      }
      if (!dir.delete()) {
        throw BackingStoreException(
          "Couldn't delete dir: " +
            dir,
        )
      }
    } catch (e: Exception) {
      throw (e as BackingStoreException)
    }
  }

  @Synchronized
  @Throws(BackingStoreException::class)
  override fun sync() {
    val userNode: Boolean = isUserNode()
    val shared: Boolean

    if (userNode) {
      // use exclusive lock for user prefs
      shared = false
    } else {
      // if can write to system root, use exclusive lock.
      // otherwise use shared lock.
      shared = !isSystemRootWritable
    }

    onLockFile(shared) {
      val newModTime: Long = kotlin.run {
        val nmt: Long
        if (isUserNode()) {
          nmt = userRootModFile!!.lastModified()
          isUserRootModified = userRootModTime == nmt
        } else {
          nmt = systemRootModFile!!.lastModified()
          isSystemRootModified = systemRootModTime == nmt
        }
        nmt
      }

      super.sync()
      if (isUserNode()) {
        userRootModTime = newModTime + 1000
        userRootModFile!!.setLastModified(userRootModTime)
      } else {
        systemRootModTime = newModTime + 1000
        systemRootModFile!!.setLastModified(systemRootModTime)
      }
    }
  }

  private fun onLockFile(shared: Boolean, action: () -> Unit) {
    onLockFile(
      locker = if (shared) SharedLock else ExclusiveLock,
      action = action,
    )
  }

  private fun onLockFile(
    locker: FileChannel.() -> FileLock = ExclusiveLock,
    action: () -> Unit,
  ) {
    val lockFile = if (isUserNode()) userLockFile!! else systemLockFile!!
    lockFile.outputStream().use { fos ->
      val channel = fos.channel
      val lock = channel.locker()
      try {
        action()
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        lock.release()
      }
    }
  }

  private val ExclusiveLock: FileChannel.() -> FileLock get() = { lock() }
  private val SharedLock: FileChannel.() -> FileLock get() = { lock(0, Long.MAX_VALUE, true) }

  @Throws(BackingStoreException::class)
  override fun syncSpi() {
    syncSpiPrivileged()
  }

  @Throws(BackingStoreException::class)
  private fun syncSpiPrivileged() {
    check(!isRemoved) { "Node has been removed" }
    if (prefsCache == null) return // We've never been used, don't bother syncing

    var lastModifiedTime: Long
    if ((if (isUserNode()) isUserRootModified else isSystemRootModified)) {
      lastModifiedTime = prefsFile.lastModified()
      if (lastModifiedTime != lastSyncTime) {
        // Prefs at this node were externally modified; read in node and
        // playback any local mods since last sync
        loadCache()
        replayChanges()
        lastSyncTime = lastModifiedTime
      }
    } else if (lastSyncTime != 0L && !dir!!.exists()) {
      // This node was removed in the background.  Playback any changes
      // against a virgin (empty) Map.
      prefsCache = TreeMap()
      replayChanges()
    }
    if (!changeLog.isEmpty()) {
      writeBackCache() // Creates directory & file if necessary
      /*
       * Attempt succeeded; it's barely possible that the call to
       * lastModified might fail (i.e., return 0), but this would not
       * be a disaster, as lastSyncTime is allowed to lag.
       */
      lastModifiedTime = prefsFile.lastModified()
      /* If lastSyncTime did not change, or went back
       * increment by 1 second. Since we hold the lock
       * lastSyncTime always monotonically encreases in the
       * atomic sense.
       */
      if (lastSyncTime <= lastModifiedTime) {
        lastSyncTime = lastModifiedTime + 1000
        prefsFile.setLastModified(lastSyncTime)
      }
      changeLog.clear()
    }
  }

  @Throws(BackingStoreException::class)
  override fun flush() {
    if (isRemoved) return
    sync()
  }

  @Throws(BackingStoreException::class)
  override fun flushSpi() {
    // assert false;
  }

  companion object {

    /**
     * Sync interval in seconds.
     */
    private val SYNC_INTERVAL: Int = max(
      1.0,
      Integer.getInteger(
        "java.util.prefs.syncInterval",
        30,
      ).toDouble(),
    ).toInt()

    private val logger: PlatformLogger
      get() = BarkPlatformLogger

    private val serializer: Serializer
      get() = PropertiesSerializer

    /**
     * Directory for system preferences.
     */
    private var systemRootDir: File? = null

    /*
     * Flag, indicating whether systemRoot  directory is writable
     */
    private var isSystemRootWritable: Boolean = false

    /**
     * Directory for user preferences.
     */
    private var userRootDir: File? = null

    /*
     * Flag, indicating whether userRoot  directory is writable
     */
    private var isUserRootWritable: Boolean = false

    /**
     * The user root.
     */
    @Volatile
    private var userRoot: Preferences? = null

    fun getUserRoot(
      fileName: String,
      applicationDir: String,
    ): Preferences = getUserRoot(NodeConfig(fileName, applicationDir))

    fun getUserRoot(config: NodeConfig): Preferences {
      var root: Preferences? = userRoot
      if (root == null) {
        synchronized(FileSystemPreferences::class.java) {
          root = userRoot
          if (root == null) {
            setupUserRoot(config)
            root = FileSystemPreferences(true, config)
            userRoot = root
          }
        }
      }
      return root!!
    }

    private fun setupUserRoot(config: NodeConfig) {
      userRootDir =
        File(
          System.getProperty(
            "java.util.prefs.userRoot",
            System.getProperty("user.home"),
          ),
          config.applicationDir,
        )
      // Attempt to create root dir if it does not yet exist.
      if (!userRootDir!!.exists()) {
        if (userRootDir!!.mkdirs()) {
          try {
            Files.setPosixFilePermissions(
              userRootDir!!.toPath(),
              PosixFilePermissions.fromString("rwx------"),
            )
          } catch (e: IOException) {
            logger.warning(
              "Could not change permissions" +
                " on userRoot directory. ",
            )
          }
          logger.info("Created user preferences directory.")
        } else logger.warning(
          "Couldn't create user preferences" +
            " directory. User preferences are unusable.",
        )
      }
      isUserRootWritable = userRootDir!!.canWrite()
      val USER_NAME: String = System.getProperty("user.name")
      userLockFile = File(
        userRootDir,
        ".user.lock.$USER_NAME",
      )
      userRootModFile = File(
        userRootDir,
        ".userRootModFile.$USER_NAME",
      )
      if (!userRootModFile!!.exists()) {
        try {
          // create if does not exist.
          userRootModFile!!.createNewFile()
          // Only user can read/write userRootModFile.
          val result = try {
            Files.setPosixFilePermissions(
              userRootModFile!!.toPath(),
              PosixFilePermissions.fromString("rw-------"),
            )
          } catch (e: IOException) {
            logger.warning(e.toString())
            null
          }
          if (result == null) {
            logger.warning(
              "Problem creating userRoot " +
                "mod file. Chmod failed on " +
                userRootModFile!!.canonicalPath +
                " Unix error code " + result,
            )
          }
        } catch (e: IOException) {
          logger.warning(e.toString())
        }
      }
      userRootModTime = userRootModFile!!.lastModified()
    }

    /**
     * The system root.
     */
    @Volatile
    private var systemRoot: Preferences? = null

    fun getSystemRoot(nodeConfig: NodeConfig): Preferences {
      var root: Preferences? = systemRoot
      if (root == null) {
        synchronized(FileSystemPreferences::class.java) {
          root = systemRoot
          if (root == null) {
            setupSystemRoot(nodeConfig)
            root = FileSystemPreferences(false, nodeConfig)
            systemRoot = root
          }
        }
      }
      return root!!
    }

    private fun setupSystemRoot(nodeConfig: NodeConfig) {
      val systemPrefsDirName: String = System.getProperty(
        "java.util.prefs.systemRoot",
        "/etc/${nodeConfig.applicationDir}",
      )
      systemRootDir = File(systemPrefsDirName)
      // Attempt to create root dir if it does not yet exist.
      if (!systemRootDir!!.exists()) {
        // system root does not exist in /etc/.java
        // Switching  to java.home
        systemRootDir =
          File(
            System.getProperty("java.home"),
            ".systemPrefs",
          )
        if (!systemRootDir!!.exists()) {
          if (systemRootDir!!.mkdirs()) {
            logger.info(
              "Created system preferences directory " +
                "in java.home.",
            )
            try {
              Files.setPosixFilePermissions(
                systemRootDir!!.toPath(),
                PosixFilePermissions.fromString("rwx---r-x"),
              )
            } catch (e: IOException) {
            }
          } else {
            logger.warning(
              (
                "Could not create " +
                  "system preferences directory. System " +
                  "preferences are unusable."
                ),
            )
          }
        }
      }
      isSystemRootWritable = systemRootDir!!.canWrite()
      systemLockFile = File(systemRootDir, ".system.lock")
      systemRootModFile =
        File(systemRootDir, ".systemRootModFile")
      if (!systemRootModFile!!.exists() && isSystemRootWritable) {
        try {
          // create if does not exist.
          systemRootModFile!!.createNewFile()
          val result = try {
            Files.setPosixFilePermissions(
              systemRootModFile!!.toPath(),
              PosixFilePermissions.fromString("rw----r--"),
            )
          } catch (e: Exception) {
            null
          }
          if (result == null) {
            logger.warning(
              "Chmod failed on " +
                systemRootModFile!!.canonicalPath +
                " Unix error code " + result,
            )
          }
        } catch (e: IOException) {
          logger.warning(e.toString())
        }
      }
      systemRootModTime = systemRootModFile!!.lastModified()
    }

    /**
     * The lock file for the user tree.
     */
    var userLockFile: File? = null

    /**
     * The lock file for the system tree.
     */
    var systemLockFile: File? = null

    /**
     * File, which keeps track of global modifications of userRoot.
     */
    private var userRootModFile: File? = null

    /**
     * Flag, which indicated whether userRoot was modified by another VM
     */
    private var isUserRootModified: Boolean = false

    /**
     * Keeps track of userRoot modification time. This time is reset to
     * zero after UNIX reboot, and is increased by 1 second each time
     * userRoot is modified.
     */
    private var userRootModTime: Long = 0

    /*
     * File, which keeps track of global modifications of systemRoot
     */
    private var systemRootModFile: File? = null

    /*
     * Flag, which indicates whether systemRoot was modified by another VM
     */
    private var isSystemRootModified: Boolean = false

    /**
     * Keeps track of systemRoot modification time. This time is reset to
     * zero after system reboot, and is increased by 1 second each time
     * systemRoot is modified.
     */
    private var systemRootModTime: Long = 0

    /**
     * Unix error code for locked file.
     */
    private const val EAGAIN: Int = 11

    /**
     * Unix error code for denied access.
     */
    private const val EACCES: Int = 13

    /* Used to interpret results of native functions */
    private const val LOCK_HANDLE: Int = 0
    private const val ERROR_CODE: Int = 1

    private val syncTimer: Timer = Timer(true) // Daemon Thread

    init {
      // Add periodic timer task to periodically sync cached prefs
      syncTimer.schedule(
        object : TimerTask() {
          override fun run() {
            syncWorld()
          }
        },
        (SYNC_INTERVAL * 1000).toLong(),
        (SYNC_INTERVAL * 1000).toLong(),
      )

      // Add shutdown hook to flush cached prefs on normal termination
      Runtime.getRuntime().addShutdownHook(
        object : Thread(null, null, "Sync Timer Thread", 0, false) {
          override fun run() {
            syncTimer.cancel()
            syncWorld()
          }
        },
      )
    }

    private fun syncWorld() {
      /*
       * Synchronization necessary because userRoot and systemRoot are
       * lazily initialized.
       */
      var userRt: Preferences?
      var systemRt: Preferences?
      synchronized(FileSystemPreferences::class.java) {
        userRt = userRoot
        systemRt = systemRoot
      }

      try {
        if (userRt != null) userRt!!.flush()
      } catch (e: BackingStoreException) {
        logger.warning("Couldn't flush user prefs: $e")
      }

      try {
        if (systemRt != null) systemRt!!.flush()
      } catch (e: BackingStoreException) {
        logger.warning("Couldn't flush system prefs: $e")
      }
    }

    private val EMPTY_STRING_ARRAY: Array<String?> = arrayOfNulls(0)

    /**
     * Returns true if the specified character is appropriate for use in
     * Unix directory names.  A character is appropriate if it's a printable
     * ASCII character (> 0x1f && < 0x7f) and unequal to slash ('/', 0x2f),
     * dot ('.', 0x2e), or underscore ('_', 0x5f).
     */
    private fun isDirChar(ch: Char): Boolean {
      return ch.code > 0x1f && ch.code < 0x7f && ch != '/' && ch != '.' && ch != '_'
    }

    /**
     * Returns the directory name corresponding to the specified node name.
     * Generally, this is just the node name.  If the node name includes
     * inappropriate characters (as per isDirChar) it is translated to Base64.
     * with the underscore  character ('_', 0x5f) prepended.
     */
    private fun dirName(nodeName: String): String {
      var i: Int = 0
      val n: Int = nodeName.length
      while (i < n) {
        if (!isDirChar(nodeName[i])) {
          return "_" + Base64.getEncoder().encode(
            byteArray(nodeName),
          )
        }
        i++
      }
      return nodeName
    }

    /**
     * Translate a string into a byte array by translating each character
     * into two bytes, high-byte first ("big-endian").
     */
    private fun byteArray(s: String): ByteArray {
      val len: Int = s.length
      val result = ByteArray(2 * len)
      var i = 0
      var j = 0
      while (i < len) {
        val c: Char = s[i]
        result[j++] = (c.code shr 8).toByte()
        result[j++] = c.code.toByte()
        i++
      }
      return result
    }

    /**
     * Returns the node name corresponding to the specified directory name.
     * (Inverts the transformation of dirName(String).
     */
    private fun nodeName(dirName: String): String {
      if (dirName[0] != '_') return dirName
      val a: ByteArray = Base64.getEncoder().encode(dirName.substring(1).toByteArray())
      val result: StringBuffer = StringBuffer(a.size / 2)
      var i: Int = 0
      while (i < a.size) {
        val highByte: Int = a[i++].toInt() and 0xff
        val lowByte: Int = a[i++].toInt() and 0xff
        result.append(((highByte shl 8) or lowByte).toChar())
      }
      return result.toString()
    }
  }
}
