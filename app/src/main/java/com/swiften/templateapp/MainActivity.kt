package com.swiften.templateapp

import android.os.Bundle
import android.webkit.WebViewFragment
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.swiften.redux.core.*
import org.swiften.redux.ui.*

class MainActivity : AppCompatActivity(),
  IPropLifecycleOwner<Redux.State, Unit> by NoopPropLifecycleOwner(),
  ILoggable,
  IPropContainer<Unit, MainActivity.Action>,
  IUniqueIDProvider by DefaultUniqueIDProvider(),
  IVetoableSubRouter {
  companion object : IPropMapper<Redux.State, Unit, Unit, Action> {
    override fun mapState(state: Redux.State, outProp: Unit) = Unit

    override fun mapAction(dispatch: IActionDispatcher, outProp: Unit): Action {
      return Action (
        registerSubRouter = { dispatch(NestedRouter.Screen.RegisterSubRouter(it)) },
        unregisterSubRouter = { dispatch(NestedRouter.Screen.UnregisterSubRouter(it)) },
        goBack = { dispatch(Redux.Screen.Back) }
      )
    }
  }

  class Action(
    val registerSubRouter: (IVetoableSubRouter) -> Unit,
    val unregisterSubRouter: (IVetoableSubRouter) -> Unit,
    val goBack: () -> Unit
  )

  override var reduxProp by ObservableReduxProp<Unit, Action> { _, next ->
    if (next.firstTime) {
      next.action.registerSubRouter(this)
    }
  }

  //region IPropLifecycleOwner
  override fun afterPropInjectionEnds(sp: StaticProp<Redux.State, Unit>) {
    this.reduxProp.action.unregisterSubRouter(this)
  }
  //endregion

  //region IVetoableSubRouter
  override val subRouterPriority get() = this.uniqueID

  override fun navigate(screen: IRouterScreen): NavigationResult {
    when (screen) {
      is Redux.Screen.Back -> {
        return if (this.supportFragmentManager.backStackEntryCount > 1) {
          this.supportFragmentManager.popBackStack()
          NavigationResult.Break
        } else {
          this.finish()
          NavigationResult.Break
        }
      }

      else -> {
        val fragment: Fragment? = when (screen) {
          is Redux.Screen.MainFragment -> MainFragment()
          else -> null
        }

        fragment?.also {
          this.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment, it)
            .addToBackStack(null)
            .commit()
        }

        return NavigationResult.Break
      }
    }
  }
  //endregion

  override fun onBackPressed() {
    this.reduxProp.action.goBack()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    if (savedInstanceState == null) {
      this.supportFragmentManager
        .beginTransaction()
        .replace(R.id.fragment, MainFragment())
        .addToBackStack(null)
        .commit()
    }
  }
}
