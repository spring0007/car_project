import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

class AnimationHandler {
    companion object {
        private const val ANIM_DURATION = 0L
    }

    fun animateShow(view: View) {
        view.apply {
            alpha = 0f
            translationY = 100f

            animate().alpha(1f)
                .translationY(0f)
                .setDuration(ANIM_DURATION)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun animateHide(view: View, onComplete: () -> Unit) {
        view.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(ANIM_DURATION)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction(onComplete)
            .start()
    }

}