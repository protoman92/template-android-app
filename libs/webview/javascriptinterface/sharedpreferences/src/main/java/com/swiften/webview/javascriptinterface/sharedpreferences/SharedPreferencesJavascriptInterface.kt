package com.swiften.webview.javascriptinterface.sharedpreferences

import android.content.SharedPreferences
import android.webkit.JavascriptInterface
import com.swiften.commonview.lifecycle.IGenericLifecycleOwner
import com.swiften.commonview.lifecycle.NoopGenericLifecycleOwner
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.IBridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import com.swiften.webview.processStream
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import java.lang.Exception

class SharedPreferencesJavascriptInterface(
  override val name: String,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val requestProcessor: Lazy<IBridgeRequestProcessor>,
  private val sharedPreferences: Lazy<SharedPreferences>,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed class MethodArguments {
    data class GetString(val key: String) : MethodArguments()

    data class RemoveString(val key: String) : MethodArguments()

    data class SetString(val key: String, val value: String?) : MethodArguments()
  }

  sealed class MethodResult {
    data class GetString(val value: String?) : MethodResult()
  }

  data class UnableToRemoveStringValueError(val key: String) : Exception(
    "Unable to remove key \"$key\""
  )

  data class UnableToSetStringValueError(val key: String, val value: String?) : Exception(
    "Unable to set value \"$value\" for key \"$key\""
  )

  private val stringProcessor = BehaviorProcessor.create<MethodArguments.SetString>()

  @JavascriptInterface
  fun getString(rawArgs: String) {
    val args = this.argsParser.value.parseArguments<MethodArguments.GetString>(rawArgs = rawArgs)

    this.requestProcessor.value.processStream(
      stream = Single.defer {
        val value = this@SharedPreferencesJavascriptInterface.sharedPreferences.value
          .getString(args.parameters.key, null)

        Single.just(MethodResult.GetString(value = value))
      },
      bridgeArguments = args,
    )
  }

  @JavascriptInterface
  fun getAll(rawArgs: String) {
    val args = this.argsParser.value.parseArguments<Unit>(rawArgs = rawArgs)

    this.requestProcessor.value.processStream(
      stream = Single.defer {
        Single.just(this@SharedPreferencesJavascriptInterface.sharedPreferences.value.all)
      },
      bridgeArguments = args,
    )
  }

  @JavascriptInterface
  fun observeString(rawArgs: String) {
    val args = this.argsParser.value.parseArguments<MethodArguments.GetString>(rawArgs = rawArgs)

    val startingValue = this@SharedPreferencesJavascriptInterface.sharedPreferences.value
      .getString(args.parameters.key, null)

    val stream = this@SharedPreferencesJavascriptInterface.stringProcessor
      .startWith(MethodArguments.SetString(key = args.parameters.key, value = startingValue))

    this.requestProcessor.value.processStream(stream = stream, bridgeArguments = args)
  }

  @JavascriptInterface
  fun removeString(rawArgs: String) {
    val args = this.argsParser.value.parseArguments<MethodArguments.RemoveString>(rawArgs = rawArgs)

    val stream = Completable.defer {
      val didSucceed = this@SharedPreferencesJavascriptInterface.sharedPreferences.value
        .edit()
        .remove(args.parameters.key)
        .commit()

      if (didSucceed) {
        Completable.complete()
      } else {
        Completable.error(UnableToRemoveStringValueError(
          key = args.parameters.key,
        ))
      }
    }

    this.requestProcessor.value.processStream(stream = stream, bridgeArguments = args)
  }

  @JavascriptInterface
  fun setString(rawArgs: String) {
    val args = this.argsParser.value.parseArguments<MethodArguments.SetString>(rawArgs = rawArgs)

    val stream = Completable.defer {
      val didSucceed = this@SharedPreferencesJavascriptInterface.sharedPreferences.value
        .edit()
        .putString(args.parameters.key, args.parameters.value)
        .commit()

      if (didSucceed) {
        this@SharedPreferencesJavascriptInterface.stringProcessor.onNext(args.parameters)
        Completable.complete()
      } else {
        Completable.error(UnableToSetStringValueError(
          key = args.parameters.key,
          value = args.parameters.value,
        ))
      }
    }

    this.requestProcessor.value.processStream(stream = stream, bridgeArguments = args)
  }
}
