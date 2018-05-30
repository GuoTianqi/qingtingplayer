package xyz.guotianqi.qtplayer.ext

import android.app.Activity
import android.widget.Toast

fun Activity.toast(text: CharSequence) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}