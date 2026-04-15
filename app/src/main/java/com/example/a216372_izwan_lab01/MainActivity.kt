package com.example.a216372_izwan_lab01

import android.os.Bundle
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Request location permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )

        enableEdgeToEdge()

        setContent {
            A216372_IZWAN_Lab01Theme {
                HomeScreen()
            }
        }
    }
}

data class PlaceUI(
    val name: String,
    val address: String,
    val latLng: LatLng,
    val rating: Double?,
    val distanceKm: Double
)
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
fun HomeScreen() {

    var cardHeightPx by remember { mutableStateOf<Int>(0) }
    val density = LocalDensity.current
    val cardHeightDp = with(density) { cardHeightPx.toDp() }

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var placesList by remember { mutableStateOf(listOf<PlaceUI>()) }
    var sheetExpanded by remember { mutableStateOf(false) }

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
                    .background(Color(0xFF1F1F1F))
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
                    colors = CardDefaults.cardColors(Color(0xFF1F1F1F))
                ) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .padding(bottom = 60.dp) // 🔥 prevent overlap
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
                            FunctionItem(Icons.Default.Home, "Booking")
                            FunctionItem(Icons.Default.Place, "Satellite")
                            FunctionItem(Icons.Default.Star, "Drive")
                            FunctionItem(Icons.Default.Info, "Weather")
                            FunctionItem(Icons.Default.Favorite, "Fav")
                        }

                        Spacer(Modifier.height(16.dp))

                        CommuteCard()

                        if (sheetExpanded) {
                            Spacer(Modifier.height(16.dp))
                            ExpandedContent()
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
                BottomNavItem(Icons.Default.Home, "Travel", true)
                BottomNavItem(Icons.AutoMirrored.Filled.List, "Explore", false)
                BottomNavItem(Icons.Default.Person, "Me", false)
            }
        }

        if (isSearchOpen) {

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
                                                Place.Field.RATING,
                                                Place.Field.FORMATTED_ADDRESS
                                            )
                                        ).build()

                                    placesClient.fetchPlace(placeRequest)
                                        .addOnSuccessListener { placeResponse ->

                                            val place = placeResponse.place
                                            val latLng = LatLng(0.0, 0.0)

                                            val distance = userLocation?.let {
                                                calculateDistance(
                                                    it.latitude, it.longitude,
                                                    latLng.latitude, latLng.longitude
                                                )
                                            } ?: 0.0

                                            tempList.add(
                                                PlaceUI(
                                                    name = prediction.getPrimaryText(null).toString(),
                                                    address = place.formattedAddress ?: "",
                                                    latLng = latLng,
                                                    rating = place.rating,
                                                    distanceKm = distance
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

                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                Image(
                                    painterResource(id = R.drawable.emptystate),
                                    null,
                                    modifier = Modifier.size(120.dp)
                                )

                                Spacer(Modifier.height(12.dp))

                                Text("Search for places", color = Color.Gray)
                            }
                        }
                    }

                    if (placesList.isNotEmpty()) {

                        LazyColumn {

                            items(placesList) { place ->

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable {
                                            cameraPositionState.move(
                                                CameraUpdateFactory.newLatLngZoom(place.latLng, 15f)
                                            )
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
            color = Color.LightGray
        )
    }
}

@Composable
fun FunctionItem(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
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

        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
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
fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean) {

    val color = if (selected) Color(0xFF4D7CFF) else Color.Gray

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

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
fun ExpandedContent() {

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
                modifier = Modifier.height(94.dp)
            )
        }
    }
}

@Composable
fun RecommendationCard(
    title: String,
    image: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { },
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