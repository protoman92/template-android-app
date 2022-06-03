package com.swiften.webview

import android.graphics.Bitmap
import android.webkit.ValueCallback
import android.webkit.WebView
import com.swiften.commonview.IGenericLifecycleOwner
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

interface IBridgeRequestProcessor {
  fun <Parameters, Result> processStream(
    stream: Flowable<Result>,
    bridgeArguments: BridgeMethodArguments<Parameters>
  )
}

fun <Parameters> IBridgeRequestProcessor.processStream(
  stream: Completable,
  bridgeArguments: BridgeMethodArguments<Parameters>,
) {
  return this.processStream(stream = stream.toFlowable<Unit>(), bridgeArguments = bridgeArguments)
}

fun <Parameters, Result> IBridgeRequestProcessor.processStream(
  stream: Maybe<Result>,
  bridgeArguments: BridgeMethodArguments<Parameters>,
) {
  return this.processStream(stream = stream.toFlowable(), bridgeArguments = bridgeArguments)
}

fun <Parameters, Result> IBridgeRequestProcessor.processStream(
  stream: Observable<Result>,
  bridgeArguments: BridgeMethodArguments<Parameters>,
) {
  return this.processStream(
    stream = stream.toFlowable(BackpressureStrategy.BUFFER),
    bridgeArguments = bridgeArguments,
  )
}

fun <Parameters, Result> IBridgeRequestProcessor.processStream(
  stream: Single<Result>,
  bridgeArguments: BridgeMethodArguments<Parameters>,
) {
  return this.processStream(stream = stream.toFlowable(), bridgeArguments = bridgeArguments)
}

interface IJavascriptEvaluator {
  fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?)
}

interface IJavascriptInterface : IGenericLifecycleOwner {
  val name: String
}

interface IWebView : IGenericLifecycleOwner, IJavascriptEvaluator {
  var javascriptInterfaces: List<IJavascriptInterface>

  fun canGoBack(): Boolean

  fun goBack()

  fun loadUrl(url: String)

  fun reload()
}

interface IWebViewEventHook {
  fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?)

  fun onPageFinished(view: WebView?, url: String?)
}

data class BridgeMethodArguments<Parameters>(
  val callback: String,
  val method: String,
  val module: String,
  val parameters: Parameters
)
