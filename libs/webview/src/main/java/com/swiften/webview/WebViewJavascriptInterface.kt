package com.swiften.webview

import android.webkit.JavascriptInterface
import com.swiften.commonview.genericlifecycle.IGenericLifecycleOwner
import com.swiften.commonview.genericlifecycle.NoopGenericLifecycleOwner
import io.reactivex.Single

class WebViewJavascriptInterface(
  override val name: String,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val requestProcessor: Lazy<IBridgeRequestProcessor>,
  private val webView: Lazy<IWebView>,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  @JavascriptInterface
  fun goBack(rawRequest: String) {
    val request = argsParser.value.parseArguments<Unit>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = Single.defer {
        this.webView.value.reload()
        Single.just(Unit)
      },
      bridgeArguments = request
    )
  }

  @JavascriptInterface
  fun reload(rawRequest: String) {
    val request = argsParser.value.parseArguments<Unit>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = Single.defer {
        this.webView.value.reload()
        Single.just(Unit)
      },
      bridgeArguments = request,
    )
  }
}
