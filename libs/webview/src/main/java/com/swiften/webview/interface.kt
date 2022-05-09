package com.swiften.webview

import android.webkit.ValueCallback
import io.reactivex.Observable

interface IBridgeRequestProcessor {
  fun <Parameters, Result : Any> processStream(
    stream: Observable<Result>,
    bridgeArguments: BridgeMethodArguments<Parameters>
  )
}

interface IJavascriptEvaluator {
  fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?)
}

interface IJavascriptInterface {
  val name: String
}

data class BridgeMethodArguments<Parameters>(
  val callback: String,
  val method: String,
  val module: String,
  val parameters: Parameters
)
