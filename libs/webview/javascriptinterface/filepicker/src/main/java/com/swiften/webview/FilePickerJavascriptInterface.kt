package com.swiften.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import com.swiften.commonview.IGenericLifecycleOwner
import com.swiften.commonview.NoopGenericLifecycleOwner
import com.swiften.commonview.activityresult.IActivityResultEventHook
import com.swiften.commonview.activityresult.IActivityResultLauncher
import io.reactivex.Single

typealias PickFileInput = Unit
typealias PickFileOutput = String?

class FilePickerJavascriptInterface(
  override val name: String,
  private val activityResultLauncher: IActivityResultLauncher<PickFileInput, PickFileOutput>,
  private val argsParser: BridgeMethodArgumentsParser,
  private val requestProcessor: BridgeRequestProcessor,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class MethodArguments {
    data class PickFile(val message: String) : MethodArguments()
  }

  sealed class MethodResult {
    data class PickFile(val uri: PickFileOutput) : MethodResult()
  }

  @JavascriptInterface
  fun pickFile(rawRequest: String) {
    val request = this.argsParser.parseArguments<MethodArguments.PickFile>(rawRequest)

    this.requestProcessor.processStream(
      stream = Single.create<MethodResult.PickFile> { emitter ->
        activityResultLauncher.launch(
          input = PickFileInput,
          eventHook = object : IActivityResultEventHook<PickFileInput, PickFileOutput> {
            override fun createIntent(context: Context, input: PickFileInput): Intent {
              val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
              chooseFile.type = "*/*"
              return Intent.createChooser(chooseFile, request.parameters.message)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): PickFileOutput {
              return when (resultCode) {
                Activity.RESULT_OK -> intent?.dataString
                else -> null
              }
            }

            override fun onActivityResult(output: PickFileOutput) {
              emitter.onSuccess(MethodResult.PickFile(uri = output))
            }
          },
        )
      },
      bridgeArguments = request,
    )
  }
}
