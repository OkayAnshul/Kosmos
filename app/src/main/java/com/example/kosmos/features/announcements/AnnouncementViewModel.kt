package com.example.kosmos.features.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementViewModel @Inject constructor(
    private val repository: AnnouncementRepository
) : ViewModel() {

    private val _pending = MutableStateFlow<Announcement?>(null)
    val pending: StateFlow<Announcement?> = _pending

    fun checkAnnouncements(userId: String) {
        viewModelScope.launch {
            _pending.value = repository.fetchUnseenAnnouncement(userId)
        }
    }

    fun dismiss(announcementId: String, userId: String) {
        viewModelScope.launch {
            repository.markSeen(announcementId, userId)
            _pending.value = null
        }
    }
}
