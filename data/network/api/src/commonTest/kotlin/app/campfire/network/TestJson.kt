package app.campfire.network

import kotlinx.serialization.json.Json

// This must match the JSON configuration in
// HttpClientModule.kt
internal val TestJson = Json {
  isLenient = true
  ignoreUnknownKeys = true
}
