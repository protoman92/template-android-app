package com.swiften.webview

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class CustomWebView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = 0
) : WebView(context, attrs, defStyle) {}
