package com.swiften.webview

import android.animation.Animator
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.constraintlayout.widget.ConstraintLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.swiften.commonview.animation.NoopAnimatorListener

class ShimmerWebView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle),
  IWebView,
  IWebViewEventHook by NoopWebViewEventHook {
  companion object {
    internal const val DURATION_ANIMATION_MS = 200L
  }

  private var loadingContainer: ShimmerFrameLayout
  private var webview: CustomWebView

  init {
    View.inflate(this.context, R.layout.shimmer_webview, this)
    this.loadingContainer = this.findViewById(R.id.loading_container)
    this.webview = this.findViewById(R.id.webview)
    this.loadingContainer.stopShimmer()
  }

  //region IGenericLifecycleOwner
  override fun initialize() {
    this.webview.initialize()
  }

  override fun deinitialize() {
    this.webview.deinitialize()
  }
  //endregion

  //region IWebView
  override var javascriptInterfaces: List<IJavascriptInterface>
    get() = this.webview.javascriptInterfaces
    set(value) {
      this.webview.javascriptInterfaces = value
    }

  override fun canGoBack(): Boolean {
    return this.webview.canGoBack()
  }

  override fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {
    this.webview.evaluateJavascript(script = script, resultCallback = resultCallback)
  }

  override fun goBack() {
    this.webview.goBack()
  }

  override fun loadUrl(url: String) {
    this.webview.loadUrl(url)
  }

  override fun reload() {
    this.webview.reload()
  }
  //endregion

  //region IWebViewEventHook
  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
    this.loadingContainer.startShimmer()
  }

  override fun onPageFinished(view: WebView?, url: String?) {
    view
      ?.animate()
      ?.alpha(1f)
      ?.setDuration(DURATION_ANIMATION_MS)
      ?.setListener(object : Animator.AnimatorListener by NoopAnimatorListener {
        override fun onAnimationStart(anim: Animator?) {
          view.visibility = View.VISIBLE
        }
      })
      ?.start()

    this.loadingContainer
      .animate()
      .alpha(0f)
      .setDuration(DURATION_ANIMATION_MS)
      .setListener(object : Animator.AnimatorListener by NoopAnimatorListener {
        override fun onAnimationEnd(anim: Animator?) {
          this@ShimmerWebView.loadingContainer.also {
            it.stopShimmer()
            it.visibility = View.GONE
          }
        }
      })
      .start()
  }
  //endregion

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    this.webview.addEventHook(hook = this)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    this.webview.removeEventHook(hook = this)
  }
}
