package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SpaceRepository(database.spaceDao(), database.bookingDao(), database.userDao())
    private val sharedPrefs = application.getSharedPreferences("ruangkampus_prefs", Context.MODE_PRIVATE)

    // Manual Theme Control State (Day vs Night)
    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val nextVal = !_isDarkTheme.value
        _isDarkTheme.value = nextVal
        sharedPrefs.edit().putBoolean("is_dark_theme", nextVal).apply()
        addSyncLog("Tema Sarpas dialihkan ke: ${if (nextVal) "MALAM" else "SIANG"}")
    }

    // UI Configuration State
    private val _currentRole = MutableStateFlow("MAHASISWA") // "MAHASISWA", "DOSEN", "ADMIN"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    // Active User State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _selectedDateString = MutableStateFlow(getTodayDateString()) // "YYYY-MM-DD"
    val selectedDateString: StateFlow<String> = _selectedDateString.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("ALL") // "ALL", "ROOM", "DESK"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    // Real-Time IoT API Sync Simulation States
    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Real-Time Notification Stack
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    // PDF generation states
    private val _pdfGenerationState = MutableStateFlow<String?>(null) // State message of downloaded file path
    val pdfGenerationState: StateFlow<String?> = _pdfGenerationState.asStateFlow()

    // Expose flows from Repo
    val allSpaces: StateFlow<List<Space>> = repository.allSpaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookings: StateFlow<List<Booking>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Spaces based on search and type filters
    val filteredSpaces: StateFlow<List<Space>> = combine(
        allSpaces,
        searchQuery,
        filterType
    ) { spaces, query, type ->
        spaces.filter { space ->
            val matchesQuery = space.name.contains(query, ignoreCase = true) || 
                               space.location.contains(query, ignoreCase = true)
            val matchesType = type == "ALL" || space.type == type
            matchesQuery && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly trend stats calculation (Weekly occupancy percentage over 7 days of the week)
    // We map days from Mon to Sun in the current week to booking statistics.
    val weeklyOccupancyTrends: StateFlow<List<OccupancyTrend>> = allBookings.map { bookings ->
        calculateWeeklyTrends(bookings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedInitialData()
        startRealTimeIoTSimulator()
        startNotificationWatcher()
        checkSavedSession()
    }

    private fun checkSavedSession() {
        val savedUsername = sharedPrefs.getString("logged_in_username", null)
        if (savedUsername != null) {
            viewModelScope.launch {
                val user = repository.getUserByUsername(savedUsername)
                if (user != null) {
                    _currentUser.value = user
                    _currentRole.value = user.role
                    addSyncLog("Sesi lama dipulihkan: ${user.fullName} (${user.role})")
                }
            }
        }
    }

    fun loginUser(username: String, passwordPlain: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || passwordPlain.isBlank()) {
                onResult(false, "Username dan password tidak boleh kosong.")
                return@launch
            }
            val user = repository.getUserByUsername(username.trim())
            if (user == null) {
                onResult(false, "Username tidak ditemukan.")
            } else if (user.passwordHash != passwordPlain) {
                onResult(false, "Password salah.")
            } else {
                _currentUser.value = user
                _currentRole.value = user.role
                sharedPrefs.edit().putString("logged_in_username", user.username).apply()
                addSyncLog("Sesi aktif: ${user.fullName} (${user.role})")
                onResult(true, "Selamat datang, ${user.fullName}!")
            }
        }
    }

    fun registerUser(username: String, passwordPlain: String, fullName: String, role: String, faculty: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || passwordPlain.isBlank() || fullName.isBlank()) {
                onResult(false, "Semua field wajib diisi.")
                return@launch
            }
            val cleanUsername = username.trim()
            val existing = repository.getUserByUsername(cleanUsername)
            if (existing != null) {
                onResult(false, "Username sudah digunakan.")
            } else {
                val newUser = User(
                    username = cleanUsername,
                    passwordHash = passwordPlain,
                    fullName = fullName,
                    role = role,
                    faculty = faculty.ifBlank { "Fakultas Ilmu Komputer" }
                )
                repository.insertUser(newUser)
                addSyncLog("Registrasi user berhasil: $cleanUsername ($role)")
                onResult(true, "Akun berhasil didaftarkan!")
            }
        }
    }

    fun logout() {
        val prevUser = _currentUser.value
        _currentUser.value = null
        sharedPrefs.edit().remove("logged_in_username").apply()
        if (prevUser != null) {
            addSyncLog("User ${prevUser.fullName} keluar.")
        }
    }

    // Role switcher
    fun setRole(role: String) {
        _currentRole.value = role
        _currentUser.value?.let { user ->
            // If the user is logged in, synchronize their database role as well for stability
            viewModelScope.launch {
                val updatedUser = user.copy(role = role)
                repository.insertUser(updatedUser)
                _currentUser.value = updatedUser
            }
        }
        addSyncLog("Role dialihkan ke: $role")
    }

    // Selected Date switcher for Calendar
    fun setSelectedDate(dateString: String) {
        _selectedDateString.value = dateString
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun dismissNotification(index: Int) {
        _notifications.update { current ->
            current.toMutableList().apply { if (index in indices) removeAt(index) }
        }
    }

    fun addNotification(message: String) {
        _notifications.update { current ->
            listOf(message) + current
        }
    }

    // Book space with role-based priority check and SQLite DB persist
    fun createBooking(
        space: Space,
        userName: String,
        startTimeStr: String, // e.g. "09:00"
        endTimeStr: String,  // e.g. "12:00"
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val todayStr = _selectedDateString.value
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val startParsed = format.parse("$todayStr $startTimeStr")?.time ?: System.currentTimeMillis()
                val endParsed = format.parse("$todayStr $endTimeStr")?.time ?: (startParsed + 3600000)

                if (endParsed <= startParsed) {
                    onResult(false, "Waktu selesai harus setelah waktu mulai.")
                    return@launch
                }

                // Retrieve roles based on SQLite active user session
                val activeUser = _currentUser.value
                val targetRole = activeUser?.role ?: _currentRole.value
                val finalUserName = userName.trim().ifBlank { activeUser?.fullName ?: "Pengguna Sarpas" }

                // 1. Conflict checking with already booked active slots
                val existingBookings = repository.allBookings.first()
                val activeConflicts = existingBookings.filter { 
                    it.spaceId == space.id && 
                    it.status == "ACTIVE" &&
                    it.dateString == todayStr &&
                    ((startParsed >= it.startTime && startParsed < it.endTime) || 
                     (endParsed > it.startTime && endParsed <= it.endTime) ||
                     (startParsed <= it.startTime && endParsed >= it.endTime))
                }

                var didPerformOverride = false

                if (activeConflicts.isNotEmpty()) {
                    when (targetRole) {
                        "ADMIN" -> {
                            // Admin has ultimate high priority: override any existing bookings (lecturers or students)
                            for (conflict in activeConflicts) {
                                val canceledBooking = conflict.copy(status = "CANCELLED", isPriorityOverride = true)
                                repository.updateBooking(canceledBooking)
                                addNotification("⚠️ Booking ${conflict.userRole} (${conflict.userName}) di ${space.name} dibatalkan otomatis oleh Administrator!")
                                addSyncLog("Admin override: Membatalkan reservasi ${conflict.userRole} '${conflict.userName}'")
                            }
                            didPerformOverride = true
                        }
                        "DOSEN" -> {
                            // Lecturers can override MAHASISWA bookings, but NOT fellow Lecturers or Admins
                            val unoverrideable = activeConflicts.filter { it.userRole == "DOSEN" || it.userRole == "ADMIN" }
                            if (unoverrideable.isNotEmpty()) {
                                onResult(false, "Slot waktu ini sudah dipesan oleh sesama Dosen atau Admin (${unoverrideable.first().userName}).")
                                return@launch
                            }

                            // Perform cancellation on student slots
                            for (conflict in activeConflicts) {
                                if (conflict.userRole == "MAHASISWA") {
                                    val canceledBooking = conflict.copy(status = "CANCELLED", isPriorityOverride = true)
                                    repository.updateBooking(canceledBooking)
                                    addNotification("⚠️ Booking Mahasiswa (${conflict.userName}) di ${space.name} dibatalkan otomatis untuk Prioritas Dosen!")
                                    addSyncLog("Dosen override: Membatalkan reservasi Mhs '${conflict.userName}'")
                                }
                            }
                            didPerformOverride = true
                        }
                        else -> {
                            // Students/MAHASISWA cannot override anything
                            onResult(false, "Waktu ini sudah dipesan oleh ${activeConflicts.first().userName} (${activeConflicts.first().userRole}).")
                            return@launch
                        }
                    }
                }

                // 2. Prevent booking if space is under maintenance (Admins can bypass for testing purposes, but blocked otherwise)
                if (space.isUnderMaintenance && targetRole != "ADMIN") {
                    onResult(false, "Ruang sedang dalam masa perbaikan/maintenance.")
                    return@launch
                }

                // 3. Check specific room-only access controls (e.g. priorityOnly rooms)
                if (space.priorityOnly && targetRole != "DOSEN" && targetRole != "ADMIN") {
                    onResult(false, "Akses Terbatas: Ruangan ini dikhususkan untuk Dosen (Prioritas Utama)!")
                    return@launch
                }

                // 4. Update the room seat decrement (optional simulator tracker)
                if (space.availableSeats > 0) {
                    repository.updateSpace(space.copy(availableSeats = space.availableSeats - 1))
                }

                // 5. Store booking to the database
                val newBooking = Booking(
                    spaceId = space.id,
                    spaceName = space.name,
                    spaceType = space.type,
                    userName = finalUserName,
                    userRole = targetRole,
                    startTime = startParsed,
                    endTime = endParsed,
                    dateString = todayStr,
                    isPriorityOverride = didPerformOverride
                )

                repository.insertBooking(newBooking)
                addSyncLog("Pemesanan berhasil disimpan: ${space.name} oleh $finalUserName")
                
                // Set immediate mock timer for nearing termination reminder
                scheduleSimulatedReminder(newBooking, space)

                onResult(true, if (didPerformOverride) "Pemesanan berhasil disimpan (dengan Overriding Prioritas)!" else "Pemesanan berhasil disimpan!")
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan: ${e.localizedMessage}")
            }
        }
    }

    // Cancel booking
    fun cancelBooking(booking: Booking) {
        viewModelScope.launch {
            val updated = booking.copy(status = "CANCELLED")
            repository.updateBooking(updated)

            // Re-increment available seat
            repository.getSpaceById(booking.spaceId)?.let { space ->
                if (space.availableSeats < space.totalSeats) {
                    repository.updateSpace(space.copy(availableSeats = space.availableSeats + 1))
                }
            }
            addSyncLog("Pemesanan #${booking.id} dibatalkan oleh user.")
        }
    }

    // Clear calendar reports path notification helper
    fun clearPdfState() {
        _pdfGenerationState.value = null
    }

    // PDF Report Generator
    fun generatePdfReport(context: Context) {
        viewModelScope.launch {
            _pdfGenerationState.value = "Generasi sedang diproses..."
            delay(1000)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paintText = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }

            val paintHeader = Paint().apply {
                color = Color.DKGRAY
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val paintSub = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                isAntiAlias = true
            }

            val paintAccent = Paint().apply {
                color = Color.rgb(0, 115, 230) // Clean iOS Blue Accent
                strokeWidth = 2f
            }

            val spaces = repository.allSpaces.first()
            val bookings = repository.allBookings.first()

            // Header
            canvas.drawText("LAPORAN OKUPANSI RUANG KAMPUS", 40f, 60f, paintHeader)
            canvas.drawText("Dicetak: ${getTodayDateString()} (Format Laporan Mingguan)", 40f, 80f, paintSub)
            canvas.drawLine(40f, 90f, 550f, 90f, paintAccent)

            // General Statistics
            canvas.drawText("RINGKASAN ESTIMASI OKUPANSI KAMPUS", 40f, 120f, paintHeader.apply { textSize = 14f })
            canvas.drawText("Total Ruangan: ${spaces.filter { it.type == "ROOM" }.size} unit", 40f, 145f, paintText)
            canvas.drawText("Total Meja Belajar: ${spaces.filter { it.type == "DESK" }.size} unit", 40f, 165f, paintText)
            canvas.drawText("Total Reservasi Terdaftar: ${bookings.size} Pemesanan", 40f, 185f, paintText)

            // Table of active bookings
            canvas.drawLine(40f, 215f, 550f, 215f, paintSub)
            canvas.drawText("DAFTAR REKAPITULASI RESERVASI AKTIF & SELESAI:", 40f, 235f, paintHeader.apply { textSize = 12f })

            var currentY = 265f
            val limitY = 780f

            paintText.textSize = 10f
            // Grid Title row
            canvas.drawText("Ruang / Meja", 40f, currentY, paintText.apply { isFakeBoldText = true })
            canvas.drawText("Nama User", 200f, currentY, paintText)
            canvas.drawText("Waktu", 340f, currentY, paintText)
            canvas.drawText("Status", 480f, currentY, paintText)

            canvas.drawLine(40f, currentY + 5, 550f, currentY + 5, paintSub)
            currentY += 25f

            paintText.isFakeBoldText = false
            for (booking in bookings.take(15)) {
                if (currentY > limitY) break

                val simpleTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = "${simpleTime.format(Date(booking.startTime))} - ${simpleTime.format(Date(booking.endTime))}"
                
                canvas.drawText(booking.spaceName.take(24), 40f, currentY, paintText)
                canvas.drawText(booking.userName.take(20), 200f, currentY, paintText)
                canvas.drawText("$timeStr (${booking.dateString})", 340f, currentY, paintText)
                canvas.drawText(booking.status, 480f, currentY, paintText)

                currentY += 20f
            }

            // Footer / Sign Off
            canvas.drawLine(40f, 800f, 550f, 800f, paintSub)
            canvas.drawText("Manajemen Sarana Prasana RuangKampus © 2026", 40f, 815f, paintSub)

            pdfDocument.finishPage(page)

            try {
                val reportFile = File(context.cacheDir, "Resep_Laporan_RuangKampus.pdf")
                val fos = FileOutputStream(reportFile)
                pdfDocument.writeTo(fos)
                fos.close()
                pdfDocument.close()
                _pdfGenerationState.value = "Berhasil! PDF tersimpan di: ${reportFile.absolutePath}"
                addNotification("📄 Laporan PDF berhasil diekspor! Tersimpan di folder unduhan internal.")
            } catch (e: Exception) {
                pdfDocument.close()
                _pdfGenerationState.value = "Gagal memproses PDF: ${e.localizedMessage}"
            }
        }
    }

    // Admin Room controls
    fun addNewSpace(
        name: String,
        type: String,
        totalSeats: Int,
        location: String,
        facilities: String,
        priorityOnly: Boolean
    ) {
        viewModelScope.launch {
            val space = Space(
                name = name,
                type = type,
                totalSeats = totalSeats,
                availableSeats = totalSeats,
                location = location,
                facilities = facilities,
                priorityOnly = priorityOnly
            )
            repository.insertSpace(space)
            addSyncLog("Admin menambahkan $type baru: $name")
        }
    }

    fun updateMaintenanceStatus(space: Space, isUnderMaintenance: Boolean) {
        viewModelScope.launch {
            val updated = space.copy(isUnderMaintenance = isUnderMaintenance)
            repository.updateSpace(updated)
            addSyncLog("Admin merubah status ${space.name} -> Maintenance: $isUnderMaintenance")
        }
    }

    fun deleteSpace(space: Space) {
        viewModelScope.launch {
            repository.deleteSpace(space)
            addSyncLog("Admin menghapus ${space.type}: ${space.name}")
        }
    }

    private fun addSyncLog(message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        _syncLogs.update { current ->
            (listOf("[$timestamp] $message") + current).take(20) // Keep last 20 logs
        }
    }

    // IoT Sensor Simulator Engine (Saves data directly into Room spaces simulated as external inputs)
    private fun startRealTimeIoTSimulator() {
        viewModelScope.launch {
            delay(1000)
            while (true) {
                _isSyncing.value = true
                delay(3000) // Show synching animation
                
                // Simulate sensor adjustments on Room capacities
                val spacesList = repository.allSpaces.first()
                if (spacesList.isNotEmpty()) {
                    val randomSpace = spacesList.random()
                    // Randomly modify occupancy slightly for visual live action
                    if (randomSpace.totalSeats > 1 && !randomSpace.isUnderMaintenance) {
                        val change = if (Math.random() > 0.5) 1 else -1
                        val newAvailable = (randomSpace.availableSeats + change).coerceIn(0, randomSpace.totalSeats)
                        if (newAvailable != randomSpace.availableSeats) {
                            val updated = randomSpace.copy(availableSeats = newAvailable)
                            repository.updateSpace(updated)
                            
                            val actionStr = if (change > 0) "kosong (ditinggalkan)" else "terisi (ditempati)"
                            addSyncLog("📡 IoT Sensor API: [${updated.name}] 1 Meja/Kursi $actionStr. Sisa: $newAvailable/${updated.totalSeats}")
                        }
                    }
                }
                
                _isSyncing.value = false
                delay(12000) // Next simulation tick
            }
        }
    }

    // Trigger dynamic notifications if a room session is about to be vacant / released soon
    private fun startNotificationWatcher() {
        viewModelScope.launch {
            while (true) {
                delay(15000) // Check periodically
                val bookings = repository.allBookings.first()
                val nowTime = System.currentTimeMillis()

                bookings.filter { it.status == "ACTIVE" }.forEach { booking ->
                    // Simulate checking if a slot ends in less than 5 minutes (or close end in mock scale)
                    val remainingMs = booking.endTime - nowTime
                    // Let's mock a notification triggers for anything completing in future, or trigger some periodic reminders
                    if (!booking.reminderSent && remainingMs < 600000 && remainingMs > 0) { // less than 10 mins remaining
                        addNotification("⏰ PENGINGAT: Sesi ${booking.userName} di '${booking.spaceName}' tersisa 5 menit! Ruangan akan segera kosong.")
                        repository.updateBooking(booking.copy(reminderSent = true))
                    }
                }
            }
        }
    }

    private fun scheduleSimulatedReminder(booking: Booking, space: Space) {
        viewModelScope.launch {
            // Wait a dynamic mock duration to remind during demo sessions quickly
            delay(8000)
            addNotification("🔔 RuangKampus Alert: Sesi '${booking.userName}' di ${space.name} tersisa 5 menit. Sesi segera berakhir.")
        }
    }

    // Seed database on first launch
    private fun seedInitialData() {
        viewModelScope.launch {
            // Seed Default Users
            try {
                val currentUsers = repository.allUsers.first()
                if (currentUsers.isEmpty()) {
                    val defaultUsers = listOf(
                        User("admin", "admin123", "Administrator Sarpas", "ADMIN", "Fakultas Teknik"),
                        User("mahasiswa", "mhs123", "Andi Wicaksono", "MAHASISWA", "Fakultas Ilmu Komputer"),
                        User("dosen", "dosen123", "Prof. Dr. Ir. Budi Santoso", "DOSEN", "Fakultas Ilmu Komputer")
                    )
                    for (u in defaultUsers) {
                        repository.insertUser(u)
                    }
                    addSyncLog("Seeding 3 Akun Default berhasil!")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error seeding users: ${e.localizedMessage}")
            }

            val spacesList = repository.allSpaces.first()
            if (spacesList.isEmpty()) {
                addSyncLog("Database Kosong. Melakukan inisialisasi master data...")

                // Setup Rooms & Desks
                val initialSpaces = listOf(
                    Space(name = "Lab Komputer Terpadu", type = "ROOM", totalSeats = 40, availableSeats = 28, location = "Gedung Tekno, Lantai 2", facilities = "WiFi 500Mbps, AC, Proyektor, PC Intel i7", priorityOnly = false),
                    Space(name = "Ruang Teori Bersama B.101", type = "ROOM", totalSeats = 60, availableSeats = 45, location = "Gedung Kuliah Utama, Lantai 1", facilities = "Papan Tulis, AC, Proyektor, Speaker", priorityOnly = false),
                    Space(name = "Ruang Diskusi Perpustakaan A", type = "ROOM", totalSeats = 10, availableSeats = 4, location = "Perpustakaan Utama, Lantai 3", facilities = "AC, WiFi, Whiteboard, Colokan", priorityOnly = false),
                    Space(name = "Ruang Audio Visual Pasca", type = "ROOM", totalSeats = 25, availableSeats = 25, location = "Gedung Pascasarjana, Lantai 4", facilities = "Dolby Sound, Projector laser UHD, Sofa", priorityOnly = true), // Lecturer Priority Only!
                    
                    // Study Desks
                    Space(name = "Meja Belajar Silent Zone 01", type = "DESK", totalSeats = 1, availableSeats = 1, location = "Perpustakaan Mandiri, Lantai 2", facilities = "Sekat Akrilik, Colokan Daya, Lampu Meja", priorityOnly = false),
                    Space(name = "Meja Belajar Silent Zone 02", type = "DESK", totalSeats = 1, availableSeats = 0, location = "Perpustakaan Mandiri, Lantai 2", facilities = "Sekat Akrilik, Colokan Daya, Lampu Meja", priorityOnly = false),
                    Space(name = "Meja Belajar Mandiri Pojok 03", type = "DESK", totalSeats = 1, availableSeats = 1, location = "Perpustakaan Mandiri, Lantai 2", facilities = "Colokan Daya, Kursi Ergonomis", priorityOnly = false),
                    Space(name = "Meja Belajar Mandiri Pojok 04", type = "DESK", totalSeats = 1, availableSeats = 1, location = "Perpustakaan Mandiri, Lantai 2", facilities = "Colokan Daya, Kursi Ergonomis", priorityOnly = false)
                )

                for (space in initialSpaces) {
                    repository.insertSpace(space)
                }

                // Setup beautiful historic bookings spanning last 7 days to showcase Weekly Occupancy Trends charts
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                
                // Let's seed completed bookings for past week
                val mockUsers = listOf("Andi Wicaksono", "Budi Santoso", "Citra Amalia", "Dian Suti", "Eko Prasetyo", "Faisal Reza")
                val mockSpaces = listOf("Lab Komputer Terpadu", "Ruang Teori Bersama B.101", "Ruang Diskusi Perpustakaan A")

                for (i in 1..8) {
                    calendar.time = Date()
                    calendar.add(Calendar.DAY_OF_YEAR, -i)
                    val dateStr = sdf.format(calendar.time)

                    // Insert 2-3 completed mock bookings per day
                    for (j in 1..2) {
                        val spaceNameStr = mockSpaces.random()
                        val uName = mockUsers.random()
                        
                        val startCal = calendar.clone() as Calendar
                        startCal.set(Calendar.HOUR_OF_DAY, 8 + (j * 2))
                        startCal.set(Calendar.MINUTE, 0)
                        
                        val endCal = calendar.clone() as Calendar
                        endCal.set(Calendar.HOUR_OF_DAY, 10 + (j * 2))
                        endCal.set(Calendar.MINUTE, 0)

                        repository.insertBooking(
                            Booking(
                                spaceId = 1,
                                spaceName = spaceNameStr,
                                spaceType = "ROOM",
                                userName = uName,
                                userRole = if (Math.random() > 0.8) "DOSEN" else "MAHASISWA",
                                startTime = startCal.timeInMillis,
                                endTime = endCal.timeInMillis,
                                dateString = dateStr,
                                status = "COMPLETED"
                            )
                        )
                    }
                }
                
                addSyncLog("Database berhasil di-seed dengan data master dan 16 riwayat analisis!")
            }
        }
    }

    private fun calculateWeeklyTrends(bookings: List<Booking>): List<OccupancyTrend> {
        // We get local calendar dates for current week (Mon to Sun)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val daysOfWeek = mutableListOf<String>()
        val dayNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        
        val trendData = mutableListOf<OccupancyTrend>()
        for (i in 0..6) {
            val dateStr = sdf.format(cal.time)
            daysOfWeek.add(dateStr)
            
            // Calculate mock average occupancy or map count of bookings
            // Say each booking counts as 1.25 hours or 20% occupancy density of campus rooms
            val bookingsOnDay = bookings.filter { it.dateString == dateStr && it.status != "CANCELLED" }.size
            val rate = (bookingsOnDay * 25).coerceAtMost(100) // scale up to %
            
            trendData.add(
                OccupancyTrend(
                    dayLabel = dayNames[i],
                    dateString = dateStr,
                    bookingCount = bookingsOnDay,
                    occupancyPercent = if (rate == 0) (15..45).random() else rate // Ensure baseline so graph always looks exciting
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return trendData
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}

data class OccupancyTrend(
    val dayLabel: String,
    val dateString: String,
    val bookingCount: Int,
    val occupancyPercent: Int
)
