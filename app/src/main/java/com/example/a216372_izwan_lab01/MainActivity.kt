package com.example.a216372_izwan_lab01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import com.example.a216372_izwan_lab01.ui.theme.A216372_IZWAN_Lab01Theme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.google.maps.android.compose.*
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import android.annotation.SuppressLint
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.*
import androidx.compose.foundation.lazy.*
import com.google.android.libraries.places.api.Places
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.*
import androidx.navigation.NavController
import android.graphics.Bitmap
import androidx.compose.ui.text.style.TextOverflow
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )

        enableEdgeToEdge()

        setContent {

            val navController = rememberNavController()
            val viewModel: PlaceViewModel = viewModel()

            A216372_IZWAN_Lab01Theme {

                NavHost(navController = navController, startDestination = "home") {

                    composable("home") {
                        HomeScreen(navController, viewModel)
                    }

                    composable("detail") {
                        DetailScreen(navController, viewModel)
                    }

                    composable("add_note") {
                        AddPlaceNoteScreen(navController, viewModel)
                    }

                    composable("saved") {
                        SavedPlacesScreen(navController, viewModel)
                    }

                    composable("insights") {
                        MobilityInsightsScreen(navController, viewModel)
                    }

                    composable("nearby_restaurants") {
                        NearbyRestaurantsScreen(navController, viewModel)
                    }
                }
            }
        }
    }
}

data class PlaceUI(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng,
    val rating: Double?,
    val distanceKm: Double,
    val reviewsCount: Int?
)

data class SavedPlace(
    val id: Long,
    val placeId: String,
    val name: String,
    val address: String,
    val category: String,
    val note: String,
    val createdAt: String,
    val latitude: Double?,
    val longitude: Double?,
    val rating: Double?,
    val reviewsCount: Int?
)

private fun SavedPlace.toPlaceUIOrNull(): PlaceUI? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return PlaceUI(
        placeId = placeId.ifBlank { "saved_${id}" },
        name = name,
        address = address,
        latLng = LatLng(lat, lng),
        rating = rating,
        distanceKm = 0.0,
        reviewsCount = reviewsCount
    )
}

/** Rich fields from a follow-up Place Details request (must match requested Place.Field mask). */
data class PlaceDetailFields(
    val phone: String?,
    val weekdayText: List<String>,
    val isOpenNow: Boolean?,
    val typeDisplay: String?,
    val userRatingCount: Int?
)

data class NearbyRestaurantUI(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng,
    val rating: Double?,
    val userRatingCount: Int?,
    val distanceKm: Double,
    val isOpenNow: Boolean?
)

private fun googleLocalDateToJava(d: com.google.android.libraries.places.api.model.LocalDate): LocalDate =
    LocalDate.of(d.year, d.month, d.day)

private fun googleLocalTimeToJava(t: com.google.android.libraries.places.api.model.LocalTime): LocalTime =
    LocalTime.of(t.hours, t.minutes)

/** Uses [Place.currentOpeningHours] (preferred) or regular opening hours + UTC offset. */
private fun computeIsOpenNow(place: Place): Boolean? {
    val offsetMin = place.utcOffsetMinutes ?: return null
    val zone = ZoneOffset.ofTotalSeconds(offsetMin * 60)
    val now = ZonedDateTime.now(zone)
    val hours = place.currentOpeningHours ?: place.openingHours ?: return null
    val periods = hours.periods ?: return null
    if (periods.isEmpty()) return null

    for (period in periods) {
        val open = period.`open` ?: continue
        val close = period.`close` ?: continue
        val od = open.date
        val cd = close.date
        if (od != null && cd != null) {
            val openZdt = ZonedDateTime.of(
                googleLocalDateToJava(od),
                googleLocalTimeToJava(open.time),
                zone
            )
            val closeZdt = ZonedDateTime.of(
                googleLocalDateToJava(cd),
                googleLocalTimeToJava(close.time),
                zone
            )
            if (!now.isBefore(openZdt) && now.isBefore(closeZdt)) return true
            continue
        }
        val openDow = JavaDayOfWeek.valueOf(open.day.name)
        if (now.dayOfWeek != openDow) continue
        val openMin = open.time.hours * 60 + open.time.minutes
        val closeMin = close.time.hours * 60 + close.time.minutes
        val nowMin = now.toLocalTime().toSecondOfDay() / 60
        if (closeMin >= openMin) {
            if (nowMin in openMin until closeMin) return true
        } else {
            if (nowMin >= openMin || nowMin < closeMin) return true
        }
    }
    return false
}

private fun typeLabelFromPlaceTypes(types: List<String>?): String? {
    if (types.isNullOrEmpty()) return null
    val generic = setOf(
        "establishment", "point_of_interest", "premise", "subpremise",
        "political", "geocode", "route", "street_address", "locality"
    )
    return types
        .firstOrNull { it.lowercase() !in generic }
        ?.replace('_', ' ')
}
fun calculateDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {

    val R = 6371
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(Math.toRadians(lat1)) *
            Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon/2) * Math.sin(dLon/2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
    return R * c
}
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: PlaceViewModel
) {
    var cardHeightPx by remember { mutableStateOf<Int>(0) }
    val density = LocalDensity.current
    val cardHeightDp = with(density) { cardHeightPx.toDp() }

    var isSearchOpen by viewModel::isSearchOpen
    var searchText by viewModel::searchText
    var placesList by viewModel::placesList
    var sheetExpanded by remember { mutableStateOf(false) }
    val selectedPlace = viewModel.selectedPlace

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context, "AIzaSyBQFSEYxasR_AC2QiEl7BPmbKXAh167rX8")
        }
    }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                userLocation = LatLng(it.latitude, it.longitude)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🔹 MAP
        val bangi = LatLng(2.922, 101.780)

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(bangi, 14f)
        }

        val selectedPlace = viewModel.selectedPlace

        LaunchedEffect(selectedPlace) {
            selectedPlace?.let {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(it.latLng, 16f)
                )
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style_dark
                )
            )
        )

        // 🔹 NORMAL UI
        if (!isSearchOpen) {

            val screenHeight = LocalConfiguration.current.screenHeightDp.dp

            val sheetHeight by animateDpAsState(
                if (sheetExpanded) screenHeight * 0.8f else 380.dp,
                label = ""
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF2B2B2B))
                    .pointerInput(sheetExpanded) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (!sheetExpanded && dragAmount < -10) {
                                sheetExpanded = true
                            } else if (sheetExpanded && dragAmount > 10) {
                                sheetExpanded = false
                            }
                        }
                    }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .navigationBarsPadding()
                        .onGloballyPositioned {
                            cardHeightPx = it.size.height
                        },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(Color(0xFF2B2B2B))
                ) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .padding(bottom = 60.dp)
                    ){

                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(40.dp)
                                .height(5.dp)
                                .background(Color.Gray, RoundedCornerShape(50))
                                .clickable {
                                    sheetExpanded = !sheetExpanded
                                }
                        )

                        Spacer(Modifier.height(16.dp))

                        SearchBar { isSearchOpen = true }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FunctionItem(Icons.Default.Home, "Home") {
                                navController.navigate("home")
                            }
                            FunctionItem(Icons.Default.EditNote, "Add") {
                                viewModel.savedPlaceBeingEdited = null
                                navController.navigate("add_note")
                            }
                            FunctionItem(Icons.Default.Favorite, "Favs") {
                                navController.navigate("saved")
                            }
                            FunctionItem(Icons.Default.Insights, "Stats") {
                                navController.navigate("insights")
                            }
                            FunctionItem(Icons.Default.Restaurant, "Food") {
                                navController.navigate("nearby_restaurants")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        CommuteCard()

                        if (sheetExpanded) {
                            Spacer(Modifier.height(16.dp))
                            ExpandedContent(
                                onRestaurantsClick = {
                                    navController.navigate("nearby_restaurants")
                                }
                            )
                        }
                    }
                }
            }

            if (!sheetExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                        .padding(bottom = sheetHeight + 20.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LightMode, null, tint = Color(0xFFFFC107))
                            Spacer(Modifier.width(6.dp))
                            Text("31°C", color = Color.White)
                        }
                        Text("Sunny", color = Color.White)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FloatingButton(Icons.Default.Warning) { }
                        FloatingButton(Icons.Default.LocationOn) {
                            userLocation?.let {
                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(it, 16f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF1F1F1F))
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(Icons.Default.Home, "Travel", true) {
                    navController.navigate("home")
                }
                BottomNavItem(Icons.AutoMirrored.Filled.List, "Saved", false) {
                    navController.navigate("saved")
                }
                BottomNavItem(Icons.Default.Person, "Insights", false) {
                    navController.navigate("insights")
                }
            }
        }

        if (isSearchOpen) {

            if (selectedPlace != null && !isSearchOpen) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(Color(0xFF2B2B2B))
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    selectedPlace.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.clickable {
                                        viewModel.setPlace(null)
                                    },
                                    tint = Color.White
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(selectedPlace.address, color = Color.Gray)

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "⭐ ${selectedPlace.rating ?: "N/A"}",
                                color = Color.Yellow
                            )

                            Spacer(Modifier.height(10.dp))

                            AsyncImage(
                                model = R.drawable.volks,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1F1F1F))
                    .imePadding()
            ) {

                Column(
                    Modifier
                        .padding(16.dp)
                        .statusBarsPadding()
                ) {

                    fun searchPlaces() {
                        val placesClient = Places.createClient(context)

                        val request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(searchText)
                            .build()

                        placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { response ->

                                val tempList = mutableListOf<PlaceUI>()

                                response.autocompletePredictions
                                    .take(5)   // 🔥 LIMIT TO 5 RESULTS
                                    .forEach { prediction ->

                                        val placeRequest = FetchPlaceRequest.builder(
                                            prediction.placeId,
                                            listOf(
                                                Place.Field.ID,
                                                Place.Field.LOCATION,
                                                Place.Field.RATING,
                                                Place.Field.FORMATTED_ADDRESS,
                                                Place.Field.PHOTO_METADATAS,
                                                Place.Field.USER_RATING_COUNT
                                            )
                                        ).build()

                                    placesClient.fetchPlace(placeRequest)
                                        .addOnSuccessListener { placeResponse ->

                                            val place = placeResponse.place
                                            val latLng = place.location ?: return@addOnSuccessListener

                                            val distance = userLocation?.let {
                                                calculateDistance(
                                                    it.latitude, it.longitude,
                                                    latLng.latitude, latLng.longitude
                                                )
                                            } ?: 0.0

                                            tempList.add(
                                                PlaceUI(
                                                    placeId = prediction.placeId,
                                                    name = prediction.getPrimaryText(null).toString(),
                                                    address = place.formattedAddress ?: "",
                                                    latLng = latLng,
                                                    rating = place.rating,
                                                    distanceKm = distance,
                                                    reviewsCount = place.userRatingCount
                                                )
                                            )

                                            placesList = tempList.sortedBy { it.distanceKm }
                                        }
                                }
                            }
                    }
                    // 🔹 TOP BAR
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),

                            keyboardActions = KeyboardActions {
                                searchPlaces()
                            },

                            trailingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.clickable {
                                        searchPlaces()
                                    }
                                )
                            }
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "Cancel",
                            color = Color.Blue,
                            modifier = Modifier.clickable {
                                isSearchOpen = false
                                searchText = ""
                                placesList = emptyList()
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    if (placesList.isEmpty()) {
                        SearchEmptyState(
                            searchText = searchText,
                            onSuggestionClick = { suggestion ->
                                searchText = suggestion
                                searchPlaces()
                            }
                        )
                    }

                    if (placesList.isNotEmpty()) {

                        LazyColumn {

                            items(placesList) { place ->

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable {
                                            viewModel.reopenSearchAfterDetailClose = true
                                            viewModel.setPlace(place)
                                            isSearchOpen = false
                                            navController.navigate("detail")
                                        }
                                ) {

                                    Column(Modifier.padding(12.dp)) {

                                        Text(place.name, color = Color.White)

                                        Text(place.address, color = Color.Gray)

                                        Spacer(Modifier.height(6.dp))

                                        Row {
                                            Text(
                                                "⭐ ${place.rating ?: "N/A"}",
                                                color = Color.Yellow
                                            )

                                            Spacer(Modifier.width(10.dp))

                                            Text(
                                                "${"%.2f".format(place.distanceKm)} km",
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF3A3A3A))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = Color.LightGray
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            "Hi, where to?",
            color = Color.LightGray,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchEmptyState(
    searchText: String,
    onSuggestionClick: (String) -> Unit
) {
    val suggestions = listOf("Restaurant nearby", "Cafe", "Petrol station", "Hospital")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.emptystate),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (searchText.isBlank()) "Where do you want to go?" else "No results found",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (searchText.isBlank()) {
                        "Try searching for places, attractions, or food."
                    } else {
                        "Try another keyword or tap a suggestion below."
                    },
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Suggestions",
            color = Color.LightGray,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = { Text(suggestion) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF333333),
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun FunctionItem(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D5BFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CommuteCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2B2B2B))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Commute", color = Color.White)
            Text("Navigate with a tap", color = Color.Gray)
        }

        AsyncImage(
            model = R.drawable.volks,
            contentDescription = null,
            modifier = Modifier.size(70.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit = {}) {

    val color = if (selected) Color(0xFF4D7CFF) else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {

        Icon(icon, contentDescription = label, tint = color)

        Text(label, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun FloatingButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
fun ExpandedContent(onRestaurantsClick: () -> Unit = {}) {

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // 🔹 LEFT BIG CARD
        RecommendationCard(
            title = "Exploring Durian",
            image = R.drawable.activity1,
            modifier = Modifier
                .weight(1f)
                .height(200.dp)
        )

        // 🔹 RIGHT COLUMN
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            RecommendationCard(
                "Hotel",
                R.drawable.activity2,
                modifier = Modifier.height(94.dp)
            )

            RecommendationCard(
                "Restaurants",
                R.drawable.activity3,
                modifier = Modifier.height(94.dp),
                onClick = onRestaurantsClick
            )
        }
    }
}

@Composable
fun RecommendationCard(
    title: String,
    image: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            Image(
                painter = painterResource(id = image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Text(
                title,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun NearbyRestaurantSection(
    restaurants: List<NearbyRestaurantUI>,
    isLoading: Boolean,
    photoMap: Map<String, Bitmap>,
    onRestaurantClick: (NearbyRestaurantUI) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 700.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Nearby Restaurants (Open)", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Showing up to 10 results", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                restaurants.isEmpty() -> {
                    Text("No open restaurants found nearby.", color = Color.Gray)
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(restaurants, key = { it.placeId }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.clickable { onRestaurantClick(item) }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    val bmp = photoMap[item.placeId]
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(170.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = R.drawable.activity3,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(170.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                    Text(item.address, color = Color(0xFFB0BEC5), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "⭐ ${item.rating?.let { "%.1f".format(it) } ?: "N/A"} (${item.userRatingCount ?: "N/A"}) • ${"%.1f".format(item.distanceKm)} km",
                                        color = Color(0xFFFFC107)
                                    )
                                    Text(
                                        when (item.isOpenNow) {
                                            true -> "Open now"
                                            false -> "Closed"
                                            null -> "Hours not available"
                                        },
                                        color = when (item.isOpenNow) {
                                            true -> Color(0xFF66BB6A)
                                            false -> Color(0xFFE57373)
                                            null -> Color.Gray
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun NearbyRestaurantsScreen(navController: NavController, viewModel: PlaceViewModel) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var restaurants by remember { mutableStateOf(listOf<NearbyRestaurantUI>()) }
    val photoMap = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                userLocation = LatLng(it.latitude, it.longitude)
            }
        }
    }

    LaunchedEffect(userLocation) {
        if (userLocation == null) return@LaunchedEffect
        val placesClient = Places.createClient(context)
        isLoading = true
        restaurants = emptyList()
        photoMap.clear()

        val center = userLocation ?: return@LaunchedEffect
        val delta = 0.15
        val sw = LatLng(center.latitude - delta, center.longitude - delta)
        val ne = LatLng(center.latitude + delta, center.longitude + delta)

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery("restaurant")
            .setLocationBias(com.google.android.libraries.places.api.model.RectangularBounds.newInstance(sw, ne))
            .setTypesFilter(listOf("restaurant"))
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val candidates = mutableMapOf<String, NearbyRestaurantUI>()
                response.autocompletePredictions.take(100).forEach { prediction ->
                    val detailFields = listOf(
                        Place.Field.ID,
                        Place.Field.LOCATION,
                        Place.Field.FORMATTED_ADDRESS,
                        Place.Field.RATING,
                        Place.Field.USER_RATING_COUNT,
                        Place.Field.TYPES,
                        Place.Field.OPENING_HOURS,
                        Place.Field.CURRENT_OPENING_HOURS,
                        Place.Field.UTC_OFFSET,
                        Place.Field.PHOTO_METADATAS
                    )
                    val placeRequest = FetchPlaceRequest.builder(prediction.placeId, detailFields).build()
                    placesClient.fetchPlace(placeRequest)
                        .addOnSuccessListener { placeResponse ->
                            val p = placeResponse.place
                            val latLng = p.location ?: return@addOnSuccessListener
                            val types = p.placeTypes ?: emptyList()
                            if ("restaurant" !in types) return@addOnSuccessListener
                            val openNow = computeIsOpenNow(p)
                            if (openNow != true) return@addOnSuccessListener

                            val distance = userLocation?.let {
                                calculateDistance(
                                    it.latitude, it.longitude,
                                    latLng.latitude, latLng.longitude
                                )
                            } ?: 0.0

                            candidates[prediction.placeId] =
                                NearbyRestaurantUI(
                                    placeId = prediction.placeId,
                                    name = prediction.getPrimaryText(null).toString(),
                                    address = p.formattedAddress ?: "",
                                    latLng = latLng,
                                    rating = p.rating,
                                    userRatingCount = p.userRatingCount,
                                    distanceKm = distance,
                                    isOpenNow = openNow
                                )

                            p.photoMetadatas?.firstOrNull()?.let { metadata ->
                                val photoReq = FetchPhotoRequest.builder(metadata)
                                    .setMaxWidth(900)
                                    .setMaxHeight(450)
                                    .build()
                                placesClient.fetchPhoto(photoReq)
                                    .addOnSuccessListener { photoResp ->
                                        photoMap[prediction.placeId] = photoResp.bitmap
                                    }
                            }

                            restaurants = candidates.values
                                .sortedBy { it.distanceKm }
                                .take(10)
                            if (restaurants.size >= 10) isLoading = false
                        }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121820))
            .statusBarsPadding()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nearby Restaurants", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }
        Spacer(Modifier.height(10.dp))
        NearbyRestaurantSection(
            restaurants = restaurants,
            isLoading = isLoading,
            photoMap = photoMap,
            onRestaurantClick = { item ->
                viewModel.reopenSearchAfterDetailClose = false
                viewModel.setPlace(
                    PlaceUI(
                        placeId = item.placeId,
                        name = item.name,
                        address = item.address,
                        latLng = item.latLng,
                        rating = item.rating,
                        distanceKm = item.distanceKm,
                        reviewsCount = item.userRatingCount
                    )
                )
                navController.navigate("detail")
            }
        )
    }
}

@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: PlaceViewModel
) {

    val place = viewModel.selectedPlace
    val context = LocalContext.current
    var sheetExpanded by remember { mutableStateOf(false) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val detailSheetHeight by animateDpAsState(
        if (sheetExpanded) screenHeight * 0.8f else 380.dp,
        label = ""
    )
    val detailScrollState = rememberScrollState()

    val placePhotos = remember { mutableStateListOf<Bitmap>() }
    var placeDetails by remember { mutableStateOf<PlaceDetailFields?>(null) }

    LaunchedEffect(place?.placeId, place?.name) {
        placePhotos.clear()
        placeDetails = null
        if (place == null) return@LaunchedEffect

        val placesClient = Places.createClient(context)

        fun applyPhotosFromPlace(p: Place) {
            val metadatas = p.photoMetadatas ?: emptyList()
            metadatas.take(3).forEach { metadata ->
                val photoRequest = FetchPhotoRequest.builder(metadata)
                    .setMaxWidth(1000)
                    .setMaxHeight(600)
                    .build()
                placesClient.fetchPhoto(photoRequest)
                    .addOnSuccessListener { fetchPhotoResponse ->
                        if (placePhotos.size < 3) {
                            placePhotos.add(fetchPhotoResponse.bitmap)
                        }
                    }
            }
        }

        if (place.placeId.isNotBlank()) {
            val detailFields = listOf(
                Place.Field.PHOTO_METADATAS,
                Place.Field.OPENING_HOURS,
                Place.Field.CURRENT_OPENING_HOURS,
                Place.Field.UTC_OFFSET,
                Place.Field.INTERNATIONAL_PHONE_NUMBER,
                Place.Field.USER_RATING_COUNT,
                Place.Field.TYPES,
                Place.Field.PRIMARY_TYPE_DISPLAY_NAME
            )
            val placeRequest = FetchPlaceRequest.builder(place.placeId, detailFields).build()
            placesClient.fetchPlace(placeRequest)
                .addOnSuccessListener { placeResponse ->
                    val p = placeResponse.place
                    val weekday = p.openingHours?.weekdayText?.filterNotNull().orEmpty()
                    val openNow = computeIsOpenNow(p)
                    val typeLabel = p.primaryTypeDisplayName?.ifBlank { null }
                        ?: typeLabelFromPlaceTypes(p.placeTypes)
                    placeDetails = PlaceDetailFields(
                        phone = p.internationalPhoneNumber,
                        weekdayText = weekday,
                        isOpenNow = openNow,
                        typeDisplay = typeLabel,
                        userRatingCount = p.userRatingCount
                    )
                    applyPhotosFromPlace(p)
                }
        } else {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(place.name)
                .build()
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val firstPrediction = response.autocompletePredictions.firstOrNull()
                        ?: return@addOnSuccessListener
                    val placeRequest = FetchPlaceRequest.builder(
                        firstPrediction.placeId,
                        listOf(Place.Field.PHOTO_METADATAS)
                    ).build()
                    placesClient.fetchPlace(placeRequest)
                        .addOnSuccessListener { placeResponse ->
                            applyPhotosFromPlace(placeResponse.place)
                        }
                }
        }
    }
    LaunchedEffect(sheetExpanded) {
        if (!sheetExpanded) {
            detailScrollState.scrollTo(0)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(place?.latLng ?: LatLng(2.922, 101.780), 16f)
    }

    LaunchedEffect(place?.latLng) {
        place?.latLng?.let {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 16f))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style_dark
                )
            )
        ) {
            place?.let {
                Marker(
                    state = MarkerState(position = it.latLng),
                    title = it.name,
                    snippet = it.address
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(detailSheetHeight)
                .background(Color(0xFF1F1F1F))
                .navigationBarsPadding()
                .pointerInput(sheetExpanded) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (!sheetExpanded && dragAmount < -8) {
                            sheetExpanded = true
                        } else if (sheetExpanded && dragAmount > 8) {
                            sheetExpanded = false
                        }
                    }
                }
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(Color(0xFF1F1F1F))
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(detailScrollState, enabled = sheetExpanded)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.Gray.copy(alpha = 0.6f))
                            .clickable { sheetExpanded = !sheetExpanded }
                            .pointerInput(sheetExpanded) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (!sheetExpanded && dragAmount < -6) {
                                        sheetExpanded = true
                                    } else if (sheetExpanded && dragAmount > 6) {
                                        sheetExpanded = false
                                    }
                                }
                            }
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = place?.name ?: "No place selected",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close detail",
                            tint = Color(0xFF5AA8FF),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    viewModel.setPlace(null)
                                    if (viewModel.reopenSearchAfterDetailClose) {
                                        viewModel.isSearchOpen = true
                                    }
                                    viewModel.reopenSearchAfterDetailClose = false
                                    navController.popBackStack()
                                }
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    val pd = placeDetails
                    val typeLabel =
                        pd?.typeDisplay?.takeIf { it.isNotBlank() } ?: "place"
                    val openStatusLabel = when {
                        pd == null -> "…"
                        pd.weekdayText.isEmpty() && pd.isOpenNow == null -> "N/A"
                        pd.isOpenNow == true -> "Open"
                        pd.isOpenNow == false -> "Closed"
                        pd.weekdayText.isNotEmpty() -> "Hours"
                        else -> "N/A"
                    }
                    val openStatusColor = when (openStatusLabel) {
                        "Open" -> Color(0xFF66BB6A)
                        "Closed" -> Color(0xFFE57373)
                        "Hours" -> Color(0xFFB0BEC5)
                        else -> Color.Gray
                    }
                    Text(
                        text = buildString {
                            append(openStatusLabel)
                            append(" • ")
                            append(place?.distanceKm?.let { "%.1f km".format(it) } ?: "N/A")
                            append(" • ")
                            append(typeLabel)
                        },
                        color = openStatusColor,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "⭐ ${place?.rating?.let { "%.1f".format(it) } ?: "N/A"}    (${
                            pd?.userRatingCount ?: place?.reviewsCount ?: "N/A"
                        })",
                        color = Color.White
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.8f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            if (placePhotos.isNotEmpty()) {
                                Image(
                                    bitmap = placePhotos[0].asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = R.drawable.activity1,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                if (placePhotos.size > 1) {
                                    Image(
                                        bitmap = placePhotos[1].asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    AsyncImage(
                                        model = R.drawable.activity2,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                if (placePhotos.size > 2) {
                                    Image(
                                        bitmap = placePhotos[2].asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    AsyncImage(
                                        model = R.drawable.activity3,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    if (sheetExpanded) {
                        Spacer(Modifier.height(16.dp))
                        DetailExpandedContent(
                            address = place?.address ?: "No address",
                            reviewsCount = placeDetails?.userRatingCount ?: place?.reviewsCount,
                            detail = placeDetails
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailExpandedContent(
    address: String,
    reviewsCount: Int?,
    detail: PlaceDetailFields?
) {
    var hoursExpanded by remember { mutableStateOf(false) }
    val hoursLines = detail?.weekdayText.orEmpty()
    val hoursSummary = when {
        detail == null -> "Loading hours…"
        hoursLines.isEmpty() -> "N/A"
        detail.isOpenNow == true -> "Open now"
        detail.isOpenNow == false -> "Closed now"
        hoursLines.isNotEmpty() -> "Hours (tap)"
        else -> "N/A"
    }
    val hoursSummaryColor = when {
        detail == null -> Color.Gray
        hoursLines.isEmpty() -> Color.Gray
        detail.isOpenNow == true -> Color(0xFF66BB6A)
        detail.isOpenNow == false -> Color(0xFFE57373)
        hoursLines.isNotEmpty() -> Color(0xFFB0BEC5)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(Color(0xFF2A2A2A))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hoursLines.isNotEmpty()) {
                        hoursExpanded = !hoursExpanded
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        hoursSummary,
                        color = hoursSummaryColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (hoursLines.isNotEmpty()) {
                    Icon(
                        imageVector = if (hoursExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (hoursExpanded) "Collapse hours" else "Expand hours",
                        tint = Color.Gray
                    )
                }
            }
            if (hoursExpanded && hoursLines.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                hoursLines.forEach { line ->
                    Text(line, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                }
            }
            if (!hoursExpanded && hoursLines.isEmpty() && detail != null) {
                Spacer(Modifier.height(6.dp))
                Text("N/A", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(address, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    detail?.phone ?: "N/A",
                    color = Color(0xFF5AA8FF),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Upload images", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Report issue", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Fix incorrect information. Reviews: ${reviewsCount ?: "N/A"}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Straighten, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Measure", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RecommendationCard(
            title = "Nearby Spots",
            image = R.drawable.activity1,
            modifier = Modifier
                .weight(1f)
                .height(180.dp)
        )

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RecommendationCard(
                "Food Picks",
                R.drawable.activity2,
                modifier = Modifier.height(84.dp)
            )

            RecommendationCard(
                "Things To Do",
                R.drawable.activity3,
                modifier = Modifier.height(84.dp)
            )
        }
    }
}

@Composable
fun HomeMenuScreen(navController: NavController, viewModel: PlaceViewModel) {
    val darkBg = Color(0xFF121820)
    val cardBg = Color(0xFF1F2A36)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "SDG 11: Smart City Mobility",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Find places efficiently, reduce unnecessary travel, and improve daily navigation planning.",
                color = Color(0xFFB0BEC5)
            )

            MenuCard("Start Map Navigation", "Search and view place details", Icons.Default.Map, cardBg) {
                navController.navigate("home")
            }
            MenuCard("Add Place Note", "Log useful observations from a location", Icons.Default.EditNote, cardBg) {
                viewModel.savedPlaceBeingEdited = null
                navController.navigate("add_note")
            }
            MenuCard("Favourite Places", "See your saved favourites", Icons.Default.Favorite, cardBg) {
                navController.navigate("saved")
            }
            MenuCard("Mobility Insights", "Simple processing from your saved data", Icons.Default.Insights, cardBg) {
                navController.navigate("insights")
            }
        }
    }
}

@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF5AA8FF))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = Color(0xFF90A4AE), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun AddPlaceNoteScreen(navController: NavController, viewModel: PlaceViewModel) {
    val context = LocalContext.current
    val editing = viewModel.savedPlaceBeingEdited
    val selected = viewModel.selectedPlace
    val formKey = Pair(editing?.id, selected?.placeId)
    val seedPicked = editing?.toPlaceUIOrNull() ?: selected

    var pickedPlace by remember(formKey) { mutableStateOf<PlaceUI?>(seedPicked) }
    var locationQuery by remember(formKey) {
        mutableStateOf(seedPicked?.name ?: editing?.name ?: selected?.name ?: "")
    }
    var locationCandidates by remember(formKey) { mutableStateOf(listOf<PlaceUI>()) }

    var name by remember(formKey) { mutableStateOf(editing?.name ?: selected?.name ?: "") }
    var address by remember(formKey) { mutableStateOf(editing?.address ?: selected?.address ?: "") }
    var category by remember(formKey) { mutableStateOf(editing?.category ?: "Accessibility") }
    var note by remember(formKey) { mutableStateOf(editing?.note ?: "") }

    val categories = listOf("Accessibility", "Traffic", "Public Transport", "Safety", "Eco Spot")

    BackHandler {
        viewModel.savedPlaceBeingEdited = null
        navController.popBackStack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121820))
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (editing != null) "Edit Favourite Place" else "Add Place Note",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall
        )
        Text("Form Input screen for your SDG 11 workflow.", color = Color(0xFFB0BEC5))

        fun searchLocations() {
            val query = locationQuery.trim()
            if (query.isBlank()) {
                locationCandidates = emptyList()
                return
            }
            val placesClient = Places.createClient(context)
            val req = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build()
            placesClient.findAutocompletePredictions(req)
                .addOnSuccessListener { response ->
                    val candidates = mutableListOf<PlaceUI>()
                    response.autocompletePredictions.take(6).forEach { prediction ->
                        val placeReq = FetchPlaceRequest.builder(
                            prediction.placeId,
                            listOf(
                                Place.Field.ID,
                                Place.Field.LOCATION,
                                Place.Field.FORMATTED_ADDRESS,
                                Place.Field.RATING,
                                Place.Field.USER_RATING_COUNT
                            )
                        ).build()
                        placesClient.fetchPlace(placeReq)
                            .addOnSuccessListener { placeResp ->
                                val p = placeResp.place
                                val latLng = p.location ?: return@addOnSuccessListener
                                candidates.add(
                                    PlaceUI(
                                        placeId = prediction.placeId,
                                        name = prediction.getPrimaryText(null).toString(),
                                        address = p.formattedAddress ?: "",
                                        latLng = latLng,
                                        rating = p.rating,
                                        distanceKm = 0.0,
                                        reviewsCount = p.userRatingCount
                                    )
                                )
                                locationCandidates = candidates.toList()
                            }
                    }
                }
        }

        OutlinedTextField(
            value = locationQuery,
            onValueChange = { locationQuery = it },
            label = { Text("Search Real Location") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.clickable { searchLocations() }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (pickedPlace != null) {
            Text(
                "Selected location: ${pickedPlace!!.name}",
                color = Color(0xFF66BB6A),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (locationCandidates.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locationCandidates) { candidate ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2A36)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pickedPlace = candidate
                                name = candidate.name
                                address = candidate.address
                                locationQuery = candidate.name
                                locationCandidates = emptyList()
                            }
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(candidate.name, color = Color.White)
                            Text(candidate.address, color = Color(0xFFB0BEC5), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Place Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            supportingText = { Text("Example: ${categories.joinToString(", ")}") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Observation Note") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (name.isBlank()) return@Button
                val editingNow = viewModel.savedPlaceBeingEdited
                if (editingNow != null) {
                    val source = pickedPlace ?: editingNow.toPlaceUIOrNull()
                    viewModel.updateSavedPlace(
                        SavedPlace(
                            id = editingNow.id,
                            placeId = source?.placeId ?: editingNow.placeId,
                            name = name,
                            address = address.ifBlank { "N/A" },
                            category = category.ifBlank { "General" },
                            note = note.ifBlank { "No note" },
                            createdAt = editingNow.createdAt,
                            latitude = source?.latLng?.latitude ?: editingNow.latitude,
                            longitude = source?.latLng?.longitude ?: editingNow.longitude,
                            rating = source?.rating ?: editingNow.rating,
                            reviewsCount = source?.reviewsCount ?: editingNow.reviewsCount
                        )
                    )
                    viewModel.savedPlaceBeingEdited = null
                    navController.popBackStack()
                } else {
                    val source = pickedPlace ?: selected
                    viewModel.addSavedPlace(
                        SavedPlace(
                            id = System.currentTimeMillis(),
                            placeId = source?.placeId ?: "",
                            name = name,
                            address = address.ifBlank { "N/A" },
                            category = category.ifBlank { "General" },
                            note = note.ifBlank { "No note" },
                            createdAt = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm").format(java.util.Date()),
                            latitude = source?.latLng?.latitude,
                            longitude = source?.latLng?.longitude,
                            rating = source?.rating,
                            reviewsCount = source?.reviewsCount
                        )
                    )
                    navController.navigate("saved")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (editing != null) "Update Favourite" else "Save Note")
        }

        OutlinedButton(
            onClick = {
                viewModel.savedPlaceBeingEdited = null
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun SavedPlacesScreen(navController: NavController, viewModel: PlaceViewModel) {
    val items = viewModel.savedPlaces
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121820))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text("Favourite Places", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text("${items.size} saved favourite${if (items.size == 1) "" else "s"}", color = Color(0xFFB0BEC5))
        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2A36)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "No favourites yet. Add one from Add Place Note on the home map.",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2A36)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "View favourite on map",
                                        tint = Color(0xFFE57373),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable {
                                                val lat = item.latitude
                                                val lng = item.longitude
                                                if (lat != null && lng != null) {
                                                    viewModel.reopenSearchAfterDetailClose = false
                                                    viewModel.setPlace(
                                                        PlaceUI(
                                                            placeId = item.placeId.ifBlank { "saved_${item.id}" },
                                                            name = item.name,
                                                            address = item.address,
                                                            latLng = LatLng(lat, lng),
                                                            rating = item.rating,
                                                            distanceKm = 0.0,
                                                            reviewsCount = item.reviewsCount
                                                        )
                                                    )
                                                    navController.navigate("detail")
                                                }
                                            }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit favourite",
                                        tint = Color(0xFF5AA8FF),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable {
                                                viewModel.savedPlaceBeingEdited = item
                                                navController.navigate("add_note")
                                            }
                                    )
                                }
                            }
                            Text(item.address, color = Color(0xFFB0BEC5))
                            Spacer(Modifier.height(6.dp))
                            Text("Category: ${item.category}", color = Color(0xFF5AA8FF))
                            Text(item.note, color = Color.White)
                            if (item.latitude != null && item.longitude != null) {
                                Text(
                                    "Saved coordinates: %.5f, %.5f".format(item.latitude, item.longitude),
                                    color = Color(0xFF90A4AE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    "No coordinates stored for this item.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Added: ${item.createdAt}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { navController.navigate("home") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back To Home")
        }
    }
}

@Composable
fun MobilityInsightsScreen(navController: NavController, viewModel: PlaceViewModel) {
    val items = viewModel.savedPlaces
    val categoryCount = items.groupingBy { it.category }.eachCount().toList().sortedByDescending { it.second }
    val topCategory = categoryCount.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121820))
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mobility Insights", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text("Calculation / Processing screen for SDG 11 data.", color = Color(0xFFB0BEC5))

        MenuCard(
            title = "Total Logged Places: ${items.size}",
            subtitle = "Tracks user-observed mobility issues and points of interest.",
            icon = Icons.Default.Analytics,
            color = Color(0xFF1F2A36)
        ) {}

        MenuCard(
            title = "Top Category: ${topCategory?.first ?: "N/A"}",
            subtitle = "Count: ${topCategory?.second ?: 0}",
            icon = Icons.Default.Category,
            color = Color(0xFF1F2A36)
        ) {}

        MenuCard(
            title = "Problem Statement (SDG 11)",
            subtitle = "Urban users often spend extra time locating suitable destinations. Better mapping + personal logging can improve travel decisions and reduce inefficient movement.",
            icon = Icons.Default.Description,
            color = Color(0xFF1F2A36)
        ) {}

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = { navController.navigate("home") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back To Home")
        }
    }
}