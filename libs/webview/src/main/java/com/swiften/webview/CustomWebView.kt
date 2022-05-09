package com.swiften.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class CustomWebView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = 0,
) : WebView(context, attrs, defStyle),
  IJavascriptEvaluator {
  var javascriptInterfaces: List<IJavascriptInterface> = arrayListOf()
    set(value) {
      if (field.isNotEmpty()) {
        throw Exception("Cannot set Javascript interfaces more than once")
      }

      field = value
    }

  init {
    this.let {
      it.settings.defaultTextEncodingName = "utf-8"
      it.settings.domStorageEnabled = true
      @SuppressLint("SetJavaScriptEnabled")
      it.settings.javaScriptEnabled = true

      // it.settings.builtInZoomControls = true
      // it.settings.displayZoomControls = false
      // it.settings.loadWithOverviewMode = true
      // it.settings.setSupportZoom(true)
      // it.settings.useWideViewPort = true

      it.webChromeClient = object : WebChromeClient() {}

      it.webViewClient = object : WebViewClient() {}
    }
  }

  /** Ensure Javascript evaluation occurs on the main thread */
  override fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {
    val mainHandler = Handler(this.context.mainLooper);

    mainHandler.post {
      super.evaluateJavascript(script, resultCallback)
    };
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    for (javascriptInterface in this.javascriptInterfaces) {
      this.addJavascriptInterface(javascriptInterface, javascriptInterface.name)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    for (javascriptInterface in this.javascriptInterfaces) {
      this.removeJavascriptInterface(javascriptInterface.name)
    }
  }
}
