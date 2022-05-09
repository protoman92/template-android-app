package com.swiften.templateapp

import android.util.Log
import com.google.gson.Gson

interface IDependency : MainFragment.IDependency {}

interface ILoggable {
  val logTag: String get() = getLogTag(target = this)
  fun logI(message: String) = Log.i(this.logTag, message)
}
