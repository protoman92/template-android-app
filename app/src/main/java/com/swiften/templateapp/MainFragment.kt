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
        unregisterSubRouter = { dispatch(NestedRouter.Screen.UnregisterSubRouter(it)) },
        decrementClickCount = { dispatch(Redux.Action.MainFragment.DecrementClickCount) },
        incrementClickCount = { dispatch(Redux.Action.MainFragment.IncrementClickCount) }
      )
    }

    override fun mapState(state: Redux.State, outProp: Unit) = state.mainFragment
  }

  data class State(val clickCount: Int = 0) : Serializable

  class Action(
    val registerSubRouter: (IVetoableSubRouter) -> Unit,
    val unregisterSubRouter: (IVetoableSubRouter) -> Unit,
    val decrementClickCount: () -> Unit,
    val incrementClickCount: () -> Unit
  )

  override var reduxProp by ObservableReduxProp<State, Action> { _, next ->
    if (next.firstTime) {
      next.action.registerSubRouter(this)
    }

    this.binding.incrementButton.text = next.state.clickCount.toString()
  }

  //region IPropLifecycleOwner
  override fun beforePropInjectionStarts(sp: StaticProp<Redux.State, Unit>) {
    this.binding.incrementButton.setOnClickListener {
      this.reduxProp.action.incrementClickCount()
    }
  }

  override fun afterPropInjectionEnds(sp: StaticProp<Redux.State, Unit>) {
    this.binding.incrementButton.setOnClickListener(null)
    this.reduxProp.action.unregisterSubRouter(this)
  }
  //endregion

  //region IVetoableSubRouter
  override val subRouterPriority get() = this.uniqueID

  override fun navigate(screen: IRouterScreen): Boolean {
    when (screen) {
      Redux.Screen.Back -> {
        if (this.reduxProp.state.clickCount > 1) {
          this.reduxProp.action.decrementClickCount()
          return true
        }

        return false
      }
      else -> {
        return false
      }
    }
  }
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
