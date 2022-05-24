package com.swiften.templateapp.webviewcentric.webview

import android.webkit.JavascriptInterface
import com.swiften.templateapp.webviewcentric.ILoggable
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.IBridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import io.reactivex.Flowable
import java.util.concurrent.TimeUnit

class AppJavascriptInterface (
  val argsParser: BridgeMethodArgumentsParser,
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
    val args = this.argsParser.parseArguments<CreateTestStreamArgs>(rawArgs = rawArgs)

    val stream = Flowable
      .interval(args.parameters.intervalMs, TimeUnit.MILLISECONDS)
      .map { CreateTestStreamResult(progress = it + 1) }
      .take(100)

    this.requestProcessor.processStream(stream = stream, bridgeArguments = args)
  }
}
