package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

@Serializable
data class PingResponse(val success: Boolean)
