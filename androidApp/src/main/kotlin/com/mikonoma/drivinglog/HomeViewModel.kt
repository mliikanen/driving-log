package com.mikonoma.drivinglog

import androidx.lifecycle.ViewModel
import com.mikonoma.drivinglog.home.HomeProcessor

/** Keeps the [HomeProcessor] alive across configuration changes and closes it when the screen is finished. */
class HomeViewModel(val processor: HomeProcessor) : ViewModel() {
    override fun onCleared() {
        processor.close()
    }
}
