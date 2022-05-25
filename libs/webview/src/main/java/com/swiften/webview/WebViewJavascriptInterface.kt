package com.swiften.webview

import android.webkit.JavascriptInterface
import io.reactivex.Single

class WebViewJavascriptInterface(
  override val name: String,
  private val argsParser: BridgeMethodArgumentsParser,
  private val requestProcessor: IBridgeRequestProcessor,
  private val webView: IWebView
) : IJavascriptInterface {
  @JavascriptInterface
  fun goBack(rawRequest: String) {
    val request = argsParser.parseArguments<Unit>(rawRequest)

    this.requestProcessor.processStream(
      stream = Single.defer {
        this.webView.reload()
        Single.just(Unit)
      },
      bridgeArguments = request
    )
  }

  @JavascriptInterface
  fun reload(rawRequest: String) {
    val request = argsParser.parseArguments<Unit>(rawRequest)

    this.requestProcessor.processStream(
      stream = Single.defer {
        this.webView.reload()
        Single.just(Unit)
      },
      bridgeArguments = request,
    )
  }
}
