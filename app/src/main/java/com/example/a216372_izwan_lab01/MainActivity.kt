package com.example.a216372_izwan_lab01

import android.content.Context
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import com.google.android.libraries.places.api.model.CircularBounds
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.navigation.NavController
import android.graphics.Bitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.random.Random

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
            val app = applicationContext as Lab5Application
            val viewModel: PlaceViewModel = viewModel(factory = PlaceViewModelFactory(app.savedPlaceRepository))

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

                    composable("trip_planner") {
                        TripPlannerScreen(navController)
                    }

                    composable(
                        "destination_explore/{placeId}/{placeName}",
                        arguments = listOf(
                            androidx.navigation.navArgument("placeId") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("placeName") { type = androidx.navigation.NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val placeId = backStackEntry.arguments?.getString("placeId") ?: ""
                        val placeName = backStackEntry.arguments?.getString("placeName") ?: ""
                        DestinationExploreScreen(
                            navController = navController,
                            viewModel = viewModel,
                            placeId = placeId,
                            placeName = placeName,
                        )
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
                            FunctionItem(Icons.Default.FlightTakeoff, "Travel") {
                                navController.navigate("trip_planner")
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

            AppFloatingBottomNavBar(
                navController = navController,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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

private fun NavController.navigateToMainTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppFloatingBottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val navEntry by navController.currentBackStackEntryAsState()
    val route = navEntry?.destination?.route.orEmpty()

    val homeSelected = route == "home"
    val favsSelected = route == "saved"
    val travelSelected = route == "trip_planner"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = false,
                    ambientColor = Color.Transparent,
                    spotColor = Color.Black.copy(alpha = 0.35f),
                )
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF252525))
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppFloatingBottomNavTab(
                label = "Home",
                icon = Icons.Default.Home,
                selected = homeSelected,
                onClick = { navController.navigateToMainTab("home") },
            )
            AppFloatingBottomNavTab(
                label = "Favs",
                icon = Icons.Default.Favorite,
                selected = favsSelected,
                onClick = { navController.navigateToMainTab("saved") },
            )
            AppFloatingBottomNavTab(
                label = "Travel",
                icon = Icons.Default.FlightTakeoff,
                selected = travelSelected,
                onClick = { navController.navigateToMainTab("trip_planner") },
            )
        }
    }
}

@Composable
private fun AppFloatingBottomNavTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = Color(0xFF5AA8FF)
    val idle = Color.White
    val iconTint = if (selected) accent else idle
    val textColor = if (selected) accent else idle.copy(alpha = 0.92f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = textColor, style = MaterialTheme.typography.labelMedium)
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
            Text("Within 10 km • Open now • Up to 10 results", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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

private const val NEARBY_RESTAURANT_RADIUS_METERS = 10_000.0
private const val NEARBY_RESTAURANT_LIMIT = 10

private val nearbyRestaurantSearchFields = listOf(
    Place.Field.ID,
    Place.Field.DISPLAY_NAME,
    Place.Field.FORMATTED_ADDRESS,
    Place.Field.LOCATION,
    Place.Field.RATING,
    Place.Field.USER_RATING_COUNT,
    Place.Field.PHOTO_METADATAS,
    Place.Field.OPENING_HOURS,
    Place.Field.CURRENT_OPENING_HOURS,
    Place.Field.UTC_OFFSET,
    Place.Field.TYPES,
    Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
)

/**
 * Nearby open restaurants via Places Nearby Search (not Autocomplete).
 * Radius: 10 km; only restaurants that [computeIsOpenNow] reports as open.
 */
private fun loadNearbyOpenRestaurants(
    context: Context,
    userLocation: LatLng,
    apiKey: String,
    photoMap: MutableMap<String, Bitmap>,
    isActive: () -> Boolean,
    onResult: (List<NearbyRestaurantUI>) -> Unit,
) {
    if (!Places.isInitialized()) Places.initialize(context, apiKey)
    val client = Places.createClient(context)
    val circle = CircularBounds.newInstance(userLocation, NEARBY_RESTAURANT_RADIUS_METERS)
    val request = SearchNearbyRequest.builder(circle, nearbyRestaurantSearchFields)
        .setIncludedTypes(listOf("restaurant"))
        .setMaxResultCount(20)
        .build()

    client.searchNearby(request)
        .addOnSuccessListener { response ->
            if (!isActive()) return@addOnSuccessListener
            val uiList = response.places
                .filter { isDiscoverableRestaurant(it) }
                .mapNotNull { p ->
                    val id = p.id ?: return@mapNotNull null
                    val latLng = p.location ?: return@mapNotNull null
                    val openNow = computeIsOpenNow(p)
                    if (openNow != true) return@mapNotNull null
                    val distanceKm = calculateDistance(
                        userLocation.latitude,
                        userLocation.longitude,
                        latLng.latitude,
                        latLng.longitude,
                    )
                    if (distanceKm > NEARBY_RESTAURANT_RADIUS_METERS / 1000.0) return@mapNotNull null
                    val name = placeDisplayName(p)
                    if (name.isBlank()) return@mapNotNull null
                    NearbyRestaurantUI(
                        placeId = id,
                        name = name,
                        address = p.formattedAddress.orEmpty(),
                        latLng = latLng,
                        rating = p.rating,
                        userRatingCount = p.userRatingCount,
                        distanceKm = distanceKm,
                        isOpenNow = true,
                    )
                }
                .sortedBy { it.distanceKm }
                .take(NEARBY_RESTAURANT_LIMIT)

            response.places.forEach { p ->
                val id = p.id ?: return@forEach
                if (uiList.none { it.placeId == id }) return@forEach
                p.photoMetadatas?.firstOrNull()?.let { meta ->
                    val photoReq = FetchPhotoRequest.builder(meta)
                        .setMaxWidth(900)
                        .setMaxHeight(450)
                        .build()
                    client.fetchPhoto(photoReq)
                        .addOnSuccessListener { resp ->
                            if (isActive()) photoMap[id] = resp.bitmap
                        }
                }
            }
            onResult(uiList)
        }
        .addOnFailureListener {
            if (isActive()) onResult(emptyList())
        }
}

@SuppressLint("MissingPermission")
@Composable
fun NearbyRestaurantsScreen(navController: NavController, viewModel: PlaceViewModel) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val placesApiKey = "AIzaSyBQFSEYxasR_AC2QiEl7BPmbKXAh167rX8"
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var restaurants by remember { mutableStateOf(listOf<NearbyRestaurantUI>()) }
    val photoMap = remember { mutableStateMapOf<String, Bitmap>() }
    var loadGeneration by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                userLocation = LatLng(it.latitude, it.longitude)
            }
        }
    }

    LaunchedEffect(userLocation) {
        val loc = userLocation ?: return@LaunchedEffect
        loadGeneration++
        val generation = loadGeneration
        val isActive = { loadGeneration == generation }
        isLoading = true
        restaurants = emptyList()
        photoMap.clear()

        loadNearbyOpenRestaurants(context, loc, placesApiKey, photoMap, isActive) { list ->
            if (!isActive()) return@loadNearbyOpenRestaurants
            restaurants = list
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
    val items by viewModel.savedPlaces.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121820))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
        }

        AppFloatingBottomNavBar(navController = navController)
    }
}

@Composable
fun MobilityInsightsScreen(navController: NavController, viewModel: PlaceViewModel) {
    val items by viewModel.savedPlaces.collectAsStateWithLifecycle()
    val categoryCount = items.groupingBy { it.category }.eachCount().toList().sortedByDescending { it.second }
    val topCategory = categoryCount.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121820))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
        }

        AppFloatingBottomNavBar(navController = navController)
    }
}

private data class TripDiscoverSlide(
    val headline: String,
    val caption: String,
    val photo: Bitmap? = null,
    val photoLoading: Boolean = true,
)

private data class AsiaDiscoverDestination(
    val headline: String,
    val caption: String,
    val searchQuery: String,
)

/** Famous Asia destinations — 5 are chosen at random each time Discover is opened. */
private val asiaDiscoverDestinationPool = listOf(
    AsiaDiscoverDestination(
        "Bangkok, Thailand",
        "Golden temples, floating markets, and legendary street food",
        "Bangkok, Thailand",
    ),
    AsiaDiscoverDestination(
        "Hat Yai, Thailand",
        "Southern hub near Malaysia—night markets, dim sum, and easy island hops",
        "Hat Yai, Thailand",
    ),
    AsiaDiscoverDestination(
        "Ho Chi Minh City, Vietnam",
        "Colonial lanes, coffee culture, and fast-paced Mekong Delta access",
        "Ho Chi Minh City, Vietnam",
    ),
    AsiaDiscoverDestination(
        "Hanoi, Vietnam",
        "Old Quarter charm, lake walks, and bowls of pho by the street",
        "Hanoi, Vietnam",
    ),
    AsiaDiscoverDestination(
        "Bali, Indonesia",
        "Rice terraces, surf beaches, and cliffside temples at sunset",
        "Bali, Indonesia",
    ),
    AsiaDiscoverDestination(
        "Phuket, Thailand",
        "Andaman beaches, island-hopping, and lively Patong nights",
        "Phuket, Thailand",
    ),
    AsiaDiscoverDestination(
        "Singapore",
        "Marina Bay skylines, hawker centers, and gardens in the city",
        "Singapore",
    ),
    AsiaDiscoverDestination(
        "Kuala Lumpur, Malaysia",
        "Petronas Towers, Batu Caves, and multicultural street eats",
        "Kuala Lumpur, Malaysia",
    ),
    AsiaDiscoverDestination(
        "Penang, Malaysia",
        "UNESCO George Town, murals, and famous char kway teow",
        "George Town, Penang, Malaysia",
    ),
    AsiaDiscoverDestination(
        "Tokyo, Japan",
        "Neon districts, sushi counters, and serene shrine gardens",
        "Tokyo, Japan",
    ),
    AsiaDiscoverDestination(
        "Kyoto, Japan",
        "Bamboo groves, geisha districts, and centuries of temples",
        "Kyoto, Japan",
    ),
    AsiaDiscoverDestination(
        "Seoul, South Korea",
        "Palaces, K-culture, and late-night BBQ alleys",
        "Seoul, South Korea",
    ),
    AsiaDiscoverDestination(
        "Taipei, Taiwan",
        "Night markets, hot springs, and Taipei 101 city views",
        "Taipei, Taiwan",
    ),
    AsiaDiscoverDestination(
        "Hong Kong",
        "Victoria Harbour, dim sum, and hikes above the skyline",
        "Hong Kong",
    ),
    AsiaDiscoverDestination(
        "Manila, Philippines",
        "Historic Intramuros, island getaways, and vibrant fiestas",
        "Manila, Philippines",
    ),
    AsiaDiscoverDestination(
        "Siem Reap, Cambodia",
        "Angkor Wat at dawn and Khmer heritage in every stone",
        "Siem Reap, Cambodia",
    ),
    AsiaDiscoverDestination(
        "Luang Prabang, Laos",
        "Mekong mornings, golden stupas, and slow Lao rhythm",
        "Luang Prabang, Laos",
    ),
    AsiaDiscoverDestination(
        "Kathmandu, Nepal",
        "Himalayan gateway, stupas, and trekking trailheads",
        "Kathmandu, Nepal",
    ),
    AsiaDiscoverDestination(
        "Colombo, Sri Lanka",
        "Tea country access, coastal forts, and spice-laced cuisine",
        "Colombo, Sri Lanka",
    ),
    AsiaDiscoverDestination(
        "Maldives",
        "Overwater villas, turquoise lagoons, and reef snorkeling",
        "Malé, Maldives",
    ),
    AsiaDiscoverDestination(
        "Jaipur, India",
        "Pink City palaces, bazaars, and Rajasthan desert forts",
        "Jaipur, India",
    ),
    AsiaDiscoverDestination(
        "Dubai, UAE",
        "Desert safaris, souks, and record-breaking modern architecture",
        "Dubai, United Arab Emirates",
    ),
)

private fun pickRandomAsiaDiscoverDestinations(count: Int = 5): List<AsiaDiscoverDestination> =
    asiaDiscoverDestinationPool.shuffled(Random).take(count.coerceAtMost(asiaDiscoverDestinationPool.size))

private data class TripRegionCandidate(
    val placeId: String,
    val title: String,
    val subtitle: String,
)

private val tripGeographyPlaceTypes = listOf(
    "country",
    "administrative_area_level_1",
    "administrative_area_level_2",
    "locality",
)

private val tripPlannerDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

private fun localDateToPickerMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun pickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun tripRegionAutocompleteSearch(
    context: Context,
    query: String,
    onResult: (List<TripRegionCandidate>) -> Unit,
) {
    val q = query.trim()
    if (q.length < 2) {
        onResult(emptyList())
        return
    }
    if (!Places.isInitialized()) {
        Places.initialize(context, "AIzaSyBQFSEYxasR_AC2QiEl7BPmbKXAh167rX8")
    }
    val client = Places.createClient(context)
    val request = FindAutocompletePredictionsRequest.builder()
        .setQuery(q)
        .setTypesFilter(tripGeographyPlaceTypes)
        .build()
    client.findAutocompletePredictions(request)
        .addOnSuccessListener { resp ->
            val list = resp.autocompletePredictions.map { p ->
                TripRegionCandidate(
                    placeId = p.placeId,
                    title = p.getPrimaryText(null).toString(),
                    subtitle = p.getSecondaryText(null).toString(),
                )
            }
            onResult(list.take(8))
        }
        .addOnFailureListener { onResult(emptyList()) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripPlannerScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tripPlacesApiKey = "AIzaSyBQFSEYxasR_AC2QiEl7BPmbKXAh167rX8"
    var placeQuery by remember { mutableStateOf("") }
    var regionPredictions by remember { mutableStateOf(listOf<TripRegionCandidate>()) }
    var selectedRegion by remember { mutableStateOf<TripRegionCandidate?>(null) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var selectedTravelMode by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(placeQuery) {
        if (placeQuery.trim().length < 2) {
            regionPredictions = emptyList()
            return@LaunchedEffect
        }
        delay(400)
        tripRegionAutocompleteSearch(context, placeQuery) { regionPredictions = it }
    }

    val startDateLabel = startDate?.format(tripPlannerDateFormatter) ?: "Start"
    val endDateLabel = endDate?.format(tripPlannerDateFormatter) ?: "End"

    var slides by remember { mutableStateOf(listOf<TripDiscoverSlide>()) }
    var slideLoadGeneration by remember { mutableIntStateOf(0) }
    var skipNextResumeReload by remember { mutableStateOf(true) }
    var resumeReloadToken by remember { mutableIntStateOf(0) }

    fun reloadDiscoverSlides() {
        slideLoadGeneration++
        val generation = slideLoadGeneration
        val isActive = { slideLoadGeneration == generation }
        val picked = pickRandomAsiaDiscoverDestinations(5)
        loadTripDiscoverSlides(context, picked, tripPlacesApiKey, isActive) { loaded ->
            if (isActive()) slides = loaded
        }
    }

    LaunchedEffect(Unit) {
        reloadDiscoverSlides()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (skipNextResumeReload) {
                    skipNextResumeReload = false
                } else {
                    resumeReloadToken++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeReloadToken) {
        if (resumeReloadToken > 0) reloadDiscoverSlides()
    }

    val pagerState = rememberPagerState(pageCount = { slides.size.coerceAtLeast(1) })

    LaunchedEffect(slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4500)
            val next = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(next, animationSpec = tween(durationMillis = 600))
        }
    }

    val pageBg = Color(0xFF121820)
    val cardBg = Color(0xFF2B2B2B)

    Box(Modifier.fillMaxSize().background(pageBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Discover",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Where to travel ?",
                    color = Color(0xFFB0BEC5),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                DiscoverSearchRow(
                    value = placeQuery,
                    onValueChange = {
                        placeQuery = it
                    },
                    placeholder = "Country, state, or city",
                    surfaceColor = cardBg,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF5AA8FF)
                        )
                    }
                )

                if (regionPredictions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2A36))
                    ) {
                        LazyColumn {
                            items(regionPredictions, key = { it.placeId }) { item ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            placeQuery = listOf(item.title, item.subtitle)
                                                .filter { it.isNotBlank() }
                                                .joinToString(", ")
                                            selectedRegion = item
                                            regionPredictions = emptyList()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        item.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (item.subtitle.isNotBlank()) {
                                        Text(
                                            item.subtitle,
                                            color = Color(0xFF90A4AE),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF2F3D4A))
                            }
                        }
                    }
                }

                DiscoverDateBar(
                    startLabel = startDateLabel,
                    endLabel = endDateLabel,
                    surfaceColor = cardBg,
                    onStartClick = { showStartDatePicker = true },
                    onEndClick = { showEndDatePicker = true }
                )

                if (showStartDatePicker) {
                    val startPickerState = rememberDatePickerState(
                        initialSelectedDateMillis = (startDate ?: LocalDate.now())
                            .let { localDateToPickerMillis(it) }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showStartDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    startPickerState.selectedDateMillis?.let { millis ->
                                        startDate = pickerMillisToLocalDate(millis)
                                        if (endDate != null && startDate != null && endDate!!.isBefore(startDate)) {
                                            endDate = startDate
                                        }
                                    }
                                    showStartDatePicker = false
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = startPickerState)
                    }
                }

                if (showEndDatePicker) {
                    val minSelectable = startDate ?: LocalDate.now()
                    val endSelectableDates = remember(minSelectable) {
                        object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                val d = pickerMillisToLocalDate(utcTimeMillis)
                                return !d.isBefore(minSelectable)
                            }
                        }
                    }
                    val endPickerState = rememberDatePickerState(
                        initialSelectedDateMillis = localDateToPickerMillis(
                            endDate ?: startDate ?: LocalDate.now()
                        ),
                        selectableDates = endSelectableDates
                    )
                    DatePickerDialog(
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    endPickerState.selectedDateMillis?.let { millis ->
                                        val picked = pickerMillisToLocalDate(millis)
                                        endDate = if (startDate != null && picked.isBefore(startDate)) {
                                            startDate
                                        } else {
                                            picked
                                        }
                                    }
                                    showEndDatePicker = false
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = endPickerState)
                    }
                }

                Button(
                    onClick = {
                        val region = selectedRegion
                        if (region != null) {
                            val encodedId = java.net.URLEncoder.encode(region.placeId, "UTF-8")
                            val encodedName = java.net.URLEncoder.encode(
                                listOf(region.title, region.subtitle).filter { it.isNotBlank() }.joinToString(", "),
                                "UTF-8"
                            )
                            navController.navigate("destination_explore/$encodedId/$encodedName")
                        } else if (placeQuery.isNotBlank()) {
                            val encodedId = java.net.URLEncoder.encode(placeQuery, "UTF-8")
                            val encodedName = java.net.URLEncoder.encode(placeQuery, "UTF-8")
                            navController.navigate("destination_explore/$encodedId/$encodedName")
                        } else {
                            android.widget.Toast.makeText(context, "Please enter a destination first", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D5BFF),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("Search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "How to travel ?",
                    color = Color(0xFFB0BEC5),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TravelModeOrb(
                        label = "Planes",
                        icon = Icons.Default.Flight,
                        surfaceColor = cardBg,
                        selected = selectedTravelMode == 0,
                        onClick = { selectedTravelMode = 0 }
                    )
                    TravelModeOrb(
                        label = "Ferries",
                        icon = Icons.Default.DirectionsBoat,
                        surfaceColor = cardBg,
                        selected = selectedTravelMode == 1,
                        onClick = { selectedTravelMode = 1 }
                    )
                    TravelModeOrb(
                        label = "Taxi",
                        icon = Icons.Default.DirectionsCar,
                        surfaceColor = cardBg,
                        selected = selectedTravelMode == 2,
                        onClick = { selectedTravelMode = 2 }
                    )
                    TravelModeOrb(
                        label = "Ride",
                        icon = Icons.Default.TwoWheeler,
                        surfaceColor = cardBg,
                        selected = selectedTravelMode == 3,
                        onClick = { selectedTravelMode = 3 }
                    )
                }

                Text(
                    text = "Maybe you're interested to",
                    color = Color(0xFFB0BEC5),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    pageSpacing = 14.dp
                ) { page ->
                    if (slides.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2A36)),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = Color(0xFF5AA8FF),
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                        return@HorizontalPager
                    }
                    val item = slides[page]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2A36)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            when {
                                item.photo != null -> {
                                    androidx.compose.foundation.Image(
                                        bitmap = item.photo.asImageBitmap(),
                                        contentDescription = item.headline,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                item.photoLoading -> {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF1F2A36), Color(0xFF121820))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF5AA8FF),
                                            modifier = Modifier.size(36.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF2A3F55), Color(0xFF121820))
                                                )
                                            )
                                    )
                                }
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.55f)
                                            ),
                                            startY = 120f
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.headline,
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.caption,
                                    color = Color.White.copy(alpha = 0.92f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            AppFloatingBottomNavBar(navController = navController)
        }
    }
}

@Composable
private fun DiscoverSearchRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    surfaceColor: Color,
    leadingIcon: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(surfaceColor)
            .border(1.dp, Color(0xFF3D4A57), RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leadingIcon()
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = Color(0xFF78909C),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    inner()
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
    }
}

@Composable
private fun DiscoverDateBar(
    startLabel: String,
    endLabel: String,
    surfaceColor: Color,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(surfaceColor)
            .border(1.dp, Color(0xFF3D4A57), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = Color(0xFF5AA8FF),
            modifier = Modifier.padding(end = 8.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onStartClick)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = startLabel,
                color = if (startLabel == "Start") Color(0xFF78909C) else Color.White,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(Color(0xFF546E7A))
        )
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onEndClick)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = endLabel,
                color = if (endLabel == "End") Color(0xFF78909C) else Color.White,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun TravelModeOrb(
    label: String,
    icon: ImageVector,
    surfaceColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ring = if (selected) Color(0xFF5AA8FF) else Color(0xFF546E7A)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(2.dp, ring, CircleShape)
                .background(surfaceColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) Color(0xFF5AA8FF) else Color(0xFFB0BEC5),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = Color(0xFFB0BEC5),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────
// Destination Explore Screen
// ─────────────────────────────────────────────────

private enum class ExploreTab { FOOD, ATTRACTIONS, HOTELS }

private const val EXPLORE_RESTAURANT_LIMIT = 10
private const val EXPLORE_ATTRACTION_LIMIT = 10
private const val EXPLORE_HOTEL_LIMIT = 10
private const val EXPLORE_CAROUSEL_PHOTOS = 5

private data class ExplorePlace(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng,
    val distanceKm: Double,
    val rating: Double?,
    val reviewCount: Int?,
    val photo: Bitmap?,
    val isOpen: Boolean?,
)

private val exploreSearchFields = listOf(
    Place.Field.ID,
    Place.Field.DISPLAY_NAME,
    Place.Field.FORMATTED_ADDRESS,
    Place.Field.LOCATION,
    Place.Field.RATING,
    Place.Field.USER_RATING_COUNT,
    Place.Field.PHOTO_METADATAS,
    Place.Field.OPENING_HOURS,
    Place.Field.CURRENT_OPENING_HOURS,
    Place.Field.UTC_OFFSET,
    Place.Field.TYPES,
    Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
)

private fun openExplorePlaceOnDetail(
    navController: NavController,
    viewModel: PlaceViewModel,
    place: ExplorePlace,
) {
    viewModel.reopenSearchAfterDetailClose = false
    viewModel.setPlace(
        PlaceUI(
            placeId = place.placeId,
            name = place.name,
            address = place.address,
            latLng = place.latLng,
            rating = place.rating,
            distanceKm = place.distanceKm,
            reviewsCount = place.reviewCount,
        )
    )
    navController.navigate("detail")
}

private const val EXPLORE_NEARBY_RADIUS_METERS = 35_000.0

private fun placeDisplayName(p: Place): String = p.displayName?.trim().orEmpty()

private fun isGenericRestaurantName(name: String): Boolean {
    val n = name.trim().lowercase(Locale.getDefault())
    return n in setOf(
        "restaurant", "restaurants", "food", "meal delivery", "cafe",
        "establishment", "food and drink", "dining",
    )
}

private fun isGenericAttractionName(name: String): Boolean {
    val n = name.trim().lowercase(Locale.getDefault())
    return n in setOf(
        "tourist attraction", "tourist attractions", "attraction", "attractions",
        "point of interest", "establishment", "premise", "place", "locality",
    )
}

private fun isGenericHotelName(name: String): Boolean {
    val n = name.trim().lowercase(Locale.getDefault())
    return n in setOf(
        "hotel", "hotels", "lodging", "motel", "motels", "accommodation",
        "accommodations", "establishment", "resort", "inn",
    )
}

private fun placeToExplorePlace(
    p: Place,
    includeOpen: Boolean,
    fromCenter: LatLng?,
): ExplorePlace? {
    val id = p.id ?: return null
    val name = placeDisplayName(p)
    val latLng = p.location ?: return null
    if (name.isBlank()) return null
    val distanceKm = fromCenter?.let { center ->
        calculateDistance(center.latitude, center.longitude, latLng.latitude, latLng.longitude)
    } ?: 0.0
    return ExplorePlace(
        placeId = id,
        name = name,
        address = p.formattedAddress.orEmpty(),
        latLng = latLng,
        distanceKm = distanceKm,
        rating = p.rating,
        reviewCount = p.userRatingCount,
        photo = null,
        isOpen = if (includeOpen) computeIsOpenNow(p) else null,
    )
}

private fun isDiscoverableRestaurant(place: Place): Boolean {
    val name = placeDisplayName(place)
    if (name.isBlank() || isGenericRestaurantName(name)) return false
    if (isGeographyOnlyPlace(place)) return false
    val types = place.placeTypes?.map { it.lowercase(Locale.getDefault()) }.orEmpty()
    if (types.isNotEmpty()) {
        val foodRelated = types.any { it == "restaurant" || it.endsWith("_restaurant") || it == "cafe" || it == "food" }
        if (!foodRelated) return false
    }
    return true
}

private fun isDiscoverableAttraction(place: Place): Boolean {
    val name = placeDisplayName(place)
    if (name.isBlank() || isGenericAttractionName(name)) return false
    if (isGeographyOnlyPlace(place)) return false
    val types = place.placeTypes?.map { it.lowercase(Locale.getDefault()) }.orEmpty()
    if (types.isNotEmpty() && types.none { it in meaningfulAttractionTypes }) return false
    return true
}

private val meaningfulHotelTypes = setOf(
    "lodging",
    "hotel",
    "motel",
    "resort_hotel",
    "bed_and_breakfast",
    "hostel",
    "guest_house",
    "extended_stay_hotel",
    "japanese_inn",
    "budget_japanese_inn",
)

private fun isDiscoverableHotel(place: Place): Boolean {
    val name = placeDisplayName(place)
    if (name.isBlank() || isGenericHotelName(name)) return false
    val types = place.placeTypes?.map { it.lowercase(Locale.getDefault()) }.orEmpty()
    if (types.isNotEmpty()) {
        if (types.any { it in administrativePlaceTypes } && types.none { it in meaningfulHotelTypes }) {
            return false
        }
        val hotelRelated = types.any {
            it in meaningfulHotelTypes || it.endsWith("_hotel") || it == "inn"
        }
        if (!hotelRelated) return false
    }
    return true
}

/** Same pattern as DetailScreen: FetchPlace → photoMetadatas → FetchPhoto */
private fun fetchFirstPlacePhoto(
    client: com.google.android.libraries.places.api.net.PlacesClient,
    placeId: String,
    maxWidth: Int,
    maxHeight: Int,
    onBitmap: (Bitmap?) -> Unit,
) {
    val placeRequest = FetchPlaceRequest.builder(
        placeId,
        listOf(Place.Field.ID, Place.Field.PHOTO_METADATAS),
    ).build()
    client.fetchPlace(placeRequest)
        .addOnSuccessListener { placeResponse ->
            val meta = placeResponse.place.photoMetadatas?.firstOrNull()
            if (meta == null) {
                onBitmap(null)
                return@addOnSuccessListener
            }
            val photoRequest = FetchPhotoRequest.builder(meta)
                .setMaxWidth(maxWidth)
                .setMaxHeight(maxHeight)
                .build()
            client.fetchPhoto(photoRequest)
                .addOnSuccessListener { onBitmap(it.bitmap) }
                .addOnFailureListener { onBitmap(null) }
        }
        .addOnFailureListener { onBitmap(null) }
}

private fun resolveRegionPlaceId(
    client: com.google.android.libraries.places.api.net.PlacesClient,
    placeId: String,
    locationName: String,
    onResolved: (String?) -> Unit,
) {
    val looksLikePlaceId = placeId.isNotBlank() && !placeId.contains(",") && placeId.length > 8
    if (looksLikePlaceId) {
        onResolved(placeId)
        return
    }
    client.findAutocompletePredictions(
        FindAutocompletePredictionsRequest.builder()
            .setQuery(locationName)
            .setTypesFilter(tripGeographyPlaceTypes)
            .build()
    )
        .addOnSuccessListener { resp -> onResolved(resp.autocompletePredictions.firstOrNull()?.placeId) }
        .addOnFailureListener { onResolved(null) }
}

/** Trip Planner carousel: random Asia picks with Places hero photos (same as Destination Explore). */
private fun loadTripDiscoverSlides(
    context: Context,
    destinations: List<AsiaDiscoverDestination>,
    apiKey: String,
    isActive: () -> Boolean,
    onSlides: (List<TripDiscoverSlide>) -> Unit,
) {
    if (destinations.isEmpty()) {
        onSlides(emptyList())
        return
    }
    if (!Places.isInitialized()) Places.initialize(context, apiKey)
    val client = Places.createClient(context)
    val slots = Array(destinations.size) { i ->
        val d = destinations[i]
        TripDiscoverSlide(d.headline, d.caption, photo = null, photoLoading = true)
    }
    var pending = destinations.size

    fun emit() {
        if (isActive()) onSlides(slots.toList())
    }

    fun completeOne(index: Int, slide: TripDiscoverSlide) {
        if (!isActive()) return
        slots[index] = slide
        pending--
        if (pending == 0) emit()
    }

    onSlides(slots.toList())

    destinations.forEachIndexed { index, dest ->
        resolveRegionPlaceId(client, "", dest.searchQuery) { regionId ->
            if (!isActive()) return@resolveRegionPlaceId
            if (regionId == null) {
                completeOne(index, TripDiscoverSlide(dest.headline, dest.caption, photoLoading = false))
                return@resolveRegionPlaceId
            }
            fetchFirstPlacePhoto(client, regionId, 1200, 700) { bmp ->
                if (!isActive()) return@fetchFirstPlacePhoto
                completeOne(
                    index,
                    TripDiscoverSlide(dest.headline, dest.caption, photo = bmp, photoLoading = false),
                )
            }
        }
    }
}

private val meaningfulAttractionTypes = setOf(
    "tourist_attraction",
    "museum",
    "art_gallery",
    "park",
    "natural_feature",
    "place_of_worship",
    "hindu_temple",
    "buddhist_temple",
    "church",
    "mosque",
    "amusement_park",
    "zoo",
    "aquarium",
    "beach",
    "national_park",
    "historical_landmark",
    "cultural_landmark",
    "monument",
    "landmark",
    "performing_arts_theater",
    "stadium",
)

private val administrativePlaceTypes = setOf(
    "locality",
    "administrative_area_level_1",
    "administrative_area_level_2",
    "administrative_area_level_3",
    "administrative_area_level_4",
    "country",
    "political",
    "colloquial_area",
    "geocode",
    "continent",
)

private data class RegionSearchContext(
    val bounds: com.google.android.libraries.places.api.model.RectangularBounds?,
    val resolvedRegionPlaceId: String?,
    val regionCenter: LatLng?,
)

private fun isGeographyOnlyPlace(place: Place): Boolean {
    val types = place.placeTypes?.map { it.lowercase(Locale.getDefault()) }.orEmpty()
    if (types.isEmpty()) return false
    return types.any { it in administrativePlaceTypes } &&
        types.none { it in meaningfulAttractionTypes }
}

private fun resolveRegionSearchContext(
    client: com.google.android.libraries.places.api.net.PlacesClient,
    regionPlaceId: String,
    locationName: String,
    onReady: (RegionSearchContext) -> Unit,
) {
    resolveRegionPlaceId(client, regionPlaceId, locationName) { regionId ->
        if (regionId == null) {
            onReady(RegionSearchContext(bounds = null, resolvedRegionPlaceId = null, regionCenter = null))
            return@resolveRegionPlaceId
        }
        client.fetchPlace(
            FetchPlaceRequest.builder(regionId, listOf(Place.Field.LOCATION)).build()
        )
            .addOnSuccessListener { resp ->
                val center = resp.place.location
                if (center == null) {
                    onReady(RegionSearchContext(bounds = null, resolvedRegionPlaceId = regionId, regionCenter = null))
                    return@addOnSuccessListener
                }
                val delta = 0.65
                val sw = LatLng(center.latitude - delta, center.longitude - delta)
                val ne = LatLng(center.latitude + delta, center.longitude + delta)
                onReady(
                    RegionSearchContext(
                        bounds = com.google.android.libraries.places.api.model.RectangularBounds.newInstance(sw, ne),
                        resolvedRegionPlaceId = regionId,
                        regionCenter = center,
                    )
                )
            }
            .addOnFailureListener {
                onReady(RegionSearchContext(bounds = null, resolvedRegionPlaceId = regionId, regionCenter = null))
            }
    }
}

private fun excludedRegionPlaceIds(
    ctx: RegionSearchContext,
    regionPlaceIdParam: String,
): Set<String> = buildSet {
    ctx.resolvedRegionPlaceId?.let { add(it) }
    if (regionPlaceIdParam.isNotBlank() && !regionPlaceIdParam.contains(",")) add(regionPlaceIdParam)
}

/**
 * POI discovery via Nearby Search + Text Search (not Autocomplete).
 * Nearby uses [includedTypes]; Text Search uses [textIncludedType] when set.
 */
private fun discoverPlacesInRegion(
    client: com.google.android.libraries.places.api.net.PlacesClient,
    ctx: RegionSearchContext,
    excludedIds: Set<String>,
    nearbyIncludedTypes: List<String>,
    textQuery: String,
    textIncludedType: String?,
    filterPlace: (Place) -> Boolean,
    onPlaces: (List<Place>) -> Unit,
) {
    val merged = linkedMapOf<String, Place>()

    fun addPlaces(places: List<Place>) {
        places.forEach { p ->
            val id = p.id ?: return@forEach
            if (id !in excludedIds && id !in merged && filterPlace(p)) {
                merged[id] = p
            }
        }
    }

    var pending = if (ctx.regionCenter != null) 2 else 1

    fun complete() {
        pending--
        if (pending == 0) onPlaces(merged.values.toList())
    }

    ctx.regionCenter?.let { center ->
        val circle = CircularBounds.newInstance(center, EXPLORE_NEARBY_RADIUS_METERS)
        val nearbyRequest = SearchNearbyRequest.builder(circle, exploreSearchFields)
            .setIncludedTypes(nearbyIncludedTypes)
            .setMaxResultCount(20)
            .build()
        client.searchNearby(nearbyRequest)
            .addOnSuccessListener { response -> addPlaces(response.places); complete() }
            .addOnFailureListener { complete() }
    }

    val textBuilder = SearchByTextRequest.builder(textQuery.trim(), exploreSearchFields)
        .setMaxResultCount(20)
    ctx.bounds?.let { textBuilder.setLocationRestriction(it) }
    textIncludedType?.let { textBuilder.setIncludedType(it) }
    client.searchByText(textBuilder.build())
        .addOnSuccessListener { response -> addPlaces(response.places); complete() }
        .addOnFailureListener { complete() }
}

private fun loadPhotosForPlaces(
    client: com.google.android.libraries.places.api.net.PlacesClient,
    places: List<Place>,
    photoMap: MutableMap<String, Bitmap>,
    isActive: () -> Boolean,
) {
    places.forEach { p ->
        val id = p.id ?: return@forEach
        p.photoMetadatas?.firstOrNull()?.let { meta ->
            val photoReq = FetchPhotoRequest.builder(meta).setMaxWidth(600).setMaxHeight(400).build()
            client.fetchPhoto(photoReq)
                .addOnSuccessListener { if (isActive()) photoMap[id] = it.bitmap }
        }
    }
}

/** DetailScreen-style: multiple photos from one place's photoMetadatas (skip hero's first photo). */
private fun fetchPlaceGalleryPhotos(
    client: com.google.android.libraries.places.api.net.PlacesClient,
    placeId: String,
    maxPhotos: Int,
    skipFirstMetadata: Boolean,
    maxWidth: Int,
    maxHeight: Int,
    onPhotos: (List<Bitmap>) -> Unit,
) {
    client.fetchPlace(
        FetchPlaceRequest.builder(placeId, listOf(Place.Field.ID, Place.Field.PHOTO_METADATAS)).build()
    )
        .addOnSuccessListener { resp ->
            val metas = resp.place.photoMetadatas
                ?.let { if (skipFirstMetadata && it.isNotEmpty()) it.drop(1) else it }
                ?.take(maxPhotos)
                .orEmpty()
            if (metas.isEmpty()) {
                onPhotos(emptyList())
                return@addOnSuccessListener
            }
            val slots = arrayOfNulls<Bitmap>(metas.size)
            var pending = metas.size
            metas.forEachIndexed { index, meta ->
                val photoRequest = FetchPhotoRequest.builder(meta)
                    .setMaxWidth(maxWidth)
                    .setMaxHeight(maxHeight)
                    .build()
                client.fetchPhoto(photoRequest)
                    .addOnSuccessListener { fetchResp ->
                        slots[index] = fetchResp.bitmap
                        pending--
                        if (pending == 0) onPhotos(slots.filterNotNull())
                    }
                    .addOnFailureListener {
                        pending--
                        if (pending == 0) onPhotos(slots.filterNotNull())
                    }
            }
        }
        .addOnFailureListener { onPhotos(emptyList()) }
}

/**
 * Carousel: hero-style destination photos (region gallery + tourist attractions).
 * One image per place; skips the region's first photo (already used as hero).
 */
private fun loadDestinationCarouselPhotos(
    context: Context,
    regionPlaceId: String,
    locationQuery: String,
    isActive: () -> Boolean,
    onDone: (List<Bitmap>) -> Unit,
) {
    val client = Places.createClient(context)
    val collected = mutableListOf<Bitmap>()
    val usedPlaceIds = mutableSetOf<String>()
    var finished = false

    fun finishIfReady() {
        if (!isActive() || finished) return
        finished = true
        onDone(collected.take(EXPLORE_CAROUSEL_PHOTOS))
    }

    fun appendAttractionPhotos() {
        val needed = EXPLORE_CAROUSEL_PHOTOS - collected.size
        if (needed <= 0) {
            finishIfReady()
            return
        }
        resolveRegionSearchContext(client, regionPlaceId, locationQuery) { ctx ->
            if (!isActive()) return@resolveRegionSearchContext
            val excluded = excludedRegionPlaceIds(ctx, regionPlaceId)
            discoverPlacesInRegion(
                client = client,
                ctx = ctx,
                excludedIds = excluded,
                nearbyIncludedTypes = listOf("tourist_attraction"),
                textQuery = "tourist attractions in ${locationQuery.trim()}",
                textIncludedType = "tourist_attraction",
                filterPlace = { isDiscoverableAttraction(it) && it.id !in usedPlaceIds },
            ) { places ->
                if (!isActive()) return@discoverPlacesInRegion
                val picks = places.take(needed + 4)
                if (picks.isEmpty()) {
                    finishIfReady()
                    return@discoverPlacesInRegion
                }
                var pending = picks.size
                picks.forEach { place ->
                    place.id?.let { usedPlaceIds.add(it) }
                    val meta = place.photoMetadatas?.firstOrNull()
                    if (meta == null) {
                        pending--
                        if (pending == 0) finishIfReady()
                        return@forEach
                    }
                    val photoReq = FetchPhotoRequest.builder(meta).setMaxWidth(900).setMaxHeight(650).build()
                    client.fetchPhoto(photoReq)
                        .addOnSuccessListener { resp ->
                            if (!isActive()) return@addOnSuccessListener
                            if (collected.size < EXPLORE_CAROUSEL_PHOTOS) collected.add(resp.bitmap)
                            pending--
                            if (pending == 0) finishIfReady()
                        }
                        .addOnFailureListener {
                            pending--
                            if (pending == 0) finishIfReady()
                        }
                }
            }
        }
    }

    resolveRegionPlaceId(client, regionPlaceId, locationQuery) { regionId ->
        if (!isActive()) return@resolveRegionPlaceId
        if (regionId == null) {
            appendAttractionPhotos()
            return@resolveRegionPlaceId
        }
        usedPlaceIds.add(regionId)
        val slotsNeeded = EXPLORE_CAROUSEL_PHOTOS - collected.size
        fetchPlaceGalleryPhotos(
            client = client,
            placeId = regionId,
            maxPhotos = slotsNeeded,
            skipFirstMetadata = true,
            maxWidth = 900,
            maxHeight = 650,
        ) { regionPhotos ->
            if (!isActive()) return@fetchPlaceGalleryPhotos
            collected.addAll(regionPhotos)
            if (collected.size >= EXPLORE_CAROUSEL_PHOTOS) {
                finishIfReady()
            } else {
                appendAttractionPhotos()
            }
        }
    }
}

/** Hero: destination region photo (same as detail page for a place). */
private fun loadDestinationHeroPhoto(
    context: Context,
    placeId: String,
    locationName: String,
    isActive: () -> Boolean,
    onDone: (Bitmap?) -> Unit,
) {
    val client = Places.createClient(context)
    resolveRegionPlaceId(client, placeId, locationName) { regionId ->
        if (!isActive()) return@resolveRegionPlaceId
        if (regionId == null) {
            onDone(null)
            return@resolveRegionPlaceId
        }
        fetchFirstPlacePhoto(client, regionId, 1200, 800) { bmp ->
            if (!isActive()) return@fetchFirstPlacePhoto
            if (bmp != null) {
                onDone(bmp)
            } else {
                resolveRegionSearchContext(client, placeId, locationName) { ctx ->
                    if (!isActive()) {
                        onDone(null)
                        return@resolveRegionSearchContext
                    }
                    discoverPlacesInRegion(
                        client = client,
                        ctx = ctx,
                        excludedIds = excludedRegionPlaceIds(ctx, placeId),
                        nearbyIncludedTypes = listOf("restaurant"),
                        textQuery = "restaurants in ${locationName.trim()}",
                        textIncludedType = "restaurant",
                        filterPlace = { isDiscoverableRestaurant(it) },
                    ) { places ->
                        if (!isActive() || places.isEmpty()) {
                            onDone(null)
                            return@discoverPlacesInRegion
                        }
                        fetchFirstPlacePhoto(client, places.first().id!!, 1200, 800, onDone)
                    }
                }
            }
        }
    }
}

private fun finalizeExplorePlaces(
    candidates: List<ExplorePlace>,
    limit: Int,
): List<ExplorePlace> =
    candidates
        .sortedWith(
            compareByDescending<ExplorePlace> { it.rating ?: 0.0 }
                .thenByDescending { it.reviewCount ?: 0 }
        )
        .take(limit)

private fun loadDestinationRestaurants(
    context: Context,
    locationQuery: String,
    regionPlaceId: String,
    photoMap: MutableMap<String, Bitmap>,
    isActive: () -> Boolean,
    onDone: (List<ExplorePlace>) -> Unit,
) {
    val client = Places.createClient(context)
    val location = locationQuery.trim()
    resolveRegionSearchContext(client, regionPlaceId, location) { ctx ->
        if (!isActive()) return@resolveRegionSearchContext
        discoverPlacesInRegion(
            client = client,
            ctx = ctx,
            excludedIds = excludedRegionPlaceIds(ctx, regionPlaceId),
            nearbyIncludedTypes = listOf("restaurant"),
            textQuery = "restaurants in $location",
            textIncludedType = "restaurant",
            filterPlace = { isDiscoverableRestaurant(it) },
        ) { places ->
            if (!isActive()) return@discoverPlacesInRegion
            val center = ctx.regionCenter
            val explorePlaces = places.mapNotNull { placeToExplorePlace(it, includeOpen = true, fromCenter = center) }
            val sorted = finalizeExplorePlaces(explorePlaces, EXPLORE_RESTAURANT_LIMIT)
            loadPhotosForPlaces(client, places.filter { p -> sorted.any { it.placeId == p.id } }, photoMap, isActive)
            onDone(sorted)
        }
    }
}

private fun loadDestinationAttractions(
    context: Context,
    locationQuery: String,
    regionPlaceId: String,
    photoMap: MutableMap<String, Bitmap>,
    isActive: () -> Boolean,
    onDone: (List<ExplorePlace>) -> Unit,
) {
    val client = Places.createClient(context)
    val location = locationQuery.trim()
    resolveRegionSearchContext(client, regionPlaceId, location) { ctx ->
        if (!isActive()) return@resolveRegionSearchContext
        discoverPlacesInRegion(
            client = client,
            ctx = ctx,
            excludedIds = excludedRegionPlaceIds(ctx, regionPlaceId),
            nearbyIncludedTypes = listOf("tourist_attraction"),
            textQuery = "tourist attractions in $location",
            textIncludedType = "tourist_attraction",
            filterPlace = { isDiscoverableAttraction(it) },
        ) { places ->
            if (!isActive()) return@discoverPlacesInRegion
            val center = ctx.regionCenter
            val explorePlaces = places.mapNotNull { placeToExplorePlace(it, includeOpen = false, fromCenter = center) }
            val sorted = finalizeExplorePlaces(explorePlaces, EXPLORE_ATTRACTION_LIMIT)
            loadPhotosForPlaces(client, places.filter { p -> sorted.any { it.placeId == p.id } }, photoMap, isActive)
            onDone(sorted)
        }
    }
}

private fun loadDestinationHotels(
    context: Context,
    locationQuery: String,
    regionPlaceId: String,
    photoMap: MutableMap<String, Bitmap>,
    isActive: () -> Boolean,
    onDone: (List<ExplorePlace>) -> Unit,
) {
    val client = Places.createClient(context)
    val location = locationQuery.trim()
    resolveRegionSearchContext(client, regionPlaceId, location) { ctx ->
        if (!isActive()) return@resolveRegionSearchContext
        discoverPlacesInRegion(
            client = client,
            ctx = ctx,
            excludedIds = excludedRegionPlaceIds(ctx, regionPlaceId),
            nearbyIncludedTypes = listOf("lodging", "hotel"),
            textQuery = "hotels in $location",
            textIncludedType = "lodging",
            filterPlace = { isDiscoverableHotel(it) },
        ) { places ->
            if (!isActive()) return@discoverPlacesInRegion
            val center = ctx.regionCenter
            val explorePlaces = places.mapNotNull { placeToExplorePlace(it, includeOpen = false, fromCenter = center) }
            val sorted = finalizeExplorePlaces(explorePlaces, EXPLORE_HOTEL_LIMIT)
            loadPhotosForPlaces(client, places.filter { p -> sorted.any { it.placeId == p.id } }, photoMap, isActive)
            onDone(sorted)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DestinationExploreScreen(
    navController: NavController,
    viewModel: PlaceViewModel,
    placeId: String,
    placeName: String,
) {
    val context = LocalContext.current
    val decodedName = remember(placeName) {
        try { java.net.URLDecoder.decode(placeName, "UTF-8") } catch (e: Exception) { placeName }
    }
    val decodedPlaceId = remember(placeId) {
        try { java.net.URLDecoder.decode(placeId, "UTF-8") } catch (e: Exception) { placeId }
    }

    val apiKey = "AIzaSyBQFSEYxasR_AC2QiEl7BPmbKXAh167rX8"

    var heroPhoto by remember(decodedName) { mutableStateOf<Bitmap?>(null) }
    var heroLoading by remember(decodedName) { mutableStateOf(true) }
    var carouselPhotos by remember(decodedName) { mutableStateOf(listOf<Bitmap>()) }
    var carouselLoading by remember(decodedName) { mutableStateOf(true) }
    var foodList by remember(decodedName) { mutableStateOf(listOf<ExplorePlace>()) }
    val foodPhotoMap = remember(decodedName) { mutableStateMapOf<String, Bitmap>() }
    var foodLoading by remember(decodedName) { mutableStateOf(true) }
    var attractionList by remember(decodedName) { mutableStateOf(listOf<ExplorePlace>()) }
    val attractionPhotoMap = remember(decodedName) { mutableStateMapOf<String, Bitmap>() }
    var attractionLoading by remember(decodedName) { mutableStateOf(true) }
    var hotelList by remember(decodedName) { mutableStateOf(listOf<ExplorePlace>()) }
    val hotelPhotoMap = remember(decodedName) { mutableStateMapOf<String, Bitmap>() }
    var hotelLoading by remember(decodedName) { mutableStateOf(true) }
    var selectedTab by remember(decodedName) { mutableStateOf(ExploreTab.FOOD) }

    var loadGeneration by remember { mutableIntStateOf(0) }

    LaunchedEffect(decodedPlaceId, decodedName) {
        if (!Places.isInitialized()) Places.initialize(context, apiKey)

        val generation = loadGeneration + 1
        loadGeneration = generation
        val isActive = { loadGeneration == generation }

        heroPhoto = null
        heroLoading = true
        carouselPhotos = emptyList()
        carouselLoading = true
        foodList = emptyList()
        foodPhotoMap.clear()
        foodLoading = true
        attractionList = emptyList()
        attractionPhotoMap.clear()
        attractionLoading = true
        hotelList = emptyList()
        hotelPhotoMap.clear()
        hotelLoading = true
        selectedTab = ExploreTab.FOOD

        loadDestinationHeroPhoto(context, decodedPlaceId, decodedName, isActive) { bmp ->
            if (!isActive()) return@loadDestinationHeroPhoto
            heroPhoto = bmp
            heroLoading = false
        }

        loadDestinationCarouselPhotos(context, decodedPlaceId, decodedName, isActive) { photos ->
            if (!isActive()) return@loadDestinationCarouselPhotos
            carouselPhotos = photos
            carouselLoading = false
        }

        loadDestinationRestaurants(context, decodedName, decodedPlaceId, foodPhotoMap, isActive) { list ->
            if (!isActive()) return@loadDestinationRestaurants
            foodList = list
            foodLoading = false
        }

        loadDestinationAttractions(context, decodedName, decodedPlaceId, attractionPhotoMap, isActive) { list ->
            if (!isActive()) return@loadDestinationAttractions
            attractionList = list
            attractionLoading = false
        }

        loadDestinationHotels(context, decodedName, decodedPlaceId, hotelPhotoMap, isActive) { list ->
            if (!isActive()) return@loadDestinationHotels
            hotelList = list
            hotelLoading = false
        }
    }

    val currentList = when (selectedTab) {
        ExploreTab.FOOD -> foodList
        ExploreTab.ATTRACTIONS -> attractionList
        ExploreTab.HOTELS -> hotelList
    }
    val currentPhotoMap = when (selectedTab) {
        ExploreTab.FOOD -> foodPhotoMap
        ExploreTab.ATTRACTIONS -> attractionPhotoMap
        ExploreTab.HOTELS -> hotelPhotoMap
    }
    val currentLoading = when (selectedTab) {
        ExploreTab.FOOD -> foodLoading
        ExploreTab.ATTRACTIONS -> attractionLoading
        ExploreTab.HOTELS -> hotelLoading
    }

    val pagerState = rememberPagerState(
        pageCount = { carouselPhotos.size.coerceAtLeast(1) },
        initialPage = 0,
    )

    LaunchedEffect(carouselPhotos.size, decodedName) {
        if (carouselPhotos.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4500)
            val next = (pagerState.currentPage + 1) % carouselPhotos.size
            pagerState.animateScrollToPage(next, animationSpec = tween(durationMillis = 750))
        }
    }

    val pageBg = Color(0xFF121820)

    Box(
        Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // ── Hero card: destination photo + location pin + carousel overlay
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    when {
                        heroPhoto != null -> {
                            androidx.compose.foundation.Image(
                                bitmap = heroPhoto!!.asImageBitmap(),
                                contentDescription = decodedName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        heroLoading -> {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1F2A36)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF5AA8FF),
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        else -> {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1F2A36), Color(0xFF121820))
                                        )
                                    )
                            )
                        }
                    }

                    // Dark gradient overlay at bottom for carousel visibility
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.7f)),
                                    startY = 60f
                                )
                            )
                    )

                    // Back button
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { navController.popBackStack() }
                            .align(Alignment.TopStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Location pin + name
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 64.dp, top = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            decodedName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Carousel — destination & attraction photos (Places API, same as detail page)
                    if (carouselLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (carouselPhotos.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .align(Alignment.BottomCenter),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            pageSpacing = 10.dp,
                        ) { page ->
                            val bmp = carouselPhotos[page]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Page dots
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            repeat(carouselPhotos.size) { idx ->
                                val selected = pagerState.currentPage == idx
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 8.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }
            }

            // ── Recommendations
            item {
                Text(
                    "Recommendations",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExploreCapsuleTab(
                        label = "Foods & Drinks",
                        icon = Icons.Default.Restaurant,
                        selected = selectedTab == ExploreTab.FOOD,
                        onClick = { selectedTab = ExploreTab.FOOD }
                    )
                    ExploreCapsuleTab(
                        label = "Tourists Attractions",
                        icon = Icons.Default.Attractions,
                        selected = selectedTab == ExploreTab.ATTRACTIONS,
                        onClick = { selectedTab = ExploreTab.ATTRACTIONS }
                    )
                    ExploreCapsuleTab(
                        label = "Hotels",
                        icon = Icons.Default.Hotel,
                        selected = selectedTab == ExploreTab.HOTELS,
                        onClick = { selectedTab = ExploreTab.HOTELS }
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Places grid (2-column)
            if (currentLoading && currentList.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF5AA8FF))
                    }
                }
            } else if (currentList.isEmpty()) {
                item {
                    val emptyMessage = when (selectedTab) {
                        ExploreTab.FOOD ->
                            "No restaurants found for $decodedName.\nTry selecting a destination from the suggestions list."
                        ExploreTab.ATTRACTIONS ->
                            "No tourist attractions found for $decodedName.\nTry selecting a destination from the suggestions list."
                        ExploreTab.HOTELS ->
                            "No hotels found for $decodedName.\nTry selecting a destination from the suggestions list."
                    }
                    Text(
                        emptyMessage,
                        color = Color(0xFF78909C),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                val rows = currentList.chunked(2)
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { place ->
                            ExplorePlaceCard(
                                place = place,
                                photo = currentPhotoMap[place.placeId],
                                showOpenBadge = selectedTab == ExploreTab.FOOD,
                                modifier = Modifier.weight(1f),
                                onClick = { openExplorePlaceOnDetail(navController, viewModel, place) },
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreCapsuleTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) Color(0xFF5AA8FF).copy(alpha = 0.18f) else Color(0xFF1F2A36)
    val border = if (selected) Color(0xFF5AA8FF) else Color(0xFF3D4A57)
    val textColor = if (selected) Color(0xFF5AA8FF) else Color(0xFFB0BEC5)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
        Text(label, color = textColor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun ExplorePlaceCard(
    place: ExplorePlace,
    photo: Bitmap?,
    showOpenBadge: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val cardBg = Color(0xFF1F2A36)
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (photo != null) {
                    androidx.compose.foundation.Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = place.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color(0xFF2B3A4A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF546E7A), modifier = Modifier.size(32.dp))
                    }
                }
                // Open/Closed badge (food & drinks only)
                if (showOpenBadge) place.isOpen?.let { open ->
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (open) Color(0xFF1B5E20).copy(alpha = 0.9f) else Color(0xFF7F0000).copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (open) "Open" else "Closed",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(
                    place.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(13.dp))
                    Text(
                        place.rating?.let { "%.1f".format(it) } ?: "–",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                    place.reviewCount?.let {
                        Text(
                            "(${if (it >= 1000) "%.1fK".format(it / 1000.0) else it.toString()})",
                            color = Color(0xFF90A4AE),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
