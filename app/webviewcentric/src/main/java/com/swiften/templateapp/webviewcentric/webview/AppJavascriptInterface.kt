package com.swiften.templateapp.webviewcentric.webview

import android.webkit.JavascriptInterface
import com.swiften.commonview.genericlifecycle.IGenericLifecycleOwner
import com.swiften.commonview.genericlifecycle.NoopGenericLifecycleOwner
import com.swiften.templateapp.webviewcentric.ILoggable
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.IBridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import io.reactivex.Flowable
import java.util.concurrent.TimeUnit

class AppJavascriptInterface (
  override val name: String,
  val argsParser: Lazy<BridgeMethodArgumentsParser>,
  val requestProcessor: Lazy<IBridgeRequestProcessor>,
) : ILoggable,
  IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  data class CreateTestStreamArgs(val intervalMs: Long)

  data class CreateTestStreamResult(val progress: Long)

  @JavascriptInterface
  fun createTestStream(rawArgs: String) {
    val args = this.argsParser.value.parseArguments<CreateTestStreamArgs>(rawArgs = rawArgs)

    val stream = Flowable
      .interval(args.parameters.intervalMs, TimeUnit.MILLISECONDS)
      .map { CreateTestStreamResult(progress = it + 1) }
      .take(100)

    this.requestProcessor.value.processStream(stream = stream, bridgeArguments = args)
  }
}
