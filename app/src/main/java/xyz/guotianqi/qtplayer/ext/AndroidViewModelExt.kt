package xyz.guotianqi.qtplayer.ext

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.content.res.Resources

val AndroidViewModel.resources: Resources
    get() = getApplication<Application>().resources