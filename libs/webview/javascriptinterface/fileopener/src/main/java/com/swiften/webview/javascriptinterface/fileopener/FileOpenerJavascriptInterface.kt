package com.swiften.webview.javascriptinterface.fileopener

import android.annotation.SuppressLint
import android.content.Context
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
import java.io.File

class FileOpenerJavascriptInterface(
  override val name: String,
  private val activityStarter: Lazy<IActivityStarter>,
  /**
   * Whether to allow world-readability for files in the internal storage. This has security
   * implications, so take care when using this.
   */
  private val allowInsecureWorldReadable: Boolean = false,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val context: Lazy<Context>,
  private val requestProcessor: Lazy<BridgeRequestProcessor>,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class MethodArguments {
    data class OpenFile(val mimeType: String, val uri: String) : MethodArguments()
  }

  @SuppressLint("SetWorldReadable")
  @JavascriptInterface
  fun openFile(rawRequest: String) {
    val request = this.argsParser.value.parseArguments<MethodArguments.OpenFile>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = Completable.defer {
        val internalStorageDir = this@FileOpenerJavascriptInterface.context.value.filesDir.absolutePath
        val rawUri = request.parameters.uri
        var uri = Uri.parse(rawUri)
        var uriWithoutScheme = ""

        /**
         * If this URI points to a file in the internal storage, we have the option to enable
         * world-readability on it. Keep in mind that this is potentially insecure.
         */
        if (
          this@FileOpenerJavascriptInterface.allowInsecureWorldReadable &&
          uri.scheme == "file" &&
          rawUri.substring("file://".length).also {
            uriWithoutScheme = it
          }.startsWith(internalStorageDir)
        ) {
          val internalStorageFile = File(uriWithoutScheme)
          internalStorageFile.setReadable(true, false)
          uri = Uri.fromFile(internalStorageFile)
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.setDataAndType(uri, request.parameters.mimeType)

        try {
          this@FileOpenerJavascriptInterface.activityStarter.value.startActivity(intent = intent)
          Completable.complete()
        } catch (error: Exception) {
          Completable.error(error)
        }
      },
      bridgeArguments = request,
    )
  }
}
