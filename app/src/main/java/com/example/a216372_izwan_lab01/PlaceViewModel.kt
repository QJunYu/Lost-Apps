package com.example.a216372_izwan_lab01

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a216372_izwan_lab01.data.CommunityPlace
import com.example.a216372_izwan_lab01.data.CommunityRepository
import com.example.a216372_izwan_lab01.data.SavedPlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaceViewModel(
    private val savedPlaceRepository: SavedPlaceRepository
) : ViewModel() {

    private val communityRepository = CommunityRepository()

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

    /** Shared community popularity feed (Firestore), most-hearted first. */
    val popularPlaces: StateFlow<List<CommunityPlace>> = communityRepository.observePopularPlaces()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** placeId -> shared heart count, for showing heart badges anywhere in the app. */
    val communityHeartCounts: StateFlow<Map<String, Long>> = popularPlaces
        .map { list -> list.associate { it.placeId to it.heartCount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    /** placeIds this device has already favourited (guards against double-hearting). */
    val heartedPlaceIds: StateFlow<Set<String>> = savedPlaces
        .map { list -> list.mapNotNull { it.placeId.ifBlank { null } }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    var savedPlaceBeingEdited by mutableStateOf<SavedPlace?>(null)
    var reopenSearchAfterDetailClose by mutableStateOf(false)

    fun setPlace(place: PlaceUI?) {
        selectedPlace = place
    }

    fun addSavedPlace(item: SavedPlace) {
        val isNewHeart = item.placeId.isNotBlank() &&
            savedPlaces.value.none { it.placeId == item.placeId }
        viewModelScope.launch {
            savedPlaceRepository.insert(item)
        }
        if (isNewHeart) {
            communityRepository.addHeart(
                CommunityPlace(
                    placeId = item.placeId,
                    name = item.name,
                    address = item.address,
                    lat = item.latitude,
                    lng = item.longitude,
                    rating = item.rating,
                    reviewsCount = item.reviewsCount,
                )
            )
        }
    }

    fun updateSavedPlace(item: SavedPlace) {
        viewModelScope.launch {
            savedPlaceRepository.update(item)
        }
    }

    /** Removes a favourite locally (Room) and takes back its community heart. */
    fun deleteSavedPlace(item: SavedPlace) {
        viewModelScope.launch {
            savedPlaceRepository.delete(item)
        }
        if (item.placeId.isNotBlank()) {
            communityRepository.removeHeart(item.placeId)
        }
    }

    /** One-tap "heart" from the detail page: saves locally (Room) and bumps the shared count. */
    fun heartPlaceFromDetail(place: PlaceUI) {
        if (place.placeId.isBlank()) return
        if (heartedPlaceIds.value.contains(place.placeId)) return
        addSavedPlace(
            SavedPlace(
                id = System.currentTimeMillis(),
                placeId = place.placeId,
                name = place.name,
                address = place.address,
                category = "Favourite",
                note = "Added from place detail",
                createdAt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date()),
                latitude = place.latLng.latitude,
                longitude = place.latLng.longitude,
                rating = place.rating,
                reviewsCount = place.reviewsCount,
            )
        )
    }
}
