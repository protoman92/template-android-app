package com.swiften.webview

interface IJavascriptInterface {
  val name: String
}

data class BridgeMethodArguments<Parameters>(
  val callback: String,
  val method: String,
  val module: String,
  val parameters: Parameters
)
