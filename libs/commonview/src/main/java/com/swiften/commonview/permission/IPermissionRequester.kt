package com.swiften.commonview.permission

interface IPermissionRequester {
  fun requestPermissionIfNeeded(
    permission: String,
    eventHooks: IPermissionRequestEventHooks,
  )
}
