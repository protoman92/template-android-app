package com.swiften.templateapp.webview

import android.webkit.JavascriptInterface
import com.swiften.templateapp.ILoggable
import com.swiften.webview.IBridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class AppJavascriptInterface (
  val argsParser: JavascriptArgumentsParser,
  val requestProcessor: IBridgeRequestProcessor,
) : ILoggable,
  IJavascriptInterface {
  data class CreateTestStreamArgs(val intervalMs: Long)

  data class CreateTestStreamResult(val progress: Long)

  //region IJavascriptInterface
  override val name get() = "AppModule"
  //endregion

  @JavascriptInterface
  fun createTestStream(rawArgs: String) {
    val args = this.argsParser.parseArguments<CreateTestStreamArgs>(rawArgs)

    val stream = Observable
      .interval(args.parameters.intervalMs, TimeUnit.MILLISECONDS)
      .map { CreateTestStreamResult(progress = it + 1) }
      .take(100)

    this.requestProcessor.processStream(stream, args)
  }
}
