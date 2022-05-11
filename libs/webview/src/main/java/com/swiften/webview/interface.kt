package com.swiften.webview

import android.webkit.ValueCallback
import android.webkit.WebView
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

interface IBridgeRequestProcessor {
  fun <Parameters, Result> processStream(
    stream: Observable<Result>,
    bridgeArguments: BridgeMethodArguments<Parameters>
  )
}

fun <Parameters, Result> IBridgeRequestProcessor.processStream(
  stream: Maybe<Result>,
  bridgeArguments: BridgeMethodArguments<Parameters>,
) {
  return this.processStream(stream = stream.toObservable(), bridgeArguments = bridgeArguments)
}

fun <Parameters, Result> IBridgeRequestProcessor.processStream(
  stream: Single<Result>,
  bridgeArguments: BridgeMethodArguments<Parameters>,
) {
  return this.processStream(stream = stream.toObservable(), bridgeArguments = bridgeArguments)
}

interface IJavascriptEvaluator {
  fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?)
}

interface IJavascriptInterface {
  val name: String
}

interface IWebView : IJavascriptEvaluator {
  var javascriptInterfaces: List<IJavascriptInterface>

  fun loadUrl(url: String)
}

interface IWebViewEventHook {
  fun onPageFinished(view: WebView?, url: String?)
}

data class BridgeMethodArguments<Parameters>(
  val callback: String,
  val method: String,
  val module: String,
  val parameters: Parameters
)
