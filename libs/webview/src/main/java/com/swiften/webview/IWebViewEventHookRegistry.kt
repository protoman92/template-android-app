package com.swiften.webview

interface IWebViewEventHookRegistry {
  fun registerEventHook(eventHook: IWebViewEventHook)

  fun unregisterEventHook(eventHook: IWebViewEventHook)
}
