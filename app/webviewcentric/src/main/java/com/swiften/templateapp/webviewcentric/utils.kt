package com.swiften.templateapp.webviewcentric

fun getLogTag(target: Any): String {
  return target.javaClass.simpleName
}
