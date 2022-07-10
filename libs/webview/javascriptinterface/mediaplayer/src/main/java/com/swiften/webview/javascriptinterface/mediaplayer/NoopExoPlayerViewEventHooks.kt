package com.swiften.webview.javascriptinterface.mediaplayer

object NoopExoPlayerViewEventHooks : IExoPlayerViewEventHooks {
  override fun onExoPlayerViewHidden() {}

  override fun onExoPlayerViewShown() {}
}
