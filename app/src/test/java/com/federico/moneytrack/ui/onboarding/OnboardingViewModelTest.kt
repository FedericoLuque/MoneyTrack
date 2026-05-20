package com.federico.moneytrack.ui.onboarding

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingViewModelTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        prefs = mockk()
        editor = mockk()
        every { prefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs
    }

    @Test
    fun `isCompleted returns false when flag is not set`() {
        every { prefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false) } returns false
        val vm = OnboardingViewModel(prefs)
        assertFalse(vm.isCompleted)
    }

    @Test
    fun `isCompleted returns true when flag is set`() {
        every { prefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false) } returns true
        val vm = OnboardingViewModel(prefs)
        assertTrue(vm.isCompleted)
    }

    @Test
    fun `markCompleted writes true to SharedPreferences`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val vm = OnboardingViewModel(prefs)
        vm.markCompleted()
        verify { editor.putBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, true) }
        verify { editor.apply() }
    }
}
