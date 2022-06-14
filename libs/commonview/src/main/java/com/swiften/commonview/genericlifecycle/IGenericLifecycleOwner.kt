package com.swiften.commonview.genericlifecycle

/** Generic lifecycle that can apply to any use case */
interface IGenericLifecycleOwner {
  fun initialize()

  fun deinitialize()
}
