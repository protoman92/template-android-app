package com.swiften.commonview

/** Generic lifecycle that can apply to any use case */
interface IGenericLifecycleOwner {
  fun initialize()

  fun deinitialize()
}
