package com.swiften.commonview

class NoopGenericLifecycleOwner : IGenericLifecycleOwner {
  override fun initialize() {}

  override fun deinitialize() {}
}
