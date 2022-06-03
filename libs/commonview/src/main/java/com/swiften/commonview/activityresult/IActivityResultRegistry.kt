package com.swiften.commonview.activityresult

/** This handler provides some contracts for useful Fragment methods relating to activity result */
interface IActivityResultRegistry {
  fun <I, O> registerForActivityResult(): IActivityResultLauncher<I, O>
}
