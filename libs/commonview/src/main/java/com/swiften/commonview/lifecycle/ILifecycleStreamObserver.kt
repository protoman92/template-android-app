package com.swiften.commonview.lifecycle

import androidx.lifecycle.Lifecycle
import io.reactivex.Flowable

interface ILifecycleStreamObserver {
  fun observeLifecycleEvents(): Flowable<Lifecycle.Event>
}
