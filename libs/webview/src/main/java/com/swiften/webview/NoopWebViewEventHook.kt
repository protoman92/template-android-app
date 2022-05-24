package com.swiften.webview

import android.graphics.Bitmap
import android.webkit.WebView

object NoopWebViewEventHook : IWebViewEventHook {
  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {}

  override fun onPageFinished(view: WebView?, url: String?) {}
}
