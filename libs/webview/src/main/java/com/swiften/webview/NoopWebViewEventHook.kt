package com.swiften.webview

import android.webkit.WebView

object NoopWebViewEventHook : IWebViewEventHook {
  override fun onPageFinished(view: WebView?, url: String?) {}
}
