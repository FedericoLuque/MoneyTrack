package com.federico.moneytrack.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federico.moneytrack.domain.model.Category
import com.federico.moneytrack.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow

    fun addCategory(name: String, type: String, colorHex: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _eventFlow.emit(UiEvent.Error("El nombre no puede estar vacío"))
                return@launch
            }

            // Asignamos un icono por defecto
            val category = Category(
                name = name,
                iconName = "ic_default",
                colorHex = colorHex,
                transactionType = type
            )
            categoryRepository.insertCategory(category)
            _eventFlow.emit(UiEvent.SaveSuccess)
        }
    }

    sealed class UiEvent {
        object SaveSuccess : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}
