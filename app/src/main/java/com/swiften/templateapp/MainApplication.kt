package com.swiften.templateapp

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import org.swiften.redux.android.ui.AndroidPropInjector
import org.swiften.redux.android.ui.lifecycle.ILifecycleInjectionHelper
import org.swiften.redux.android.ui.lifecycle.injectActivityParcelable
import org.swiften.redux.android.ui.lifecycle.injectLifecycle
import org.swiften.redux.core.FinalStore
import org.swiften.redux.core.NestedRouter
import org.swiften.redux.core.RouterMiddleware
import org.swiften.redux.core.applyMiddlewares
import org.swiften.redux.saga.common.SagaMiddleware
import org.swiften.redux.ui.IPropInjector

class MainApplication : Application(),
  ILoggable {
  override fun onCreate() {
    super.onCreate()

    val store = applyMiddlewares<Redux.State>(
      RouterMiddleware.create(NestedRouter.create { false }),
      SagaMiddleware.create(effects = Redux.Saga.allSagas())
    )(FinalStore(state = Redux.State(), reducer = Redux.Reducer))

    val injector = AndroidPropInjector(store = store)

    injector.injectActivityParcelable(
      application = this,
      injectionHelper = object : ILifecycleInjectionHelper<Redux.State> {
        override fun inject(injector: IPropInjector<Redux.State>, owner: LifecycleOwner) {
          when (owner) {
            is MainActivity -> injector.injectLifecycle(Unit, owner, MainActivity)
            is MainFragment -> injector.injectLifecycle(Unit, owner, MainFragment)
          }
        }

        override fun deinitialize(owner: LifecycleOwner) {}
      }
    )
  }
}
