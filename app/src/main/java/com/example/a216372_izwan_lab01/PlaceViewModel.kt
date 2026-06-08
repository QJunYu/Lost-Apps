package com.example.a216372_izwan_lab01

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a216372_izwan_lab01.data.SavedPlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaceViewModel(
    private val savedPlaceRepository: SavedPlaceRepository
) : ViewModel() {

    var selectedPlace by mutableStateOf<PlaceUI?>(null)
    var isSearchOpen by mutableStateOf(false)
    var searchText by mutableStateOf("")
    var placesList by mutableStateOf(listOf<PlaceUI>())

    val savedPlaces: StateFlow<List<SavedPlace>> = savedPlaceRepository.observeSavedPlaces()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    var savedPlaceBeingEdited by mutableStateOf<SavedPlace?>(null)
    var reopenSearchAfterDetailClose by mutableStateOf(false)

    fun setPlace(place: PlaceUI?) {
        selectedPlace = place
    }

    fun addSavedPlace(item: SavedPlace) {
        viewModelScope.launch {
            savedPlaceRepository.insert(item)
        }
    }

    fun updateSavedPlace(item: SavedPlace) {
        viewModelScope.launch {
            savedPlaceRepository.update(item)
        }
    }
}
