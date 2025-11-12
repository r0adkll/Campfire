package app.campfire.settings.test

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.get
import kotlin.enums.enumEntries
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalSettingsApi::class)
abstract class TestSettings {

  val values: Map<String, Any>
    get() = _settings.toMap()

  private val _settings = mutableMapOf<String, Any>()
  protected val settings = MapSettings(_settings)

  protected fun string() = property(
    getter = { getStringOrNull(it) ?: "" },
    setter = { key, value -> putString(key, value) },
  )

  protected fun observeString(property: KProperty<*>): Flow<String> {
    return settings.getStringFlow(property.name, "")
  }

  protected fun stringOrNull() = property(
    getter = { getStringOrNull(it) },
    setter = { key, value ->
      if (value == null) {
        remove(key)
      } else {
        putString(key, value)
      }
    },
  )

  protected fun observeStringOrNull(property: KProperty<*>): Flow<String?> {
    return settings.getStringOrNullFlow(property.name)
  }

  protected fun int() = property(
    getter = { getIntOrNull(it) ?: 0 },
    setter = { key, value -> putInt(key, value) },
  )

  protected fun boolean() = property(
    getter = { getBooleanOrNull(it) ?: false },
    setter = { key, value -> putBoolean(key, value) },
  )

  protected fun observeBoolean(property: KProperty<*>): Flow<Boolean> {
    return settings.getBooleanFlow(property.name, false)
  }

  protected inline fun <reified E : Enum<E>> enum() = property(
    getter = { enumEntries<E>()[getInt(it, 0)] },
    setter = { key, value -> putInt(key, value.ordinal) },
  )

  protected inline fun <reified E : Enum<E>> observeEnum(property: KProperty<*>): Flow<E> {
    return settings.getIntFlow(property.name, 0).map {
      enumValues<E>()[it]
    }
  }

  protected fun <V> property(
    getter: Settings.(String) -> V,
    setter: Settings.(String, V) -> Unit,
  ) = object : ReadWriteProperty<TestSettings, V> {
    override fun getValue(thisRef: TestSettings, property: KProperty<*>): V {
      return thisRef.settings.getter(property.name)
    }

    override fun setValue(thisRef: TestSettings, property: KProperty<*>, value: V) {
      return thisRef.settings.setter(property.name, value)
    }
  }
}
