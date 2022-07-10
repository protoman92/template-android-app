package com.swiften.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
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

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
          super.doUpdateVisitedHistory(view, url, isReload)

          for (eventHook in this@CustomWebView.eventHooks) {
            eventHook.doUpdateVisitedHistory(view = view, url = url, isReload = isReload)
          }
        }
      }
    }
  }

  /** Hook into webview events, since we are creating the web view client internally within this class */
  private val eventHooks: MutableSet<IWebViewEventHook> = mutableSetOf()

  @Suppress("ClickableViewAccessibility")
  private val noTouchListener: OnTouchListener by lazy {
    OnTouchListener { _, _ -> true }
  }

  //region IGenericLifecycleOwner
  override fun initialize() {
    for (javascriptInterface in this.javascriptInterfaces) {
      this.addJavascriptInterface(javascriptInterface, javascriptInterface.name)
      javascriptInterface.initialize()
    }
  }

  override fun deinitialize() {
    for (javascriptInterface in this.javascriptInterfaces) {
      javascriptInterface.deinitialize()
      this.removeJavascriptInterface(javascriptInterface.name)
    }
  }
  //endregion

  //region IWebView
  override var javascriptInterfaces: List<IJavascriptInterface> = arrayListOf()

  @SuppressLint("ClickableViewAccessibility")
  override fun enableInteractions() {
    this.setOnTouchListener(null)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun disableInteractions() {
    this.setOnTouchListener(this.noTouchListener)
  }

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

  //region IWebViewEventHookRegistry
  override fun registerEventHook(eventHook: IWebViewEventHook) {
    this.eventHooks.add(eventHook)
  }

  override fun unregisterEventHook(eventHook: IWebViewEventHook) {
    this.eventHooks.remove(eventHook)
  }
  //endregion
}
