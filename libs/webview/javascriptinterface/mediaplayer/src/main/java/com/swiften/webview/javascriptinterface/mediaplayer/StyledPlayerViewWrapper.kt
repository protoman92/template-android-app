package com.swiften.webview.javascriptinterface.mediaplayer

import android.animation.Animator
import android.view.View
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.swiften.commonview.animation.NoopAnimatorListener
import com.swiften.webview.ShimmerWebView
import java.util.concurrent.atomic.AtomicBoolean

class StyledPlayerViewWrapper(
  private val playerView: StyledPlayerView,
) : IExoPlayerView {
  companion object {
    internal const val DURATION_ANIMATION_MS = 200L
  }

  private val eventHooks = arrayListOf<IExoPlayerViewEventHooks>()

  /**
   * Since showing/hiding may involve some animations, we can use [isVisible] to check whether the
   * [playerView] has been marked for state changes, so that the [isShown] method can remain
   * synchronous.
   */
  private val isVisible = AtomicBoolean(true)

  //region IExoPlayerView
  override fun isShown(): Boolean {
    return this.isVisible.get()
  }

  override fun show() {
    if (this.isVisible.getAndSet(true)) {
      return
    }

    this.playerView
      .animate()
      .alpha(1f)
      .setDuration(DURATION_ANIMATION_MS)
      .setListener(object : Animator.AnimatorListener by NoopAnimatorListener {
        override fun onAnimationStart(p0: Animator?) {
          this@StyledPlayerViewWrapper.playerView.visibility = View.VISIBLE
        }

        override fun onAnimationEnd(p0: Animator?) {
          p0?.removeListener(this)

          for (eventHooks in this@StyledPlayerViewWrapper.eventHooks) {
            eventHooks.onExoPlayerViewShown()
          }
        }
      })
      .start()
  }

  override fun hide() {
    if (!this.isVisible.getAndSet(false)) {
      return
    }

    this.playerView
      .animate()
      .alpha(0f)
      .setDuration(DURATION_ANIMATION_MS)
      .setListener(object : Animator.AnimatorListener by NoopAnimatorListener {
        override fun onAnimationEnd(p0: Animator?) {
          p0?.removeListener(this)
          this@StyledPlayerViewWrapper.playerView.visibility = View.GONE

          for (eventHooks in this@StyledPlayerViewWrapper.eventHooks) {
            eventHooks.onExoPlayerViewHidden()
          }
        }
      })
      .start()
  }

  override fun setPlayer(player: Player?) {
    this.playerView.player = player
  }

  override fun addEventHooks(eventHooks: IExoPlayerViewEventHooks) {
    this.eventHooks.add(eventHooks)
  }

  override fun removeEventHooks(eventHooks: IExoPlayerViewEventHooks) {
    this.eventHooks.remove(eventHooks)
  }
  //endregion
}
