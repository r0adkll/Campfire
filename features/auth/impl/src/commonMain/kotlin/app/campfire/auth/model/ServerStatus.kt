package app.campfire.auth.model

import app.campfire.auth.api.model.OpenIdFormData
import app.campfire.auth.api.model.ServerStatus
import app.campfire.network.models.ServerStatus as NetworkServerStatus

fun NetworkServerStatus.asDomainModel(): ServerStatus = ServerStatus(
  serverVersion = serverVersion,
  isInit = isInit,
  language = language,
  authMethods = authMethods,
  authFormData = authFormData?.let {
    OpenIdFormData(
      customMessage = it.authLoginCustomMessage,
      openIdButtonText = it.authOpenIDButtonText,
    )
  },
)
