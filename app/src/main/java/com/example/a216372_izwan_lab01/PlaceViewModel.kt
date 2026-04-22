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

    fun setPlace(place: PlaceUI?) {
        selectedPlace = place
    }
}