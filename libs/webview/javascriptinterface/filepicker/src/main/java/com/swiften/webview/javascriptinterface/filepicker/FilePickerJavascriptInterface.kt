package com.swiften.webview.javascriptinterface.filepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import com.swiften.commonview.activity.IActivityResultEventHook
import com.swiften.commonview.activity.IActivityResultLauncher
import com.swiften.commonview.lifecycle.IGenericLifecycleOwner
import com.swiften.commonview.lifecycle.NoopGenericLifecycleOwner
import com.swiften.commonview.permission.IPermissionRequester
import com.swiften.commonview.utils.CommonUtils
import com.swiften.commonview.utils.LazyProperty
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.BridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import com.swiften.webview.processStream
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicReference

class FilePickerJavascriptInterface(
  override val name: String,
  private val activityResultLauncher: Lazy<IActivityResultLauncher<PickFileInput, PickFileOutput>>,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val context: Lazy<Context>,
  private val mimeTypeMap: Lazy<MimeTypeMap> = LazyProperty(initialValue = MimeTypeMap.getSingleton()),
  private val permissionRequester: Lazy<IPermissionRequester>,
  private val requestProcessor: Lazy<BridgeRequestProcessor>,
  /**
   * If this is true, save selected files to the app's internal storage, and then expose its URI.
   * https://commonsware.com/blog/2016/03/15/how-consume-content-uri.html
   */
  private val saveToInternalStorage: Boolean = true,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class MethodArguments {
    data class ObservePickFileResult(val requestID: String) : MethodArguments()

    data class PickFile(
      val message: String,
      val mimeType: String?,
      val requestID: String,
    ) : MethodArguments()
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
      stream = Single.create<PickFileOutput> { emitter ->
        this@FilePickerJavascriptInterface.activeRequestID.set(request.parameters.requestID)

        this@FilePickerJavascriptInterface.activityResultLauncher.value.launch(
          input = PickFileInput,
          eventHook = object : IActivityResultEventHook<PickFileInput, PickFileOutput> {
            override fun createIntent(context: Context, input: PickFileInput): Intent {
              val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
              chooseFile.type = request.parameters.mimeType ?: "*/*"
              return Intent.createChooser(chooseFile, request.parameters.message)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): PickFileOutput {
              var contentCursor: Cursor? = null
              var inputStream: InputStream? = null
              var outputStream: OutputStream? = null
              val rawUri = intent?.dataString

              if (resultCode == Activity.RESULT_OK && rawUri != null) {
                val defaultErrorMessage = "$rawUri does not point to a valid content"

                try {
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
                  contentCursor = contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null,
                  )?.also { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    fileName = cursor.getString(nameIndex)
                    fileSize = cursor.getLong(sizeIndex)
                  }

                  var finalUri = rawUri

                  if (this@FilePickerJavascriptInterface.saveToInternalStorage) {
                    inputStream = contentResolver.openInputStream(uri)

                    if (inputStream == null || fileName.isNullOrEmpty()) {
                      throw FileNotFoundException(defaultErrorMessage)
                    }

                    outputStream = this@FilePickerJavascriptInterface.context.value
                      .openFileOutput(fileName, Context.MODE_PRIVATE)

                    CommonUtils.transferInputToOutput(source = inputStream, sink = outputStream)

                    val internalStoragePath = arrayListOf(
                      this@FilePickerJavascriptInterface.context.value.filesDir.absolutePath,
                      fileName,
                    ).joinToString(separator = File.separator)

                    finalUri = Uri.fromFile(File(internalStoragePath)).toString()
                  }

                  return PickFileOutput(
                    requestID = this@FilePickerJavascriptInterface.activeRequestID.get(),
                    extension = extension,
                    mimeType = mimeType,
                    name = fileName,
                    size = fileSize,
                    uri = finalUri,
                  )
                } catch (error: Exception) {
                  emitter.onError(error)
                } finally {
                  contentCursor?.close()
                  inputStream?.close()
                  outputStream?.close()
                }
              }

              return PickFileOutput.NOOP
            }

            override fun onActivityResult(output: PickFileOutput) {
              emitter.onSuccess(output)
            }
          },
        )
      }.doOnSuccess {
        this@FilePickerJavascriptInterface.pickFileResultProcessor.onNext(it)
      }.ignoreElement(),
      bridgeArguments = request,
    )
  }
}
