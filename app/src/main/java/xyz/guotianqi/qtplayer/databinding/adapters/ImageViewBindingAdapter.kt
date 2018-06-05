package xyz.guotianqi.qtplayer.databinding.adapters

import android.databinding.BindingAdapter
import android.graphics.drawable.Animatable
import android.widget.ImageView

object ImageViewBindingAdapter {
    @BindingAdapter("app:animating")
    @JvmStatic fun animating(imageView: ImageView, animating: Boolean) {
        val drawable = imageView.drawable
        if (drawable is Animatable) {
            if (animating) drawable.start() else drawable.stop()
        }
    }
}