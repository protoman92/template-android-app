package com.swiften.templateapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.swiften.redux.core.IReducer
import org.swiften.redux.core.IReduxAction
import org.swiften.redux.core.IRouterScreen
import org.swiften.redux.saga.common.SagaEffect

object Redux {
  @Parcelize
  data class State(val mainFragment: MainFragment.State = MainFragment.DefaultState): Parcelable

  sealed class Action : IReduxAction {
    sealed class MainFragment : Action() {
      object DecrementClickCount : MainFragment()
      object IncrementClickCount : MainFragment()
    }
  }

  sealed class Screen : IRouterScreen {
    object MainFragment : Screen()
    object Back : Screen()
  }

  object Reducer : IReducer<State, IReduxAction> {
    override fun invoke(state: State, action: IReduxAction): State {
      return when (action) {
        is Action.MainFragment.DecrementClickCount -> state.copy(
          mainFragment = state.mainFragment.copy(
            clickCount = state.mainFragment.clickCount - 1
          )
        )
        is Action.MainFragment.IncrementClickCount -> state.copy(
          mainFragment = state.mainFragment.copy(
            clickCount = state.mainFragment.clickCount + 1
          )
        )
        else -> state
      }
    }
  }

  object Saga {
    fun allSagas() = arrayListOf<SagaEffect<Unit>>()
  }
}
