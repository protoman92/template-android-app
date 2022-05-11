package com.swiften.commonview

import android.animation.Animator

object NoopAnimatorListener : Animator.AnimatorListener {
  override fun onAnimationStart(anim: Animator?) {}

  override fun onAnimationEnd(anim: Animator?) {}

  override fun onAnimationCancel(anim: Animator?) {}

  override fun onAnimationRepeat(anim: Animator?) {}
}
