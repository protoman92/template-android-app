package com.swiften.templateapp

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.swiften.templateapp.webview.JavascriptArgumentsParser
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
    val gson = Gson()
    val jsArgsParser = JavascriptArgumentsParser(gson)

    val dependency = object : IDependency {
      override val gson: Gson get() = gson

      override val jsArgsParser get() = jsArgsParser
    }

    injector.injectActivityParcelable(
      application = this,
      injectionHelper = object : ILifecycleInjectionHelper<Redux.State> {
        override fun inject(injector: IPropInjector<Redux.State>, owner: LifecycleOwner) {
          when (owner) {
            is MainActivity -> injector.injectLifecycle(Unit, owner, MainActivity)
            is MainFragment -> injector.injectLifecycle(dependency, owner, MainFragment)
          }
        }

        override fun deinitialize(owner: LifecycleOwner) {}
      }
    )
  }
}
