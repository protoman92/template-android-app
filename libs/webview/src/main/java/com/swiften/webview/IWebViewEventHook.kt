package com.swiften.webview

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat

/** Mimics the methods exposed by [WebViewClientCompat] */
interface IWebViewEventHook {
  fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean)

  fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?)

  fun onPageFinished(view: WebView?, url: String?)
}
