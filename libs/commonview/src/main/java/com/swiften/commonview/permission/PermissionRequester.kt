package com.swiften.commonview.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.swiften.commonview.activity.IActivityResultEventHook
import com.swiften.commonview.activity.IActivityResultLauncher

class PermissionRequester(
  private val activity: Activity,
  private val activityResultLauncher: IActivityResultLauncher<String, Boolean>,
) : IPermissionRequester {
  //region IPermissionRequester
  override fun requestPermissionIfNeeded(
    permission: String,
    eventHooks: IPermissionRequestEventHooks,
  ) {
    val permissionGranted = ContextCompat.checkSelfPermission(
      this.activity,
      permission,
    ) == PackageManager.PERMISSION_GRANTED

    if (permissionGranted) {
      eventHooks.onPermissionRequestResult(result = PermissionRequestResult.Granted)
      return
    }

    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        activity.shouldShowRequestPermissionRationale(permission) -> {
      }
      else -> {
        val requestPermissionContract = ActivityResultContracts.RequestPermission()

        this.activityResultLauncher.launch(permission, object : IActivityResultEventHook<String, Boolean> {
          override fun createIntent(context: Context, input: String): Intent {
            return requestPermissionContract.createIntent(context = context, input = input)
          }

          override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return requestPermissionContract.parseResult(resultCode = resultCode, intent = intent)
          }

          override fun onActivityResult(output: Boolean) {
            val result = when (output) {
              true -> PermissionRequestResult.Granted
              else -> PermissionRequestResult.Denied
            }

            eventHooks.onPermissionRequestResult(result = result)
          }
        })
      }
    }
  }
  //endregion
}
