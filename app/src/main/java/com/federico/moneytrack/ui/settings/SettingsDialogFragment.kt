package com.federico.moneytrack.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.DialogSettingsBinding
import com.federico.moneytrack.util.ThemeManager

class SettingsDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSettingsBinding.inflate(LayoutInflater.from(requireContext()))

        val currentTheme = ThemeManager.getThemePreference(requireContext())
        if (currentTheme == ThemeManager.THEME_DARK) {
            binding.radioDark.isChecked = true
        } else {
            binding.radioLight.isChecked = true
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radioDark -> ThemeManager.THEME_DARK
                else -> ThemeManager.THEME_LIGHT
            }
            ThemeManager.saveThemePreference(requireContext(), theme)
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_title)
            .setView(binding.root)
            .create()
    }
}
