package com.swiften.commonview.activity

/** This handler provides some contracts for useful Fragment methods relating to activity result */
interface IActivityResultRegistry {
  fun <I, O> registerForActivityResult(): IActivityResultLauncher<I, O>
}
