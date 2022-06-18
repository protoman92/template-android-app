package com.swiften.commonview.lifecycle

object NoopGenericLifecycleOwner : IGenericLifecycleOwner {
  override fun initialize() {}

  override fun deinitialize() {}
}
