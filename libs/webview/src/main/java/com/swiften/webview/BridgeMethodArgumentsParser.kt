package com.swiften.webview

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BridgeMethodArgumentsParser(val gson: Gson)

inline fun <reified Params> BridgeMethodArgumentsParser.parseArguments(rawArgs: String): BridgeMethodArguments<Params> {
  return this.gson.fromJson(rawArgs, object : TypeToken<BridgeMethodArguments<Params>>() {}.type)
}
