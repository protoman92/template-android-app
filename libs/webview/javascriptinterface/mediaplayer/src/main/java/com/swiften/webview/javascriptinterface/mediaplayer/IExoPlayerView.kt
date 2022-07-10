package com.swiften.webview.javascriptinterface.mediaplayer

import com.google.android.exoplayer2.Player

interface IExoPlayerView {
  fun isShown(): Boolean

  /** This method should be idempotent */
  fun show()

  /** This method should be idempotent */
  fun hide()

  fun setPlayer(player: Player?)

  fun addEventHooks(eventHooks: IExoPlayerViewEventHooks)

  fun removeEventHooks(eventHooks: IExoPlayerViewEventHooks)
}
