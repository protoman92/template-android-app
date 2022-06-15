package com.swiften.templateapp.webviewcentric

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.swiften.commonview.lifecycle.LifecycleStreamObserver
import com.swiften.commonview.utils.LazyProperty
import com.swiften.commonview.utils.map
import com.swiften.templateapp.webviewcentric.databinding.MainFragmentBinding
import com.swiften.templateapp.webviewcentric.webview.AppJavascriptInterface
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.BridgeRequestProcessor
import com.swiften.webview.javascriptinterface.genericlifecycle.GenericLifecycleJavascriptInterface
import com.swiften.webview.IWebViewEventHook
import com.swiften.webview.NoopWebViewEventHook
import com.swiften.webview.javascriptinterface.sharedpreferences.SharedPreferencesJavascriptInterface
import org.swiften.redux.core.*
import org.swiften.redux.ui.*
import java.io.Serializable

class MainFragment : Fragment(),
  IPropLifecycleOwner<Redux.State, MainFragment.IDependency> by NoopPropLifecycleOwner(),
  ILoggable,
  IPropContainer<MainFragment.State, MainFragment.Action>,
  IUniqueIDProvider by DefaultUniqueIDProvider(),
  IVetoableSubRouter,
  IWebViewEventHook by NoopWebViewEventHook
{
  companion object : IPropMapper<Redux.State, IDependency, State, Action> {
    val DefaultState = State()

    override fun mapAction(dispatch: IActionDispatcher, outProp: IDependency): Action {
      return Action(
        registerSubRouter = { dispatch(NestedRouter.Screen.RegisterSubRouter(it)) },
        unregisterSubRouter = { dispatch(NestedRouter.Screen.UnregisterSubRouter(it)) },
        updateCurrentURL = { dispatch(Redux.Action.MainFragment.UpdateCurrentURL(it)) },
      )
    }

    override fun mapState(state: Redux.State, outProp: IDependency) = state.mainFragment
  }

  interface IDependency {
    val gson: Gson
    val jsArgsParser: BridgeMethodArgumentsParser
    val sharedPreferences: SharedPreferences
  }

  data class State(val currentURL: String = BuildConfig.WEB_APP_URL) : Serializable

  class Action(
    val registerSubRouter: (IVetoableSubRouter) -> Unit,
    val unregisterSubRouter: (IVetoableSubRouter) -> Unit,
    val updateCurrentURL: (String) -> Unit,
  )

  private lateinit var bridgeRequestProcessor: BridgeRequestProcessor

  private val lifecycleStreamObserver: LifecycleStreamObserver by lazy {
    LifecycleStreamObserver(lifecycleOwner = this)
  }

  private val lazyDependency: LazyProperty<IDependency> by lazy { LazyProperty() }

  //region IPropContainer
  override var reduxProp by ObservableReduxProp<State, Action> { _, next ->
    if (next.firstTime) {
      next.action.registerSubRouter(this)

      if (this.binding.customWebview.getUrl() !== next.state.currentURL) {
        this.binding.customWebview.loadUrl(url = next.state.currentURL)
      }
    }
  }
  //endregion

  //region IPropLifecycleOwner
  override fun beforePropInjectionStarts(sp: StaticProp<Redux.State, IDependency>) {
    this.lazyDependency.value = sp.outProp
  }

  override fun afterPropInjectionEnds(sp: StaticProp<Redux.State, IDependency>) {
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

  //region IWebViewEventHook
  override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
    url?.also { this.reduxProp.action.updateCurrentURL(it) }
  }
  //endregion

  private var _binding: MainFragmentBinding? = null
  private val binding get() = this._binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.lifecycleStreamObserver.initialize()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    this._binding = MainFragmentBinding.inflate(inflater, container, false)
    return this.binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.bridgeRequestProcessor = BridgeRequestProcessor(
      gson = lazyDependency.map { it.gson },
      javascriptEvaluator = LazyProperty(initialValue = this.binding.customWebview),
      lifecycleStreamObserver = LazyProperty(initialValue = this.lifecycleStreamObserver),
    )

    this.binding.customWebview.let { webview ->
      webview.javascriptInterfaces = arrayListOf(
        AppJavascriptInterface(
          name = "AppModule",
          argsParser = this.lazyDependency.map { it.jsArgsParser },
          requestProcessor = LazyProperty(initialValue = bridgeRequestProcessor),
        ),
        GenericLifecycleJavascriptInterface(
          name = "GenericLifecycleModule",
          argsParser = this.lazyDependency.map { it.jsArgsParser },
          requestProcessor = LazyProperty(initialValue = bridgeRequestProcessor),
        ),
        SharedPreferencesJavascriptInterface(
          name = "StorageModule",
          argsParser = this.lazyDependency.map { it.jsArgsParser },
          requestProcessor = LazyProperty(initialValue = bridgeRequestProcessor),
          sharedPreferences = this.lazyDependency.map { it.sharedPreferences },
        ),
      )

      webview.registerEventHook(eventHook = this@MainFragment)
      this.binding.customWebview.initialize()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    this._binding = null
  }

  override fun onDestroy() {
    super.onDestroy()
    this.lifecycleStreamObserver.deinitialize()
  }
}
