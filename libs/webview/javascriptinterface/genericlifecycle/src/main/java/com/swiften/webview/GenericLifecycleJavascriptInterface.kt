package com.swiften.webview

import android.webkit.JavascriptInterface
import com.swiften.commonview.IGenericLifecycleOwner
import com.swiften.commonview.NoopGenericLifecycleOwner
import io.reactivex.subjects.PublishSubject

class GenericLifecycleJavascriptInterface(
  override val name: String,
  private val argsParser: BridgeMethodArgumentsParser,
  private val requestProcessor: BridgeRequestProcessor,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class LifecycleEvent {
    object Initialize : LifecycleEvent()
    object Deinitialize : LifecycleEvent()
  }

  private val lifecycleEventSubject = PublishSubject.create<LifecycleEvent>()

  //region IGenericLifecycleOwner
  override fun initialize() {
    this.lifecycleEventSubject.onNext(LifecycleEvent.Initialize)
  }

  override fun deinitialize() {
    this.lifecycleEventSubject.onNext(LifecycleEvent.Deinitialize)
  }
  //endregion

  @JavascriptInterface
  fun observeInitialize(rawRequest: String) {
    val request = this.argsParser.parseArguments<Unit>(rawRequest)

    this.requestProcessor.processStream(
      stream = this.lifecycleEventSubject.filter { it is LifecycleEvent.Initialize },
      bridgeArguments = request,
    )
  }

  @JavascriptInterface
  fun observeDeinitialize(rawRequest: String) {
    val request = this.argsParser.parseArguments<Unit>(rawRequest)


    this.requestProcessor.processStream(
      stream = this.lifecycleEventSubject.filter { it is LifecycleEvent.Deinitialize },
      bridgeArguments = request,
    )
  }
}
