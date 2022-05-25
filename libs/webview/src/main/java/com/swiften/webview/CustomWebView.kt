package com.swiften.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.util.AttributeSet
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat

class CustomWebView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = 0,
) : WebView(context, attrs, defStyle),
  IWebView
{
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

      it.webViewClient = object : WebViewClientCompat() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
          super.onPageStarted(view, url, favicon)

          for (eventHook in this@CustomWebView.eventHooks) {
            eventHook.onPageStarted(view = view, url = url, favicon = favicon)
          }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)

          for (eventHook in this@CustomWebView.eventHooks) {
            eventHook.onPageFinished(view = view, url = url)
          }
        }
      }
    }
  }

  /** Hook into webview events, since we are creating the web view client internally within this class */
  private var eventHooks: MutableSet<IWebViewEventHook> = mutableSetOf()

  //region IWebView
  override var javascriptInterfaces: List<IJavascriptInterface> = arrayListOf()

  /** Ensure Javascript evaluation occurs on the main thread */
  override fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {
    val mainHandler = Handler(this.context.mainLooper);
    mainHandler.post { super.evaluateJavascript(script, resultCallback) }
  }

  /** Ensure goBack happens on the main thread */
  override fun goBack() {
    val mainHandler = Handler(this.context.mainLooper);
    mainHandler.post { super.goBack() }
  }

  /** Ensure reload happens on the main thread */
  override fun reload() {
    val mainHandler = Handler(this.context.mainLooper);
    mainHandler.post { super.reload() }
  }
  //endregion

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

  fun addEventHook(hook: IWebViewEventHook) {
    this.eventHooks.add(hook)
  }

  fun removeEventHook(hook: IWebViewEventHook) {
    this.eventHooks.remove(hook)
  }
}
