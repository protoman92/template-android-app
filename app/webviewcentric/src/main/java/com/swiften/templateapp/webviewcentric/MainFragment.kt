package com.swiften.templateapp.webviewcentric

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.swiften.templateapp.webviewcentric.databinding.MainFragmentBinding
import com.swiften.templateapp.webviewcentric.webview.AppJavascriptInterface
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.BridgeRequestProcessor
import com.swiften.webview.SharedPreferencesJavascriptInterface
import org.swiften.redux.core.*
import org.swiften.redux.ui.*
import java.io.Serializable

class MainFragment : Fragment(),
  IPropLifecycleOwner<Redux.State, MainFragment.IDependency> by NoopPropLifecycleOwner(),
  ILoggable,
  IPropContainer<MainFragment.State, MainFragment.Action>,
  IUniqueIDProvider by DefaultUniqueIDProvider(),
  IVetoableSubRouter
{
  companion object : IPropMapper<Redux.State, IDependency, State, Action> {
    val DefaultState = State()

    override fun mapAction(dispatch: IActionDispatcher, outProp: IDependency): Action {
      return Action(
        registerSubRouter = { dispatch(NestedRouter.Screen.RegisterSubRouter(it)) },
        unregisterSubRouter = { dispatch(NestedRouter.Screen.UnregisterSubRouter(it)) },
      )
    }

    override fun mapState(state: Redux.State, outProp: IDependency) = state.mainFragment
  }

  interface IDependency {
    val gson: Gson
    val jsArgsParser: BridgeMethodArgumentsParser
    val sharedPreferences: SharedPreferences
  }

  data class State(val noop: Boolean = true) : Serializable

  class Action(
    val registerSubRouter: (IVetoableSubRouter) -> Unit,
    val unregisterSubRouter: (IVetoableSubRouter) -> Unit
  )

  private lateinit var bridgeRequestProcessor: BridgeRequestProcessor

  //region IPropContainer
  override var reduxProp by ObservableReduxProp<State, Action> { _, next ->
    if (next.firstTime) {
      next.action.registerSubRouter(this)
    }
  }
  //endregion

  //region IPropLifecycleOwner
  override fun beforePropInjectionStarts(sp: StaticProp<Redux.State, IDependency>) {
    bridgeRequestProcessor = BridgeRequestProcessor(
      gson = sp.outProp.gson,
      javascriptEvaluator = this.binding.customWebview
    )

    this.binding.customWebview.let {
      it.javascriptInterfaces = arrayListOf(
        AppJavascriptInterface(
          argsParser = sp.outProp.jsArgsParser,
          requestProcessor = bridgeRequestProcessor,
        ),
        SharedPreferencesJavascriptInterface(
          name = "StorageModule",
          argsParser = sp.outProp.jsArgsParser,
          requestProcessor = bridgeRequestProcessor,
          sharedPreferences = sp.outProp.sharedPreferences,
        ),
      )

      this.binding.customWebview.initialize()
      it.loadUrl("file:///android_asset/index.html")
    }
  }

  override fun afterPropInjectionEnds(sp: StaticProp<Redux.State, IDependency>) {
    this.binding.customWebview.deinitialize()
    this.bridgeRequestProcessor.deinitialize()
    this.reduxProp.action.unregisterSubRouter(this)
  }
  //endregion

  //region IVetoableSubRouter
  override val subRouterPriority get() = this.uniqueID

  override fun navigate(screen: IRouterScreen): NavigationResult {
    return when (screen) {
      Redux.Screen.Back -> {
        if (this.binding.customWebview.canGoBack()) {
          this.binding.customWebview.goBack()
          return NavigationResult.Break
        }

        NavigationResult.Fallthrough
      }
      else -> NavigationResult.Fallthrough
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
