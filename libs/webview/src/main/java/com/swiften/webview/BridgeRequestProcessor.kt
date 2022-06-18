package com.swiften.webview

import com.google.gson.Gson
import com.swiften.commonview.lifecycle.IGenericLifecycleOwner
import com.swiften.commonview.lifecycle.NoopGenericLifecycleOwner
import com.swiften.commonview.lifecycle.ILifecycleStreamObserver
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Semaphore

class BridgeRequestProcessor(
  private val gson: Lazy<Gson>,
  private val javascriptEvaluator: Lazy<IJavascriptEvaluator>,
  private val lifecycleStreamObserver: Lazy<ILifecycleStreamObserver>,
  private val scheduler: Scheduler = Schedulers.computation(),
) : IBridgeRequestProcessor,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class StreamEventResult(val event: String) {
    object Complete : StreamEventResult(event = "STREAM_COMPLETE")
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
      this.javascriptEvaluator.value
        .evaluateJavascript(script = """javascript:typeof window.$callback == "function"""") {
          result = it == "true"
          done()
        }
    }

    return result
  }

  private fun <Result> sendResponseToCallback(callback: String, response: StreamResponse<Result>) {
    this.ensureSynchronous { done ->
      val jsonResponse = this.gson.value.toJson(response)

      val scriptToExecute = arrayListOf(
        """if (typeof window.$callback !== "function") {throw new Error("window.$callback does not exist") }""",
        "window.$callback($jsonResponse)"
      ).joinToString(";")

      this.javascriptEvaluator.value
        .evaluateJavascript(script = "javascript:$scriptToExecute") { done() }
    }
  }

  //region IBridgeRequestProcessor
  override fun <Parameters, Result> processStream(
    stream: Flowable<Result>,
    bridgeArguments: BridgeMethodArguments<Parameters>
  ) {
    var disposable: Disposable? = null

    val sendResultIfCallbackAvailable: (Any?, String?) -> Unit = { result, error ->
      val isCallbackAvailable = this.checkCallbackAvailable(callback = bridgeArguments.callback)

      if (isCallbackAvailable) {
        this.sendResponseToCallback(
          callback = bridgeArguments.callback,
          response = StreamResponse(result = result, error = error)
        )
      } else {
        disposable?.dispose()
      }
    }

    disposable = stream
      .observeOn(this.scheduler)
      .subscribe(
        { sendResultIfCallbackAvailable(it, null) },
        { sendResultIfCallbackAvailable(null, it.message) },
        { sendResultIfCallbackAvailable(StreamEventResult.Complete, null) }
      )

    compositeDisposable.add(disposable)
  }
  //endregion

  //region IGenericLifecycleOwner
  override fun deinitialize() {
    this.compositeDisposable.dispose()
  }
  //endregion
}