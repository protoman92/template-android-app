package com.swiften.commonview.utils

open class LazyProperty<T : Any>(initialValue: T? = null): IReadonlyLazyProperty<T> {
  private lateinit var _value: T

  init {
    if (initialValue != null) {
      this._value = initialValue
    }
  }

  //region Lazy
  override var value: T
    get() = this._value

    set(value) {
      this._value = value
    }

  @Suppress("SENSELESS_COMPARISON")
  override fun isInitialized(): Boolean {
    return this._value != null
  }
  //endregion
}
