package com.swiften.commonview.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor

class LifecycleStreamObserver(private val lifecycle: Lifecycle) : DefaultLifecycleObserver,
  IGenericLifecycleOwner,
  ILifecycleStreamObserver
{
  private val lifecycleProcessor = BehaviorProcessor.create<Lifecycle.Event>()

  constructor(lifecycleOwner: LifecycleOwner) : this(lifecycleOwner.lifecycle)

  //region IGenericLifecycleOwner
  override fun initialize() {
    this.lifecycle.addObserver(this)
  }

  override fun deinitialize() {
    this.lifecycle.removeObserver(this)
  }
  //endregion

  //region ILifecycleStreamObserver
  override fun observeLifecycleEvents(): Flowable<Lifecycle.Event> {
    return this.lifecycleProcessor
  }
  //endregion

  //region DefaultLifecycleObserver
  override fun onCreate(owner: LifecycleOwner) {
    super.onCreate(owner)
    this.lifecycleProcessor.onNext(Lifecycle.Event.ON_CREATE)
  }

  override fun onStart(owner: LifecycleOwner) {
    super.onStart(owner)
    this.lifecycleProcessor.onNext(Lifecycle.Event.ON_START)
  }

  override fun onResume(owner: LifecycleOwner) {
    super.onResume(owner)
    this.lifecycleProcessor.onNext(Lifecycle.Event.ON_RESUME)
  }

  override fun onPause(owner: LifecycleOwner) {
    super.onPause(owner)
    this.lifecycleProcessor.onNext(Lifecycle.Event.ON_PAUSE)
  }

  override fun onStop(owner: LifecycleOwner) {
    super.onStop(owner)
    this.lifecycleProcessor.onNext(Lifecycle.Event.ON_STOP)
  }

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    this.lifecycleProcessor.onNext(Lifecycle.Event.ON_DESTROY)
  }
  //endregion
}