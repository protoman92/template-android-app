package com.swiften.webview.javascriptinterface.fileopener

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
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
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

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
  sealed interface MethodArguments {
    data class OpenFile(val mimeType: String, val uri: String) : MethodArguments

    data class ReadFile(val encoding: Encoding, val uri: String) : MethodArguments {
      companion object {
        @Suppress("EnumEntryName")
        enum class Encoding {
          base64
        }
      }
    }
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
          rawUri.removePrefix("file://").also {
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

  @JavascriptInterface
  fun readFile(rawRequest: String) {
    val request = this.argsParser.value.parseArguments<MethodArguments.ReadFile>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = Single.defer {
        val internalStorageDir = this@FileOpenerJavascriptInterface.context.value.filesDir
        val internalStorageRawUri = Uri.fromFile(internalStorageDir).toString()
        val rawUri = request.parameters.uri
        var inputStream: InputStream? = null

        try {
          val uri = Uri.parse(rawUri)

          inputStream = if (request.parameters.uri.startsWith(internalStorageRawUri)) {
            val filePath = request.parameters.uri
              .substring(internalStorageRawUri.length)
              .removePrefix("/")

            this@FileOpenerJavascriptInterface.context.value.openFileInput(filePath)
          } else {
            this@FileOpenerJavascriptInterface.context.value.contentResolver.openInputStream(uri)
          }

          if (inputStream == null) {
            throw FileNotFoundException("$rawUri does not point to a valid file")
          }

          val contents = when (request.parameters.encoding) {
            MethodArguments.ReadFile.Companion.Encoding.base64 -> {
              Base64.encodeToString(inputStream.readBytes(), Base64.NO_WRAP)
            }
          }

          Single.just(contents)
        } catch (error: Exception) {
          Single.error(error)
        } finally {
          inputStream?.close()
        }
      }.subscribeOn(
        /**
         * Since we are reading contents from an input stream, we need to execute this from another
         * thread other than the main thread, otherwise it might block for a long time while reading
         * a large file.
         */
        Schedulers.io()
      ),
      bridgeArguments = request,
    )
  }
}
