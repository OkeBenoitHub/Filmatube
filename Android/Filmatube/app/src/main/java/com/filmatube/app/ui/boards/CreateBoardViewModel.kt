package com.filmatube.app.ui.boards

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.boards.BoardRepository
import com.filmatube.app.data.boards.BoardTypes
import com.filmatube.app.data.upload.AvatarUploader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateBoardUiState(
    val title: String = "",
    val description: String = "",
    val type: String = BoardTypes.GENERAL,
    val isPublic: Boolean = true,
    val coverUri: Uri? = null,
    val isSaving: Boolean = false,
    val createdId: String? = null,
    val error: Boolean = false,
) {
    val canCreate: Boolean get() = title.isNotBlank() && !isSaving
}

@HiltViewModel
class CreateBoardViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val uploader: AvatarUploader,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateBoardUiState())
    val state = _state.asStateFlow()

    fun setTitle(v: String) = _state.update { it.copy(title = v, error = false) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setType(v: String) = _state.update { it.copy(type = v) }
    fun setPublic(v: Boolean) = _state.update { it.copy(isPublic = v) }
    fun setCover(uri: Uri?) = _state.update { it.copy(coverUri = uri) }

    fun create() {
        val s = _state.value
        if (!s.canCreate) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = false) }
            val result = runCatching {
                val coverUrl = s.coverUri?.let { uploader.uploadBoardCover(it) } ?: ""
                boardRepository.createBoard(s.title, s.description, s.type, s.isPublic, coverUrl)
            }.getOrNull()

            if (result != null) {
                _state.update { it.copy(isSaving = false, createdId = result) }
            } else {
                _state.update { it.copy(isSaving = false, error = true) }
            }
        }
    }
}
