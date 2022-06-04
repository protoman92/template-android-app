package com.swiften.commonview.activityresult

import android.content.Context
import android.content.Intent

interface IActivityResultEventHook<I, O> {
  fun createIntent(context: Context, input: I): Intent

  fun parseResult(resultCode: Int, intent: Intent?): O

  fun onActivityResult(output: O)
}
