// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.HomeNetworkSettings
import app.campfire.settings.api.LearnedHomeNetwork
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = HomeNetworkSettings::class)
@Inject
class HomeNetworkSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : HomeNetworkSettings, AppSettings() {

  private val pauseAwayFromHomeProperty = booleanSetting(KEY_PAUSE_AWAY_FROM_HOME, true)
  override var pauseAwayFromHome: Boolean by pauseAwayFromHomeProperty
  override fun observePauseAwayFromHome(): StateFlow<Boolean> = pauseAwayFromHomeProperty.observe()

  private val learnedHomeNetworksProperty = customSetting(
    key = KEY_LEARNED_HOME_NETWORKS,
    defaultValue = emptyList(),
    getter = { raw -> decodeLearnedHomeNetworks(raw) },
    setter = { networks -> encodeLearnedHomeNetworks(networks) },
  )
  override var learnedHomeNetworks: List<LearnedHomeNetwork> by learnedHomeNetworksProperty
  override fun observeLearnedHomeNetworks(): StateFlow<List<LearnedHomeNetwork>> =
    learnedHomeNetworksProperty.observe()
}

internal fun encodeLearnedHomeNetworks(networks: List<LearnedHomeNetwork>): String {
  val array = buildJsonArray {
    networks.forEach { network ->
      add(
        buildJsonObject {
          put(FIELD_SERVER, network.serverOrigin)
          put(FIELD_SUBNET, network.subnet)
          put(FIELD_GATEWAY, network.gateway)
          put(FIELD_TRANSPORT, network.transport.name)
          put(FIELD_DOMAIN, network.domain)
          put(FIELD_LABEL, network.label)
          put(FIELD_FIRST_LEARNED, network.firstLearnedAtMs)
          put(FIELD_LAST_SEEN, network.lastSeenAtMs)
        },
      )
    }
  }
  return array.toString()
}

/** Decodes leniently: a malformed store yields no networks (they're relearned) rather than a crash. */
internal fun decodeLearnedHomeNetworks(raw: String): List<LearnedHomeNetwork> {
  val array = runCatching { Json.parseToJsonElement(raw) as? JsonArray }.getOrNull() ?: return emptyList()
  return array.mapNotNull { element ->
    val obj = element as? JsonObject ?: return@mapNotNull null
    LearnedHomeNetwork(
      serverOrigin = obj.string(FIELD_SERVER) ?: return@mapNotNull null,
      subnet = obj.string(FIELD_SUBNET) ?: return@mapNotNull null,
      gateway = obj.string(FIELD_GATEWAY),
      transport = obj.string(FIELD_TRANSPORT)
        ?.let { name -> LearnedHomeNetwork.Transport.entries.firstOrNull { it.name == name } }
        ?: return@mapNotNull null,
      domain = obj.string(FIELD_DOMAIN),
      label = obj.string(FIELD_LABEL),
      firstLearnedAtMs = obj.long(FIELD_FIRST_LEARNED) ?: 0L,
      lastSeenAtMs = obj.long(FIELD_LAST_SEEN) ?: 0L,
    )
  }
}

private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull

private fun JsonObject.long(key: String): Long? = get(key)?.jsonPrimitive?.longOrNull

private const val FIELD_SERVER = "server"
private const val FIELD_SUBNET = "subnet"
private const val FIELD_GATEWAY = "gateway"
private const val FIELD_TRANSPORT = "transport"
private const val FIELD_DOMAIN = "domain"
private const val FIELD_LABEL = "label"
private const val FIELD_FIRST_LEARNED = "firstLearned"
private const val FIELD_LAST_SEEN = "lastSeen"

internal const val KEY_PAUSE_AWAY_FROM_HOME = "pref_pause_away_from_home"
internal const val KEY_LEARNED_HOME_NETWORKS = "pref_learned_home_networks"
