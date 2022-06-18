package com.swiften.webview.javascriptinterface.filepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import com.swiften.commonview.activity.IActivityResultEventHook
import com.swiften.commonview.activity.IActivityResultLauncher
import com.swiften.commonview.lifecycle.IGenericLifecycleOwner
import com.swiften.commonview.lifecycle.NoopGenericLifecycleOwner
import com.swiften.commonview.permission.IPermissionRequester
import com.swiften.commonview.utils.LazyProperty
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.BridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import com.swiften.webview.processStream
import io.reactivex.Completable
import io.reactivex.processors.PublishProcessor
import java.util.concurrent.atomic.AtomicReference

class FilePickerJavascriptInterface(
  override val name: String,
  private val activityResultLauncher: Lazy<IActivityResultLauncher<PickFileInput, PickFileOutput>>,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val context: Lazy<Context>,
  private val mimeTypeMap: Lazy<MimeTypeMap> = LazyProperty(initialValue = MimeTypeMap.getSingleton()),
  private val permissionRequester: Lazy<IPermissionRequester>,
  private val requestProcessor: Lazy<BridgeRequestProcessor>,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class MethodArguments {
    data class ObservePickFileResult(val requestID: String) : MethodArguments()

    data class PickFile(val message: String, val requestID: String) : MethodArguments()
  }

  sealed class MethodResult {
    data class PickFile(
      val requestID: String,
      val extension: String?,
      val mimeType: String?,
      val name: String?,
      val size: Long?,
      val uri: String?,
    ) : MethodResult() {
      companion object {
        internal val NOOP = PickFile(
          requestID = REQUEST_ID_NOOP,
          extension = null,
          mimeType = null,
          name = null,
          size = null,
          uri = null,
        )
      }
    }
  }

  companion object {
    private const val REQUEST_ID_NOOP = (-1).toString()
  }

  /**
   * Practically, there can only be one active file-picking request at one time, since the file
   * chooser [Intent] opens a system activity.
   */
  private val activeRequestID = AtomicReference(REQUEST_ID_NOOP)

  private val pickFileResultProcessor = PublishProcessor.create<PickFileOutput>()

  @JavascriptInterface
  fun observePickFileResult(rawRequest: String) {
    val request = this.argsParser.value
      .parseArguments<MethodArguments.ObservePickFileResult>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = this@FilePickerJavascriptInterface.pickFileResultProcessor,
      bridgeArguments = request,
    )
  }

  @JavascriptInterface
  fun pickFile(rawRequest: String) {
    val request = this.argsParser.value.parseArguments<MethodArguments.PickFile>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = Completable.defer {
        this@FilePickerJavascriptInterface.activeRequestID.set(request.parameters.requestID)

        this@FilePickerJavascriptInterface.activityResultLauncher.value.launch(
          input = PickFileInput,
          eventHook = object : IActivityResultEventHook<PickFileInput, PickFileOutput> {
            override fun createIntent(context: Context, input: PickFileInput): Intent {
              val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
              chooseFile.type = "*/*"
              return Intent.createChooser(chooseFile, request.parameters.message)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): PickFileOutput {
              val rawUri = intent?.dataString

              if (resultCode == Activity.RESULT_OK && rawUri != null) {
                val contentResolver = this@FilePickerJavascriptInterface.context.value.contentResolver
                val uri = Uri.parse(rawUri)
                val mimeType = contentResolver.getType(uri)

                val extension = this@FilePickerJavascriptInterface.mimeTypeMap.value
                  .getExtensionFromMimeType(mimeType)

                var fileName: String? = null
                var fileSize: Long? = null

                /**
                 * We are only given permission to read the file metadata right here, so we must
                 * acquire as much information as we can.
                 */
                contentResolver.query(
                  uri,
                  null,
                  null,
                  null,
                  null,
                )?.let { cursor ->
                  val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                  val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                  cursor.moveToFirst()
                  fileName = cursor.getString(nameIndex)
                  fileSize = cursor.getLong(sizeIndex)
                  cursor.close()
                }

                return PickFileOutput(
                  requestID = this@FilePickerJavascriptInterface.activeRequestID.get(),
                  extension = extension,
                  mimeType = mimeType,
                  name = fileName,
                  size = fileSize,
                  uri = rawUri,
                )
              }

              return PickFileOutput.NOOP
            }

            override fun onActivityResult(output: PickFileOutput) {
              this@FilePickerJavascriptInterface.pickFileResultProcessor.onNext(output)
            }
          },
        )

        Completable.complete()
      },
      bridgeArguments = request,
    )
  }
}
