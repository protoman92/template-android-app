package com.swiften.webview

import android.content.SharedPreferences
import android.webkit.JavascriptInterface
import com.swiften.commonview.IGenericLifecycleOwner
import com.swiften.commonview.NoopGenericLifecycleOwner
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import java.lang.Exception

class SharedPreferencesJavascriptInterface(
  override val name: String,
  private val argsParser: BridgeMethodArgumentsParser,
  private val requestProcessor: IBridgeRequestProcessor,
  private val sharedPreferences: SharedPreferences,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner()
{
  sealed class MethodArguments {
    data class GetString(val key: String) : MethodArguments()

    data class SetString(val key: String, val value: String?) : MethodArguments()
  }

  sealed class MethodResult {
    data class GetString(val value: String?) : MethodResult()
  }

  data class UnableToSetStringValueError(val key: String, val value: String?) : Exception(
    "Unable to set value \"$value\" for key \"$key\""
  )

  private val stringSubject = BehaviorSubject.create<MethodArguments.SetString>()

  @JavascriptInterface
  fun getString(rawArgs: String) {
    val args = this.argsParser.parseArguments<MethodArguments.GetString>(rawArgs = rawArgs)

    val stream = Single.defer {
      val value = this.sharedPreferences.getString(args.parameters.key, null)
      Single.just(MethodResult.GetString(value = value))
    }

    this.requestProcessor.processStream(stream = stream, bridgeArguments = args)
  }

  @JavascriptInterface
  fun observeString(rawArgs: String) {
    val args = this.argsParser.parseArguments<MethodArguments.GetString>(rawArgs = rawArgs)
    val startingValue = this.sharedPreferences.getString(args.parameters.key, null)

    val stream = this.stringSubject
      .startWith(MethodArguments.SetString(key = args.parameters.key, value = startingValue))

    this.requestProcessor.processStream(stream = stream, bridgeArguments = args)
  }

  @JavascriptInterface
  fun setString(rawArgs: String) {
    val args = this.argsParser.parseArguments<MethodArguments.SetString>(rawArgs = rawArgs)

    val stream = Single.defer {
      val didSucceed = this.sharedPreferences.edit().putString(args.parameters.key, args.parameters.value).commit()

      if (didSucceed) {
        this.stringSubject.onNext(args.parameters)
        Single.just(null)
      } else {
        Single.error(UnableToSetStringValueError(key = args.parameters.key, value = args.parameters.value))
      }
    }

    this.requestProcessor.processStream(stream = stream, bridgeArguments = args)
  }
}
