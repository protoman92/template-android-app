package com.swiften.webview

import com.swiften.commonview.lifecycle.IGenericLifecycleOwner

interface IWebView :
  IGenericLifecycleOwner,
  IJavascriptEvaluator,
  IWebViewEventHookRegistry
{
  var javascriptInterfaces: List<IJavascriptInterface>

  fun canGoBack(): Boolean

  fun disableInteractions()

  fun enableInteractions()

  fun getUrl(): String?

  fun goBack()

  fun loadUrl(url: String)

  fun reload()
}
