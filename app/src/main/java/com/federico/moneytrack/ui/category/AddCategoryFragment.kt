package com.federico.moneytrack.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.FragmentAddCategoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddCategoryFragment : Fragment() {

    private var _binding: FragmentAddCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by viewModels()
    private var selectedColor = "#F44336" // Default Red

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeSpinner()
        setupColorSelection()

        binding.btnSaveCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString()
            val typeText = binding.actvCategoryType.text.toString()
            val type = if (typeText == getString(R.string.type_income)) "INCOME" else "EXPENSE"

            viewModel.addCategory(name, type, selectedColor)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    when (event) {
                        is CategoriesViewModel.UiEvent.SaveSuccess -> {
                            Toast.makeText(requireContext(), getString(R.string.msg_category_saved), Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        is CategoriesViewModel.UiEvent.Error -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupTypeSpinner() {
        val types = listOf(getString(R.string.type_expense), getString(R.string.type_income))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.actvCategoryType.setAdapter(adapter)
        binding.actvCategoryType.setText(types[0], false)
    }

    private fun setupColorSelection() {
        val colorViews = listOf(
            binding.colorRed to "#F44336",
            binding.colorGreen to "#4CAF50",
            binding.colorBlue to "#2196F3",
            binding.colorOrange to "#FF9800",
            binding.colorPurple to "#9C27B0",
            binding.colorTeal to "#009688"
        )

        colorViews.forEach { (view, color) ->
            view.setOnClickListener {
                selectedColor = color
                // Reset alpha for all
                colorViews.forEach { it.first.alpha = 0.3f }
                // Highlight selected
                view.alpha = 1.0f
            }
        }
        
        // Init state
        colorViews.forEach { it.first.alpha = 0.3f }
        binding.colorRed.alpha = 1.0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
