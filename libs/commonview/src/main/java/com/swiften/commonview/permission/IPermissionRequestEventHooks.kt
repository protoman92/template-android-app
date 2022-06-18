package com.swiften.commonview.permission

interface IPermissionRequestEventHooks {
  fun onPermissionRequestResult(result: PermissionRequestResult)
}
