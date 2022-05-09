package com.swiften.templateapp.webview

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.swiften.webview.BridgeMethodArguments

class JavascriptArgumentsParser(val gson: Gson)

inline fun <reified Params> JavascriptArgumentsParser.parseArguments(rawArgs: String): BridgeMethodArguments<Params> {
  return this.gson.fromJson(rawArgs, object : TypeToken<BridgeMethodArguments<Params>>() {}.type)
}
