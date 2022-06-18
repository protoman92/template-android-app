package com.swiften.commonview.activity

import androidx.activity.result.ActivityResultLauncher

/** Mimics the functionalities of an [ActivityResultLauncher] */
interface IActivityResultLauncher<I, O> {
  fun launch(input: I, eventHook: IActivityResultEventHook<I, O>)

  fun unregister()
}
