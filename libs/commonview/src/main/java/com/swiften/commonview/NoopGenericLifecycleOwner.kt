package com.swiften.commonview

object NoopGenericLifecycleOwner : IGenericLifecycleOwner {
  override fun initialize() {}

  override fun deinitialize() {}
}
