package com.federico.moneytrack

import android.app.Application
import com.federico.moneytrack.util.ThemeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoneyTrackApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
    }
}
