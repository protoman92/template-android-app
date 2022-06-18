package com.swiften.commonview.permission

sealed class PermissionRequestResult {
  object Granted : PermissionRequestResult()

  object Denied : PermissionRequestResult()
}
