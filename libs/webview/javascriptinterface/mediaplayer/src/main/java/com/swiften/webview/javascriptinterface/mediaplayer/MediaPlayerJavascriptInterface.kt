package com.swiften.webview.javascriptinterface.mediaplayer

import android.R.attr.bitmap
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Base64
import android.util.Size
import android.webkit.JavascriptInterface
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.swiften.commonview.lifecycle.IGenericLifecycleOwner
import com.swiften.commonview.lifecycle.NoopGenericLifecycleOwner
import com.swiften.webview.BridgeMethodArgumentsParser
import com.swiften.webview.IBridgeRequestProcessor
import com.swiften.webview.IJavascriptInterface
import com.swiften.webview.parseArguments
import com.swiften.webview.processStream
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.File


class MediaPlayerJavascriptInterface(
  override val name: String,
  private val argsParser: Lazy<BridgeMethodArgumentsParser>,
  private val context: Lazy<Context>,
  private val playerView: Lazy<IExoPlayerView>,
  private val requestProcessor: Lazy<IBridgeRequestProcessor>,
) : IJavascriptInterface,
  IGenericLifecycleOwner by NoopGenericLifecycleOwner
{
  sealed interface MethodArguments {
    data class GenerateVideoThumbnail(
      val height: Int,
      val uri: String,
      val width: Int,
    ) : MethodArguments

    data class PlayMedia(val mimeType: String, val uri: String) : MethodArguments
  }

  sealed interface MethodResult {
    data class PlayMedia(val event: Event) : MethodResult {
      sealed interface Event {
        val type: String

        object PlayerOpened : Event {
          override val type = "PLAYER_OPENED"
        }

        data class IsPlayingChanged(val isPlaying: Boolean) : Event {
          override val type = "IS_PLAYING_CHANGED"
        }

        data class PlaybackError(val error: Exception) : Event {
          override val type = "PLAYBACK_ERROR"
        }

        object PlaybackFinished : Event {
          override val type = "PLAYBACK_FINISHED"
        }

        object PlayerClosed : Event {
          override val type = "PLAYER_CLOSED"
        }
      }
    }
  }

  @JavascriptInterface
  fun generateVideoThumbnail(rawRequest: String) {
    val request = this.argsParser.value.parseArguments<MethodArguments.GenerateVideoThumbnail>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = Maybe.create<String> { emitter ->
        /**
         * The URI might be url-encoded, and thus to get the actual file name, we need to url-decode
         * it.
         */
        val filePath = Uri.decode(request.parameters.uri).removePrefix("file://")
        val file = File(filePath)
        val cancellationSignal = CancellationSignal()
        val size = Size(request.parameters.width, request.parameters.height)

        emitter.setCancellable {
          cancellationSignal.cancel()
        }

        try {
          val base64EncodedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ThumbnailUtils.createVideoThumbnail(file, size, cancellationSignal)
          } else {
            ThumbnailUtils.createVideoThumbnail(
              file.absolutePath,
              MediaStore.Images.Thumbnails.MINI_KIND,
            )
          }?.let { bitmap ->
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT);
          }

          if (base64EncodedBitmap != null) {
            emitter.onSuccess(base64EncodedBitmap)
          } else {
            emitter.onComplete()
          }
        } catch (error: Exception) {
          emitter.onError(error)
        }
      }.subscribeOn(
        Schedulers.io()
      ),
      bridgeArguments = request,
    )
  }

  @JavascriptInterface
  fun playMedia(rawRequest: String) {
    val request = this.argsParser.value.parseArguments<MethodArguments.PlayMedia>(rawRequest)

    this.requestProcessor.value.processStream(
      stream = Flowable.create<MethodResult.PlayMedia>({ emitter ->
        try {
          val player = ExoPlayer.Builder(this@MediaPlayerJavascriptInterface.context.value).build()
          this@MediaPlayerJavascriptInterface.playerView.value.setPlayer(player)

          val playerViewEventHooks = object : IExoPlayerViewEventHooks by NoopExoPlayerViewEventHooks {
            override fun onExoPlayerViewShown() {
              emitter.onNext(MethodResult.PlayMedia(
                event = MethodResult.PlayMedia.Event.PlayerOpened,
              ))
            }

            override fun onExoPlayerViewHidden() {
              emitter.onNext(MethodResult.PlayMedia(
                event = MethodResult.PlayMedia.Event.PlayerClosed,
              ))

              emitter.onComplete()
            }
          }

          val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
              super.onIsPlayingChanged(isPlaying)

              emitter.onNext(MethodResult.PlayMedia(
                event = MethodResult.PlayMedia.Event.IsPlayingChanged(isPlaying),
              ))
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
              super.onPlaybackStateChanged(playbackState)

              when (playbackState) {
                Player.STATE_IDLE -> {}
                Player.STATE_READY -> {}
                Player.STATE_BUFFERING -> {}
                Player.STATE_ENDED -> {
                  emitter.onNext(MethodResult.PlayMedia(
                    event = MethodResult.PlayMedia.Event.PlaybackFinished,
                  ))
                }
              }
            }

            override fun onPlayerError(error: PlaybackException) {
              super.onPlayerError(error)

              emitter.onNext(MethodResult.PlayMedia(
                event = MethodResult.PlayMedia.Event.PlaybackError(error),
              ))
            }
          }

          val mediaItem = MediaItem.Builder()
            .setUri(request.parameters.uri)
            .setMimeType(request.parameters.mimeType)
            .build()

          player.addListener(playerListener)
          player.setMediaItem(mediaItem)
          player.prepare()
          this@MediaPlayerJavascriptInterface.playerView.value.addEventHooks(playerViewEventHooks)
          this@MediaPlayerJavascriptInterface.playerView.value.show()
          player.play()

          emitter.setCancellable {
            /**
             * In this case, [IExoPlayerView.hide] could be called twice if the current [Flowable]
             * completes due to an outside [IExoPlayerView.hide] call, since we are calling
             * [io.reactivex.Emitter.onComplete] from
             * [IExoPlayerViewEventHooks.onExoPlayerViewHidden]. The timeline could look as follows:
             * - A [IExoPlayerView.hide] call is triggered from outside (e.g. via the user clicking
             *   the back button).
             * - [IExoPlayerViewEventHooks.onExoPlayerViewHidden] is triggered.
             * - This [io.reactivex.functions.Cancellable] function is triggered.
             * - Another [IExoPlayerView.hide] call is triggered, but since it is idempotent (based
             *   on the [IExoPlayerView] contract), it should not cause any issue.
             */
            this@MediaPlayerJavascriptInterface.playerView.value.hide()
            this@MediaPlayerJavascriptInterface.playerView.value.removeEventHooks(playerViewEventHooks)
            this@MediaPlayerJavascriptInterface.playerView.value.setPlayer(null)
            player.removeListener(playerListener)
            player.release()
          }
        } catch (error: Exception) {
          emitter.onError(error)
        }
      }, BackpressureStrategy.BUFFER
      ).subscribeOn(
        AndroidSchedulers.mainThread()
      ),
      bridgeArguments = request,
    )
  }
}
