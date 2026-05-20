package com.federico.moneytrack.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnboardingSlide(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int
)
