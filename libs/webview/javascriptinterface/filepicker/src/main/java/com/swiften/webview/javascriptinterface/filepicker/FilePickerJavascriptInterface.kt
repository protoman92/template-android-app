package com.swiften.webview.javascriptinterface.filepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.fragment.app.Fragment
import com.swiften.commonview.genericlifecycle.IGenericLifecycleOwner
import com.swiften.commonview.genericlifecycle.NoopGenericLifecycleOwner
import com.swiften.commonview.activityresult.IActivityResultEventHook
import com.swiften.commonview.activityresult.IActivityResultLauncher
import com.swiften.commonview.utils.LazyProperty
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.BridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import com.swiften.webview.processStream
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import java.util.concurrent.atomic.AtomicReference

class FilePickerJavascriptInterface(
  override val name: String,
  private val activityResultLauncher: Lazy<IActivityResultLauncher<PickFileInput, PickFileOutput>>,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val requestProcessor: Lazy<BridgeRequestProcessor>,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class MethodArguments {
    data class ObservePickFileResult(val requestID: String) : MethodArguments()

    data class PickFile(val message: String, val requestID: String) : MethodArguments()
  }

  sealed class MethodResult {
    data class PickFile(val requestID: String, val uri: String?) : MethodResult() {
      companion object {
        internal val NOOP = PickFile(requestID = REQUEST_ID_NOOP, uri = null)
      }
    }
  }

  companion object {
    private const val REQUEST_ID_NOOP = (-1).toString()
  }

  /**
   * Practically, there can only be one active file-picking request at one time, since the
   * file chooser [Intent] opens a system activity.
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

        activityResultLauncher.value.launch(
          input = PickFileInput,
          eventHook = object : IActivityResultEventHook<PickFileInput, PickFileOutput> {
            override fun createIntent(context: Context, input: PickFileInput): Intent {
              val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
              chooseFile.type = "*/*"
              return Intent.createChooser(chooseFile, request.parameters.message)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): PickFileOutput {
              val uri = intent?.dataString

              if (resultCode == Activity.RESULT_OK && uri != null) {
                return PickFileOutput(
                  requestID = this@FilePickerJavascriptInterface.activeRequestID.get(),
                  uri = uri,
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
