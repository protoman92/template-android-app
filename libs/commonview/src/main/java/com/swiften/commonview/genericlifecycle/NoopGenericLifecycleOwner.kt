package com.swiften.commonview.genericlifecycle

object NoopGenericLifecycleOwner : IGenericLifecycleOwner {
  override fun initialize() {}

  override fun deinitialize() {}
}
