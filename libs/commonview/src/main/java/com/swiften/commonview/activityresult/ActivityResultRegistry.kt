package com.swiften.commonview.activityresult

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import com.swiften.commonview.IGenericLifecycleOwner
import com.swiften.commonview.NoopGenericLifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

/**
 * This is similar to [Fragment.registerForActivityResult], but instead of centralizing the activity
 * result callback, we allow each [IActivityResultLauncher.launch] call to accept a set of event.
 * hooks. This allows us to preserve important context for the caller.
 */
class ActivityResultRegistry(private val fragment: Fragment) :
  IActivityResultRegistry,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()

  //region IActivityResultRegistry
  override fun <I, O> registerForActivityResult(): IActivityResultLauncher<I, O> {
    var eventHookRef: IActivityResultEventHook<I, O>? = null
    val semaphore = Semaphore(1)

    val launcher = this@ActivityResultRegistry.fragment
      .registerForActivityResult(object : ActivityResultContract<I, O>() {
        override fun createIntent(context: Context, input: I): Intent {
          return requireNotNull(eventHookRef)
            .createIntent(context = context, input = input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): O {
          return requireNotNull(eventHookRef)
            .parseResult(resultCode = resultCode, intent = intent)
        }
      }) { result ->
        requireNotNull(eventHookRef).onActivityResult(output = result)
        semaphore.release()
      }

    return object : IActivityResultLauncher<I, O> {
      override fun launch(input: I, eventHook: IActivityResultEventHook<I, O>) {
        this@ActivityResultRegistry.executor.submit {
          /**
           * Ensure launch calls are always sequential. In practice, this is likely not an issue
           * since starting activity for result is usually blocking (e.g. user cannot do anything
           * else until they have completed the activity result flow).
           */
          semaphore.acquire()
          eventHookRef = eventHook
          launcher.launch(input)
        }
      }

      override fun unregister() {
        launcher.unregister()
      }
    }
  }
  //endregion

  //region IGenericLifecycleOwner
  override fun deinitialize() {
    this.executor.shutdown()
  }
  //endregion
}
