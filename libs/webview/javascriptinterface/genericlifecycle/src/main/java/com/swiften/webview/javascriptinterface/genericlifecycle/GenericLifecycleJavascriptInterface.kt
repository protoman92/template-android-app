package com.swiften.webview.javascriptinterface.genericlifecycle

import android.webkit.JavascriptInterface
import com.swiften.commonview.genericlifecycle.IGenericLifecycleOwner
import com.swiften.commonview.genericlifecycle.NoopGenericLifecycleOwner
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.BridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import io.reactivex.processors.PublishProcessor

class GenericLifecycleJavascriptInterface(
  override val name: String,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val requestProcessor: Lazy<BridgeRequestProcessor>,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class LifecycleEvent {
    object Initialize : LifecycleEvent()

    object Deinitialize : LifecycleEvent()
  }

  private val lifecycleEventProcessor = PublishProcessor.create<LifecycleEvent>()

  //region IGenericLifecycleOwner
  override fun initialize() {
    this.lifecycleEventProcessor.onNext(LifecycleEvent.Initialize)
  }

  override fun deinitialize() {
    this.lifecycleEventProcessor.onNext(LifecycleEvent.Deinitialize)
  }
  //endregion

  @JavascriptInterface
  fun observeInitialize(rawRequest: String) {
    val request = this.argsParser.value.parseArguments<Unit>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = this.lifecycleEventProcessor.filter { it is LifecycleEvent.Initialize },
      bridgeArguments = request,
    )
  }

  @JavascriptInterface
  fun observeDeinitialize(rawRequest: String) {
    val request = this.argsParser.value.parseArguments<Unit>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = this.lifecycleEventProcessor.filter { it is LifecycleEvent.Deinitialize },
      bridgeArguments = request,
    )
  }
}
