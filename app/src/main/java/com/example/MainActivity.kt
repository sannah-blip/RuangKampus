package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Booking
import com.example.data.Space
import com.example.data.User
import com.example.ui.MainViewModel
import com.example.ui.OccupancyTrend
import com.example.ui.AuthScreen
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                if (currentUser == null) {
                    AuthScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        MainScreenContent(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val selectedDateString by viewModel.selectedDateString.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val pdfState by viewModel.pdfGenerationState.collectAsStateWithLifecycle()

    val filteredSpaces by viewModel.filteredSpaces.collectAsStateWithLifecycle()
    val bookings by viewModel.allBookings.collectAsStateWithLifecycle()
    val trends by viewModel.weeklyOccupancyTrends.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Booking Dashboard, 1: Kalender, 2: Laporan & Analitik
    var showLogsDrawer by remember { mutableStateOf(false) }
    var spaceToBook by remember { mutableStateOf<Space?>(null) }
    var showAddSpaceDialog by remember { mutableStateOf(false) }
    var spaceToDelete by remember { mutableStateOf<Space?>(null) }

    // Live breathing transition for IoT sync indicator
    val infiniteTransition = rememberInfiniteTransition(label = "SyncIndicator")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // --- HEADER BLOCK (Sleek Theme Layout) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "RuangKampus",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = currentUser?.let { "${it.fullName} • ${it.role}" } ?: "Real-Time Resource Sync",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Sync Indicator and Logout Actions Group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Live Sync Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .clickable { showLogsDrawer = !showLogsDrawer }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Color(0xFF10B981).copy(
                                                alpha = if (isSyncing) alphaAnim else 1.0f
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showLogsDrawer) Icons.Default.Close else Icons.Default.Analytics,
                                    contentDescription = "Logs",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Dynamic Theme Toggle Button
                            val isDarkThemeActive by viewModel.isDarkTheme.collectAsStateWithLifecycle()
                            IconButton(
                                onClick = { viewModel.toggleTheme() },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .testTag("dashboard_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkThemeActive) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Ganti Tema",
                                    tint = if (isDarkThemeActive) Color.Yellow else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Soft-Red Logout action button
                            IconButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Red.copy(alpha = 0.07f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Keluar",
                                    tint = Color.Red,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- NOTIFIKASI CENTER ALERTS BANNER ---
            AnimatedVisibility(
                visible = notifications.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                if (notifications.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Peringatan",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = notifications.first(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(onClick = { viewModel.dismissNotification(0) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- REAL-TIME IOT SENSOR LOGS DRAWER ---
            AnimatedVisibility(visible = showLogsDrawer) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Logs",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE CLOUD & IOT FIELD SOCKET SENSORS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Text(
                                text = "Live Update Ticker",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF475569))

                        LazyColumn(
                            modifier = Modifier.height(100.dp),
                            reverseLayout = false
                        ) {
                            if (syncLogs.isEmpty()) {
                                item {
                                    Text(
                                        text = "Menunggu data transmisi real-time API...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(syncLogs) { log ->
                                    Text(
                                        text = log,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (log.contains("API")) Color(0xFF34D399) else Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- TAB NAVIGATION BAR ---
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Ketersediaan Space", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = "Dashboard", modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Kalender & Jadwal", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = "Kalender", modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Analisis & PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.Analytics, contentDescription = "Report", modifier = Modifier.size(20.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- MAIN INTERACTIVE VIEW ACCORDING TO SELECTED TAB ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> BookingDashboardView(
                        spaces = filteredSpaces,
                        currentRole = currentRole,
                        searchQuery = searchQuery,
                        filterType = filterType,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterTypeChange = { viewModel.setFilterType(it) },
                        onBookClick = { spaceToBook = it },
                        onMaintenanceToggle = { space, state -> viewModel.updateMaintenanceStatus(space, state) },
                        onDeleteSpace = { spaceToDelete = it },
                        onAddSpaceClick = { showAddSpaceDialog = true }
                    )
                    1 -> CalendarView(
                        selectedDate = selectedDateString,
                        onDateSelected = { viewModel.setSelectedDate(it) },
                        bookings = bookings,
                        currentRole = currentRole,
                        onCancelBooking = { viewModel.cancelBooking(it) }
                    )
                    2 -> ReportsAndAnalyticsView(
                        trends = trends,
                        bookings = bookings,
                        pdfState = pdfState,
                        onPrintPdfClick = { viewModel.generatePdfReport(context) },
                        onClearPdfState = { viewModel.clearPdfState() }
                    )
                }
            }
        }

        // CUSTOM MODAL WINDOWS FOR FORM INJECTORS
        spaceToBook?.let { space ->
            BookingSelectionDialog(
                space = space,
                currentRole = currentRole,
                currentUser = currentUser,
                onDismiss = { spaceToBook = null },
                onConfirm = { name, start, end ->
                    viewModel.createBooking(space, name, start, end) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (success) {
                            spaceToBook = null
                        }
                    }
                }
            )
        }

        if (showAddSpaceDialog) {
            AddSpaceDialog(
                onDismiss = { showAddSpaceDialog = false },
                onConfirm = { name, type, seats, loc, fac, priority ->
                    viewModel.addNewSpace(name, type, seats, loc, fac, priority)
                    showAddSpaceDialog = false
                    Toast.makeText(context, "$type berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                }
            )
        }

        spaceToDelete?.let { space ->
            AlertDialog(
                onDismissRequest = { spaceToDelete = null },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfirmasi Hapus", fontWeight = FontWeight.Bold)
                    }
                },
                text = { Text("Apakah Anda yakin ingin menghapus '${space.name}' (${space.type}) dari server database lokal SQLite secara permanen?") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        onClick = {
                            viewModel.deleteSpace(space)
                            spaceToDelete = null
                        }
                    ) {
                        Text("Hapus Permanen", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { spaceToDelete = null }) {
                        Text("Batal")
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

// ==========================================
// TAB 0: BOOKING & LIVE AVAILABILITY SCREEN
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingDashboardView(
    spaces: List<Space>,
    currentRole: String,
    searchQuery: String,
    filterType: String,
    onSearchChange: (String) -> Unit,
    onFilterTypeChange: (String) -> Unit,
    onBookClick: (Space) -> Unit,
    onMaintenanceToggle: (Space, Boolean) -> Unit,
    onDeleteSpace: (Space) -> Unit,
    onAddSpaceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        
        // Search & Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari ruangan atau lantai...", fontSize = 13.sp) },
                prefix = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_field"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            if (currentRole == "ADMIN") {
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = onAddSpaceClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("add_space_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Space", tint = Color.White)
                }
            }
        }

        // Filter Category Chips (All, Rooms, Desks)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "Semua Tipe", "ROOM" to "Ruangan 🚪", "DESK" to "Meja Belajar ✍️").forEach { (typeKey, label) ->
                val isSelected = filterType == typeKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterTypeChange(typeKey) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Room lists scroll
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (spaces.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tidak ada ruangan/meja ditemukan.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(spaces, key = { it.id }) { space ->
                    val isFull = space.availableSeats == 0
                    val isPriority = space.priorityOnly
                    
                    val cardBorder = when {
                        space.isUnderMaintenance -> BorderStroke(1.5.dp, Color.Red.copy(alpha = 0.6f))
                        isPriority -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f))
                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("space_card_${space.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (space.isUnderMaintenance) Color.Red.copy(alpha = 0.02f) 
                                             else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = cardBorder,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Row 1: Left-hand Side Icon and Info Details (Sleek layout from Mockup)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Left icon block
                                val iconBoxBg = if (space.type == "ROOM") {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                                }
                                val iconBoxTint = if (space.type == "ROOM") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                }
                                val iconVector = if (space.type == "ROOM") {
                                    Icons.Default.MeetingRoom
                                } else {
                                    Icons.Default.Computer
                                }
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(iconBoxBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = iconBoxTint,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Info Details Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = space.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (space.type == "ROOM") "RUANG" else "MEJA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = space.location,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                // Status Badge
                                when {
                                    space.isUnderMaintenance -> {
                                        StatusBadge(text = "PERBAIKAN", containerColor = Color.Red, contentColor = Color.White)
                                    }
                                    isPriority -> {
                                        StatusBadge(text = "PRIORITAS DOSEN", containerColor = MaterialTheme.colorScheme.tertiary, contentColor = Color.White)
                                    }
                                    isFull -> {
                                        StatusBadge(text = "PENUH", containerColor = Color.Gray, contentColor = Color.White)
                                    }
                                    else -> {
                                        StatusBadge(text = "TERSEDIA (${space.availableSeats}/${space.totalSeats})", containerColor = Color(0xFF10B981), contentColor = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val progress = if (space.totalSeats > 0) {
                                (space.totalSeats - space.availableSeats).toFloat() / space.totalSeats
                            } else 0f
                            
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Kepadatan Kursi: ${space.totalSeats - space.availableSeats} terisi dari ${space.totalSeats}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}% Okupansi",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progress > 0.8f) Color.Red else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (progress > 0.8f) Color.Red else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                space.facilities.split(",").forEach { facility ->
                                    val cleaned = facility.trim()
                                    if (cleaned.isNotEmpty()) {
                                        Text(
                                            text = cleaned,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentRole == "ADMIN") {
                                    Button(
                                        onClick = { onMaintenanceToggle(space, !space.isUnderMaintenance) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (space.isUnderMaintenance) Color(0xFF10B981) else Color.Red.copy(alpha = 0.1f),
                                            contentColor = if (space.isUnderMaintenance) Color.White else Color.Red
                                        ),
                                        modifier = Modifier.testTag("maintenance_toggle_${space.id}"),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (space.isUnderMaintenance) "Buka Kunci" else "Set Perbaikan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Spacer(modifier = Modifier.width(6.dp))
                                    
                                    IconButton(
                                        onClick = { onDeleteSpace(space) },
                                        modifier = Modifier.testTag("delete_space_${space.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                } else {
                                    val canBook = !space.isUnderMaintenance
                                    val btnLabel = when {
                                        space.isUnderMaintenance -> "Pemeliharaan"
                                        currentRole == "DOSEN" -> "Pesan Prioritas ⭐"
                                        else -> "Pesan Tempat 📅"
                                    }

                                    Button(
                                        onClick = { onBookClick(space) },
                                        enabled = canBook,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("book_button_${space.id}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentRole == "DOSEN") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (currentRole == "DOSEN") Icons.Default.Star else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(btnLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
fun StatusBadge(text: String, containerColor: Color, contentColor: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// ==========================================
// TAB 1: ACADEMIC CALENDAR & RESERVATION LISTS
// ==========================================
@Composable
fun CalendarView(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    bookings: List<Booking>,
    currentRole: String,
    onCancelBooking: (Booking) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val datesSuite = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -2) // Historical slots
        for (i in 0..11) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val bookingsForDate = bookings.filter { it.dateString == selectedDate }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "PILIH TANGGAL KALENDER ACARA:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(datesSuite) { date ->
                val dateStrInCell = sdf.format(date)
                val isSelected = dateStrInCell == selectedDate
                
                val calendarInstance = Calendar.getInstance().apply { time = date }
                val dayOfWeek = calendarInstance.get(Calendar.DAY_OF_WEEK)
                val dayOfMonth = calendarInstance.get(Calendar.DAY_OF_MONTH)
                val dayName = when(dayOfWeek) {
                    Calendar.MONDAY -> "Sen"
                    Calendar.TUESDAY -> "Sel"
                    Calendar.WEDNESDAY -> "Rab"
                    Calendar.THURSDAY -> "Kam"
                    Calendar.FRIDAY -> "Jum"
                    Calendar.SATURDAY -> "Sab"
                    Calendar.SUNDAY -> "Min"
                    else -> "Day"
                }

                val hasBookingsScheduled = bookings.any { it.dateString == dateStrInCell && it.status == "ACTIVE" }

                Card(
                    modifier = Modifier
                        .width(60.dp)
                        .clickable { onDateSelected(dateStrInCell) }
                        .testTag("calendar_day_$dateStrInCell"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                                         else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayOfMonth.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (hasBookingsScheduled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSelected) Color.White else MaterialTheme.colorScheme.tertiary)
                            )
                        }
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reservasi Terdaftar ($selectedDate)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${bookingsForDate.size} Sesi",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            if (bookingsForDate.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Belum ada agenda pemesanan untuk tanggal ini.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else {
                items(bookingsForDate) { booking ->
                    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val startTimeFormatted = sdfTime.format(Date(booking.startTime))
                    val endTimeFormatted = sdfTime.format(Date(booking.endTime))
                    
                    val cardBg = when (booking.status) {
                        "CANCELLED" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                        "COMPLETED" -> Color.Gray.copy(alpha = 0.05f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_card_${booking.id}"),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = if (booking.isPriorityOverride) BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary) 
                                 else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (booking.spaceType == "ROOM") Icons.Default.MeetingRoom else Icons.Default.Computer,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = booking.spaceName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (booking.status == "CANCELLED") Color.Gray else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Dipesan Oleh: ${booking.userName} (${booking.userRole})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Waktu Sesi: $startTimeFormatted - $endTimeFormatted",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                if (booking.isPriorityOverride) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚠️ Diambil Alih Prioritas Dosen",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                when (booking.status) {
                                    "CANCELLED" -> {
                                        Text("DIBATALKAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                    }
                                    "COMPLETED" -> {
                                        Text("SELESAI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                    else -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("AKTIF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { onCancelBooking(booking) },
                                                modifier = Modifier.size(24.dp).testTag("cancel_booking_${booking.id}")
                                            ) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red.copy(alpha = 0.7f))
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
}

// ==========================================
// TAB 2: REPORTS & OCCUPANCY ANALYTICS GRID
// ==========================================
@Composable
fun ReportsAndAnalyticsView(
    trends: List<OccupancyTrend>,
    bookings: List<Booking>,
    pdfState: String?,
    onPrintPdfClick: () -> Unit,
    onClearPdfState: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Report",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Laporan Okupansi PDF",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ekspor berkas rekapitulasi data pemakaian mingguan dan tingkat keramaian sarana ke bentuk PDF resmi untuk arsip sarpas kampus.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onPrintPdfClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_generation_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Laporan PDF Resmi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                pdfState?.let { status ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = status,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onClearPdfState, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GRAFIK OKUPANSI RUANGAN MINGGUAN (%)",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Sleek Dark Slate-900 contrast card
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rata-Rata Tren Kampus",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Okupansi memuncak di Hari Senin-Rabu jam kuliah",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = "Minggu Ini",
                        color = Color(0xFF60A5FA), // Accent Sleek Sky Blue
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val primaryColor = Color(0xFF60A5FA)      // bright sleek blue
                val barColorAccent = Color(0xFFC084FC)    // lavender purple alert
                val textGridColor = Color.White.copy(alpha = 0.15f)
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barWidth = 32.dp.toPx()
                    val totalBarsCount = trends.size
                    
                    val levels = listOf(100, 75, 50, 25, 0)
                    levels.forEach { level ->
                        val yPos = canvasHeight - ((level.toFloat() / 100) * (canvasHeight - 30.dp.toPx())) - 25.dp.toPx()
                        drawLine(
                            color = textGridColor.copy(alpha = 0.12f),
                            start = Offset(0f, yPos),
                            end = Offset(canvasWidth, yPos),
                            strokeWidth = 1f
                        )
                    }

                    val spacingBetweenBars = (canvasWidth - (barWidth * totalBarsCount)) / (totalBarsCount + 1)
                    
                    trends.forEachIndexed { index, trend ->
                        val barHeightFactor = trend.occupancyPercent.toFloat() / 100
                        val barHeight = barHeightFactor * (canvasHeight - 60.dp.toPx())
                        val xPos = spacingBetweenBars + (index * (barWidth + spacingBetweenBars))
                        val yPos = canvasHeight - barHeight - 25.dp.toPx()

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = if (trend.occupancyPercent > 70) {
                                    listOf(barColorAccent, barColorAccent.copy(alpha = 0.6f))
                                } else {
                                    listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                                }
                            ),
                            topLeft = Offset(xPos, yPos),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        drawContextText(
                            text = "${trend.occupancyPercent}%",
                            x = xPos + (barWidth / 2f),
                            y = yPos - 6.dp.toPx(),
                            drawContext = this,
                            color = primaryColor,
                            fontSize = 10.sp.toPx()
                        )

                        drawContextText(
                            text = trend.dayLabel,
                            x = xPos + (barWidth / 2f),
                            y = canvasHeight - 6.dp.toPx(),
                            drawContext = this,
                            color = textGridColor,
                            fontSize = 11.sp.toPx()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "RIWAYAT TRANSAKSI TERAKHIR",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val completedBookings = bookings.filter { it.status == "COMPLETED" }.take(6)
                if (completedBookings.isEmpty()) {
                    Text(
                        text = "Belum ada riwayat aktivitas pemakaian tercatat.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    completedBookings.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = item.spaceName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "Pengguna: ${item.userName} (${item.userRole})", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Text(text = "Selesai", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        if (idx < completedBookings.size - 1) {
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        }
                    }
                }
            }
        }
    }
}

private fun drawContextText(
    text: String,
    x: Float,
    y: Float,
    drawContext: androidx.compose.ui.graphics.drawscope.DrawScope,
    color: Color,
    fontSize: Float
) {
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.rgb(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        this.textSize = fontSize
        this.textAlign = android.graphics.Paint.Align.CENTER
        this.isAntiAlias = true
    }
    drawContext.drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

// ==========================================
// FORM DIALOG 1: SPACE RESERVATION MODAL
// ==========================================
@Composable
fun BookingSelectionDialog(
    space: Space,
    currentRole: String,
    currentUser: User?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var userName by remember { mutableStateOf(currentUser?.fullName ?: "") }
    
    val startTimes = listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00")
    val endTimes = listOf("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00")

    var selectedStartIdx by remember { mutableStateOf(0) }
    var selectedEndIdx by remember { mutableStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Konfirmasi Pemesanan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${space.name} (${space.location})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                if (currentRole == "DOSEN") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Prioritas Dosen Aktif! Pemesanan Anda berhak mengambil alih slot mahasiswa jika terjadi konflik jadwal.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Nama Pemesan / Kode Identitas") },
                    placeholder = { Text("Andi / DSN-450") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TENTUKAN DURASI WAKTU SESI:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mulai Jam:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedStartIdx = (selectedStartIdx + 1) % startTimes.size
                                    if (selectedStartIdx >= selectedEndIdx) {
                                        selectedEndIdx = (selectedStartIdx + 1) % endTimes.size
                                    }
                                }
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(startTimes[selectedStartIdx], fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Selesai Jam:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedEndIdx = (selectedEndIdx + 1) % endTimes.size
                                    if (selectedEndIdx <= selectedStartIdx) {
                                        selectedEndIdx = (selectedStartIdx + 1) % endTimes.size
                                    }
                                }
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(endTimes[selectedEndIdx], fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("booking_dismiss")) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(userName, startTimes[selectedStartIdx], endTimes[selectedEndIdx])
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentRole == "DOSEN") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("booking_confirm"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pesan Sekarang")
                    }
                }
            }
        }
    }
}

// ==========================================
// FORM DIALOG 2: ADD NEW SPACE SPECIFICALLY
// ==========================================
@Composable
fun AddSpaceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("ROOM") } 
    var totalSeatsStr by remember { mutableStateOf("30") }
    var location by remember { mutableStateOf("Gedung Kuliah Terpadu") }
    var facilities by remember { mutableStateOf("AC, WiFi, Proyektor") }
    var priorityOnly by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tambah Master Ruang Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Ruangan / Meja") },
                    placeholder = { Text("Lab Multimedia RT.203") },
                    modifier = Modifier.fillMaxWidth().testTag("add_space_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Tipe Fasilitas:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { type = "ROOM" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "ROOM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            contentColor = if (type == "ROOM") Color.White else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Ruangan (🚪)")
                    }
                    Button(
                        onClick = { type = "DESK" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "DESK") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            contentColor = if (type == "DESK") Color.White else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Meja (✍️)")
                    }
                }

                OutlinedTextField(
                    value = totalSeatsStr,
                    onValueChange = { totalSeatsStr = it },
                    label = { Text("Total Kursi / Kapasitas") },
                    modifier = Modifier.fillMaxWidth().testTag("add_space_capacity_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi (Gedung & Lantai)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_space_location_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = facilities,
                    onValueChange = { facilities = it },
                    label = { Text("Fasilitas (Dipisah Koma)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_space_facilities_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { priorityOnly = !priorityOnly }
                ) {
                    Checkbox(checked = priorityOnly, onCheckedChange = { priorityOnly = it })
                    Text("Batasi Akses Khusus Dosen", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("add_space_dismiss")) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val seats = totalSeatsStr.toIntOrNull() ?: 1
                            onConfirm(name, type, seats, location, facilities, priorityOnly)
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("add_space_confirm"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
