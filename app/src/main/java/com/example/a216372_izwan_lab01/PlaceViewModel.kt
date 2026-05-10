package com.example.a216372_izwan_lab01
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class PlaceViewModel : ViewModel() {

    var selectedPlace by mutableStateOf<PlaceUI?>(null)
    var isSearchOpen by mutableStateOf(false)
    var searchText by mutableStateOf("")
    var placesList by mutableStateOf(listOf<PlaceUI>())
    var savedPlaces by mutableStateOf(listOf<SavedPlace>())
    /** When set, [AddPlaceNoteScreen] opens in edit mode for this item. */
    var savedPlaceBeingEdited by mutableStateOf<SavedPlace?>(null)
    /** When true, closing [DetailScreen] should reopen the home search overlay (search flow only). */
    var reopenSearchAfterDetailClose by mutableStateOf(false)

    fun setPlace(place: PlaceUI?) {
        selectedPlace = place
    }

    fun addSavedPlace(item: SavedPlace) {
        savedPlaces = listOf(item) + savedPlaces
    }

    fun updateSavedPlace(item: SavedPlace) {
        savedPlaces = savedPlaces.map { if (it.id == item.id) item else it }
    }
}