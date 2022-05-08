package com.swiften.templateapp

fun getLogTag(target: Any): String {
  return target.javaClass.simpleName
}
