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
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.BridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import com.swiften.webview.processStream
import io.reactivex.Completable
import io.reactivex.Single
import java.util.concurrent.atomic.AtomicReference

class FilePickerJavascriptInterface(
  override val name: String,
  private val activityResultLauncher: IActivityResultLauncher<PickFileInput, PickFileOutput>,
  private val argsParser: BridgeMethodArgumentsParser,
  private val requestProcessor: BridgeRequestProcessor,
  private val persistentState: PersistentState = PersistentState.create()
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  /**
   * Since the opening the file picker activity might lead to the recreation of
   * [FilePickerJavascriptInterface], we might want to inject the [PersistentState] manually based
   * on external conditions.
   *
   * For example, if we register [FilePickerJavascriptInterface] in [Fragment.onStart], and then
   * call [Fragment.registerForActivityResult], when the user finishes picking a file,
   * [IActivityResultEventHook.onActivityResult] is actually called before [Fragment.onStart] is
   * called, resulting in the [FilePickerJavascriptInterface] being replaced shortly afterwards,
   * thus losing its state up till then.
   */
  class PersistentState private constructor() {
    companion object {
      fun create(): PersistentState {
        return PersistentState()
      }
    }

    /**
     * Practically, there can only be one active file-picking request at one time, since the
     * file chooser [Intent] opens a system activity.
     */
    internal val activeRequestID = AtomicReference(REQUEST_ID_NOOP)

    /**
     * When we open the file picker activity, the current web view page will be destroyed and then
     * recreated again once the file-picking is complete. Thus, it might not be possible to have the
     * file result be delivered to the web in one [pickFile] call.
     *
     * The web app should thus get the latest file-picking result using [activePickFileResult].
     */
    internal val activePickFileResult = AtomicReference(PickFileOutput.NOOP)
  }

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

  @JavascriptInterface
  fun getLatestPickFileResult(rawRequest: String) {
    val request = this.argsParser.parseArguments<MethodArguments.ObservePickFileResult>(rawRequest)

    this.requestProcessor.processStream(
      stream = Single.defer {
        Single.just(this@FilePickerJavascriptInterface.persistentState
          .activePickFileResult.getAndSet(PickFileOutput.NOOP))
      },
      bridgeArguments = request,
    )
  }

  @JavascriptInterface
  fun pickFile(rawRequest: String) {
    val request = this.argsParser.parseArguments<MethodArguments.PickFile>(rawRequest)

    this.requestProcessor.processStream(
      stream = Completable.defer {
        this@FilePickerJavascriptInterface
          .persistentState.activeRequestID.set(request.parameters.requestID)

        activityResultLauncher.launch(
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
                  requestID = this@FilePickerJavascriptInterface.persistentState.activeRequestID.get(),
                  uri = uri,
                )
              }

              return PickFileOutput.NOOP
            }

            override fun onActivityResult(output: PickFileOutput) {
              this@FilePickerJavascriptInterface.persistentState.activePickFileResult.set(output)
            }
          },
        )

        Completable.complete()
      },
      bridgeArguments = request,
    )
  }
}
