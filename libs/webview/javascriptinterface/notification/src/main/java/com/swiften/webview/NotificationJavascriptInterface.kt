package com.swiften.webview

import android.os.Handler
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Single

class NotificationJavascriptInterface(
  override val name: String,
  private val argsParser: BridgeMethodArgumentsParser,
  private val parentView: View,
  private val requestProcessor: IBridgeRequestProcessor
) : IJavascriptInterface {
  sealed class MethodArguments {
    data class ShowNotification(
      val durationMs: Int,
      val message: String,
    ) : MethodArguments()
  }

  @JavascriptInterface
  fun showNotification(rawArgs: String) {
    val args = this.argsParser.parseArguments<MethodArguments.ShowNotification>(rawArgs = rawArgs)

    this.requestProcessor.processStream(
      stream = Single.defer {
        this@NotificationJavascriptInterface.showNotificationOnMainThread(args.parameters)
        Single.just(Unit)
      },
      bridgeArguments = args,
    )
  }

  private fun showNotificationOnMainThread(args: MethodArguments.ShowNotification) {
    val mainHandler = Handler(this.parentView.context.mainLooper);

    mainHandler.post {
      val snackBar = Snackbar.make(this.parentView, args.message, args.durationMs,)
      snackBar.show()

      snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).also {
        it.gravity = Gravity.CENTER_HORIZONTAL
        it.textAlignment = View.TEXT_ALIGNMENT_CENTER
      }
    }
  }
}
