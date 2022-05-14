package com.swiften.templateapp.webviewcentric

import android.util.Log

interface IDependency : MainFragment.IDependency {}

interface ILoggable {
  val logTag: String get() = getLogTag(target = this)
  fun logI(message: String) = Log.i(this.logTag, message)
}
