package com.intdv.robotzone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aldebaran.qi.sdk.`object`.human.Human

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var selectedHuman: Human? = null

    companion object {
        const val TAG: String = "MainViewModel"
    }
}