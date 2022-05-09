package com.swiften.templateapp.webview

import android.webkit.JavascriptInterface
import com.swiften.templateapp.ILoggable
import com.swiften.webview.IJavascriptInterface

class AppJavascriptInterface (val argsParser: JavascriptArgumentsParser) : ILoggable,
  IJavascriptInterface {
  data class TestArgs(val a: Int)

  //region IJavascriptInterface
  override val name get() = "AppModule"
  //endregion

  @JavascriptInterface
  fun test(rawArgs: String) {
    val args = this.argsParser.parseArguments<TestArgs>(rawArgs)
    this.logI(args.parameters.toString())
  }
}
