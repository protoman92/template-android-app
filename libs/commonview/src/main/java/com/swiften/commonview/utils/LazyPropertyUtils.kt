package com.swiften.commonview.utils

fun <T1: Any, T2 : Any> IReadonlyLazyProperty<T1>.map(mapper: (T1) -> T2): IReadonlyLazyProperty<T2> {
  return object : IReadonlyLazyProperty<T2> {
    override val value: T2 get() {
      return mapper(this@map.value)
    }

    override fun isInitialized(): Boolean {
      return this@map.isInitialized()
    }
  }
}
