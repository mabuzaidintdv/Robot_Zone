package com.intdv.robotzone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aldebaran.qi.sdk.`object`.human.Human

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _btStatus = MutableLiveData<String>()
    val btStatus: LiveData<String>
        get() = _btStatus

    private val _locationsLiveData: MutableLiveData<List<Any>> = MutableLiveData(emptyList())
    val locationsLiveData: LiveData<List<Any>> = _locationsLiveData

    var selectedHuman: Human? = null

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        const val TAG: String = "MainViewModel"
    }
}