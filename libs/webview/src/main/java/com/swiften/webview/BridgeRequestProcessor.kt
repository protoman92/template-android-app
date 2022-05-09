package com.swiften.webview

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.concurrent.Semaphore

class BridgeRequestProcessor(
  private val gson: Gson,
  private val javascriptEvaluator: IJavascriptEvaluator,
) : IBridgeRequestProcessor {
  sealed class StreamEventResult(val event: String) {
    object Terminated : StreamEventResult(event = "STREAM_TERMINATED")
  }

  data class StreamResponse<Result>(val result: Result?, val error: String?)

  private val compositeDisposable = CompositeDisposable()
  private val semaphore = Semaphore(1)

  private fun ensureSynchronous(fn: (done: () -> Unit) -> Unit) {
    this.semaphore.acquire()

    fn {
      this.semaphore.release()
    }

    this.semaphore.acquire()
    this.semaphore.release()
  }

  private fun checkCallbackAvailable(callback: String): Boolean {
    var result = false

    this.ensureSynchronous { done ->
      this.javascriptEvaluator.evaluateJavascript("javascript:window.$callback != null") {
        result = it == "true"
        done()
      }
    }

    return result
  }

  private fun <Result> sendResponseToCallback(callback: String, response: StreamResponse<Result>) {
    this.ensureSynchronous { done ->
      val jsonResponse = this.gson.toJson(response)

      this.javascriptEvaluator.evaluateJavascript("javascript:window.$callback($jsonResponse)") {
        done()
      }
    }
  }

  //region IBridgeRequestProcessor
  override fun <Parameters, Result : Any> processStream(
    stream: Observable<Result>,
    bridgeArguments: BridgeMethodArguments<Parameters>
  ) {
    var disposable: Disposable? = null

    val sendResultIfCallbackAvailable: (Any?, String?) -> Unit = { result, error ->
      val isCallbackAvailable = this.checkCallbackAvailable(callback = bridgeArguments.callback)

      if (isCallbackAvailable) {
        this.sendResponseToCallback(bridgeArguments.callback, StreamResponse(result = result, error = error))
      } else {
        disposable?.dispose()
      }
    }

    disposable = stream.subscribe(
      { sendResultIfCallbackAvailable(it, null) },
      { sendResultIfCallbackAvailable(null, it.message) },
      { sendResultIfCallbackAvailable(StreamEventResult.Terminated, null) }
    )

    compositeDisposable.add(disposable)
  }
  //endregion

  fun deinitialize() {
    this.compositeDisposable.dispose()
  }
}