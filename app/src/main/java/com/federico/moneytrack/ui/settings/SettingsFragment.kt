package com.federico.moneytrack.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.FragmentSettingsBinding
import com.federico.moneytrack.util.ThemeManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.openOutputStream(it)?.let { stream ->
                viewModel.exportData(stream)
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.openInputStream(it)?.let { stream ->
                viewModel.importData(stream)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Tema
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
        }

        // Exportar / Importar
        binding.btnExport.setOnClickListener {
            exportLauncher.launch("moneytrack_backup.csv")
        }

        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("text/*"))
        }

        // Observar estado
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SettingsUiState.Exporting -> {
                            binding.btnExport.isEnabled = false
                            binding.btnImport.isEnabled = false
                        }
                        is SettingsUiState.Importing -> {
                            binding.btnExport.isEnabled = false
                            binding.btnImport.isEnabled = false
                        }
                        is SettingsUiState.Success -> {
                            binding.btnExport.isEnabled = true
                            binding.btnImport.isEnabled = true
                            val msg = when (state.type) {
                                SuccessType.EXPORT -> getString(R.string.settings_export_success)
                                SuccessType.IMPORT -> getString(R.string.settings_import_success, state.recordCount)
                            }
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                            viewModel.clearState()
                        }
                        is SettingsUiState.Error -> {
                            binding.btnExport.isEnabled = true
                            binding.btnImport.isEnabled = true
                            val msg = when (state.type) {
                                ErrorType.EXPORT -> getString(R.string.settings_export_error, state.message)
                                ErrorType.IMPORT -> getString(R.string.settings_import_error, state.message)
                            }
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                            viewModel.clearState()
                        }
                        is SettingsUiState.Idle -> {
                            binding.btnExport.isEnabled = true
                            binding.btnImport.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
