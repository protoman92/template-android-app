package com.swiften.templateapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.swiften.templateapp.databinding.MainFragmentBinding
import org.swiften.redux.core.*
import org.swiften.redux.ui.*
import java.io.Serializable

class MainFragment : Fragment(),
  IPropLifecycleOwner<Redux.State, Unit> by NoopPropLifecycleOwner(),
  ILoggable,
  IPropContainer<MainFragment.State, MainFragment.Action>,
  IUniqueIDProvider by DefaultUniqueIDProvider(),
  IVetoableSubRouter {
  companion object : IPropMapper<Redux.State, Unit, State, Action> {
    val DefaultState = State()

    override fun mapAction(dispatch: IActionDispatcher, outProp: Unit): Action {
      return Action(
        registerSubRouter = { dispatch(NestedRouter.Screen.RegisterSubRouter(it)) },
        unregisterSubRouter = { dispatch(NestedRouter.Screen.UnregisterSubRouter(it)) }
      )
    }

    override fun mapState(state: Redux.State, outProp: Unit) = state.mainFragment
  }

  data class State(val noop: Boolean = true) : Serializable

  class Action(
    val registerSubRouter: (IVetoableSubRouter) -> Unit,
    val unregisterSubRouter: (IVetoableSubRouter) -> Unit
  )

  override var reduxProp by ObservableReduxProp<State, Action> { _, next ->
    if (next.firstTime) {
      next.action.registerSubRouter(this)
    }
  }

  //region IPropLifecycleOwner
  override fun beforePropInjectionStarts(sp: StaticProp<Redux.State, Unit>) {
    this.binding.customWebview.loadUrl("https://www.google.com")
  }

  override fun afterPropInjectionEnds(sp: StaticProp<Redux.State, Unit>) {
    this.reduxProp.action.unregisterSubRouter(this)
  }
  //endregion

  //region IVetoableSubRouter
  override val subRouterPriority get() = this.uniqueID

  override fun navigate(screen: IRouterScreen) = NavigationResult.Fallthrough
  //endregion

  private var _binding: MainFragmentBinding? = null
  private val binding get() = this._binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    this._binding = MainFragmentBinding.inflate(inflater, container, false)
    return this.binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    this._binding = null
  }
}
