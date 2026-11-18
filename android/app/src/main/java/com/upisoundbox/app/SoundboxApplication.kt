package com.upisoundbox.app

import android.app.Application
import com.upisoundbox.app.data.repository.SoundboxRepository

class SoundboxApplication : Application() {

    lateinit var repository: SoundboxRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = SoundboxRepository.getInstance(this)
    }
}

