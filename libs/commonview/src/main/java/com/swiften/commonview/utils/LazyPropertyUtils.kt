package com.swiften.commonview.utils

fun <T1: Any, T2 : Any> Lazy<T1>.map(mapper: (T1) -> T2): Lazy<T2> {
  return object : Lazy<T2> {
    override val value: T2 get() {
      return mapper(this@map.value)
    }

    override fun isInitialized(): Boolean {
      return this@map.isInitialized()
    }
  }
}
