package app.campfire.shared.di

import app.campfire.core.di.ActivityScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import me.tatarka.inject.annotations.Provides

//@SingleIn(UserScope::class)
//@ContributesSubcomponent(
//  scope = UserScope::class,
//  parentScope = ActivityScope::class,
//)
//interface UserComponent {
//
//  @Provides
//  @SingleIn(UserScope::class)
//  fun provideCircuit(
//    uiFactories: Set<Ui.Factory>,
//    presenterFactories: Set<Presenter.Factory>,
//  ): Circuit = Circuit.Builder()
//    .addUiFactories(uiFactories)
//    .addPresenterFactories(presenterFactories)
//    .build()
//
//  @ContributesSubcomponent.Factory
//  interface Factory {
//    fun create(userSession: UserSession): UserComponent
//  }
//}
