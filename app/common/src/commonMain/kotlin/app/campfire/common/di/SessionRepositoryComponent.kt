package app.campfire.common.di

import app.campfire.sessions.api.SessionsRepository

interface SessionRepositoryComponent {

  val sessionsRepository: SessionsRepository
}
