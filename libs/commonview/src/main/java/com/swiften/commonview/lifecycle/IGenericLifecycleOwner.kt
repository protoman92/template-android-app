package com.swiften.commonview.lifecycle

/** Generic lifecycle that can apply to any use case */
interface IGenericLifecycleOwner {
  fun initialize()

  fun deinitialize()
}
