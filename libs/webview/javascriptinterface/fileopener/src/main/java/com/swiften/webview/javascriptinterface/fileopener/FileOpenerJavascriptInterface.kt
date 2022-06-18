package com.swiften.webview.javascriptinterface.fileopener

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import com.swiften.commonview.activity.IActivityStarter
import com.swiften.commonview.lifecycle.IGenericLifecycleOwner
import com.swiften.commonview.lifecycle.NoopGenericLifecycleOwner
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.BridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import com.swiften.webview.processStream
import io.reactivex.Completable


class FileOpenerJavascriptInterface(
  override val name: String,
  private val activityStarter: Lazy<IActivityStarter>,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val requestProcessor: Lazy<BridgeRequestProcessor>,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class MethodArguments {
    data class OpenFile(val mimeType: String, val uri: String) : MethodArguments()
  }

  @JavascriptInterface
  fun openFile(rawRequest: String) {
    val request = this.argsParser.value.parseArguments<MethodArguments.OpenFile>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = Completable.defer {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.setDataAndType(Uri.parse(request.parameters.uri), request.parameters.mimeType)

        try {
          this@FileOpenerJavascriptInterface.activityStarter.value.startActivity(intent = intent)
          Completable.complete()
        } catch (e: ActivityNotFoundException) {
          Completable.error(e)
        }
      },
      bridgeArguments = request,
    )
  }
}
