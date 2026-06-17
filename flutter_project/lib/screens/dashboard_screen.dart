import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/ruang_kampus_provider.dart';
import '../models/room.dart';
import '../models/booking.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  int _currentTab = 0; // 0: Rooms, 1: Calendar/Agenda, 2: System Logs

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<RuangKampusProvider>(context);
    final user = provider.currentUser;
    final theme = Theme.of(context);

    // Responsive adaptation: Navigation rail for wider layouts, bottom navigation for mobile
    final screenWidth = MediaQuery.of(context).size.width;
    final isExpanded = screenWidth > 720;

    final isAdmin = user?.role.toLowerCase() == "admin";

    return Scaffold(
      backgroundColor: theme.colorScheme.background,
      appBar: AppBar(
        titleSpacing: 20,
        backgroundColor: theme.colorScheme.surface,
        elevation: 0,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1.0),
          child: Container(color: theme.colorScheme.onSurface.withOpacity(0.08), height: 1.0),
        ),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: theme.colorScheme.primary.withOpacity(0.12),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(Icons.apartment, color: theme.colorScheme.primary, size: 22),
            ),
            const SizedBox(width: 12),
            Text(
              "RuangKampus",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: theme.colorScheme.onSurface, letterSpacing: 0.5),
            ),
          ],
        ),
        actions: [
          // Live sync indicator — hide text label on small screens to avoid overflow
          if (MediaQuery.of(context).size.width > 400)
            Padding(
              padding: const EdgeInsets.only(right: 4.0),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _buildSyncIndicator(),
                  const SizedBox(width: 6),
                  Text(
                    "SQLite Sync",
                    style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.6), fontSize: 11),
                  ),
                ],
              ),
            )
          else
            Padding(
              padding: const EdgeInsets.only(right: 4.0),
              child: _buildSyncIndicator(),
            ),
          IconButton(
            icon: Icon(
              provider.isDarkMode ? Icons.dark_mode : Icons.light_mode,
              color: provider.isDarkMode ? const Color(0xFF818CF8) : const Color(0xFFD97706),
              size: 20,
            ),
            tooltip: provider.isDarkMode ? "Ganti ke Tema Siang" : "Ganti ke Tema Malam",
            onPressed: () {
              provider.toggleTheme();
            },
          ),
          IconButton(
            icon: Icon(Icons.logout, color: theme.colorScheme.primary, size: 20),
            tooltip: "Keluar Sesi",
            onPressed: () {
              provider.logout();
            },
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: Row(
        children: [
          // Navigation Rail sidebar for Tablets/Desktops
          if (isExpanded)
            NavigationRail(
              backgroundColor: theme.colorScheme.surface,
              selectedIndex: _currentTab,
              unselectedIconTheme: IconThemeData(
                color: theme.colorScheme.onSurface.withOpacity(0.45),
              ),
              selectedIconTheme: IconThemeData(color: theme.colorScheme.primary),
              unselectedLabelTextStyle: TextStyle(
                color: theme.colorScheme.onSurface.withOpacity(0.45),
                fontSize: 11,
              ),
              selectedLabelTextStyle: TextStyle(
                color: theme.colorScheme.primary,
                fontSize: 11,
                fontWeight: FontWeight.bold,
              ),
              labelType: NavigationRailLabelType.all,
              destinations: const [
                NavigationRailDestination(
                  icon: Icon(Icons.meeting_room_outlined),
                  selectedIcon: Icon(Icons.meeting_room),
                  label: Text("Sarpras"),
                ),
                NavigationRailDestination(
                  icon: Icon(Icons.calendar_month_outlined),
                  selectedIcon: Icon(Icons.calendar_month),
                  label: Text("Kalender"),
                ),
                NavigationRailDestination(
                  icon: Icon(Icons.terminal_outlined),
                  selectedIcon: Icon(Icons.terminal),
                  label: Text("Log Telemetri"),
                ),
              ],
              onDestinationSelected: (idx) {
                setState(() => _currentTab = idx);
              },
            ),
          // Primary Viewport
          Expanded(
            child: Column(
              children: [
                // Session Status Banner Card
                _buildUserBanner(context, user),

                Expanded(
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 200),
                    child: _buildCurrentTabContent(context, provider),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      // Mobile bottom bar fallback
      bottomNavigationBar: !isExpanded
          ? BottomNavigationBar(
              backgroundColor: theme.colorScheme.surface,
              currentIndex: _currentTab,
              selectedItemColor: theme.colorScheme.primary,
              unselectedItemColor: theme.colorScheme.onSurface.withOpacity(0.45),
              selectedFontSize: 11,
              unselectedFontSize: 11,
              items: const [
                BottomNavigationBarItem(
                  icon: Icon(Icons.meeting_room_outlined),
                  activeIcon: Icon(Icons.meeting_room),
                  label: "Sarpras",
                ),
                BottomNavigationBarItem(
                  icon: Icon(Icons.calendar_month_outlined),
                  activeIcon: Icon(Icons.calendar_month),
                  label: "Kalender",
                ),
                BottomNavigationBarItem(
                  icon: Icon(Icons.terminal_outlined),
                  activeIcon: Icon(Icons.terminal),
                  label: "Log Sync",
                ),
              ],
              onTap: (idx) {
                setState(() => _currentTab = idx);
              },
            )
          : null,
      floatingActionButton: _currentTab == 0 && isAdmin
          ? FloatingActionButton.extended(
              backgroundColor: theme.colorScheme.primary,
              foregroundColor: Colors.white,
              onPressed: () => _showAddRoomDialog(context, provider),
              icon: const Icon(Icons.add),
              label: const Text("Sarpras Baru"),
            )
          : null,
    );
  }

  // --- Header User Info UI ---
  Widget _buildUserBanner(BuildContext context, user) {
    if (user == null) return const SizedBox.shrink();
    final theme = Theme.of(context);
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        border: Border(bottom: BorderSide(color: theme.colorScheme.onSurface.withOpacity(0.08), width: 0.5)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: theme.colorScheme.primary.withOpacity(0.12),
            child: Text(
              user.fullName.substring(0, 1).toUpperCase(),
              style: TextStyle(color: theme.colorScheme.primary, fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  user.fullName,
                  style: TextStyle(color: theme.colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: 14),
                ),
                const SizedBox(height: 2),
                Text(
                  "${user.role.toUpperCase()}  •  ${user.faculty}",
                  style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.6), fontSize: 11),
                ),
              ],
            ),
          ),
          // Display a pill reflecting active role privileges
          _buildRoleBadge(user.role),
        ],
      ),
    );
  }

  Widget _buildRoleBadge(String role) {
    final r = role.toLowerCase();
    Color badgeColor = Colors.teal;
    if (r == "admin") badgeColor = Colors.redAccent;
    if (r == "lecturer") badgeColor = Colors.amber;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: badgeColor.withOpacity(0.12),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: badgeColor.withOpacity(0.3)),
      ),
      child: Text(
        role.toUpperCase(),
        style: TextStyle(color: badgeColor, fontSize: 10, fontWeight: FontWeight.bold, letterSpacing: 0.5),
      ),
    );
  }

  Widget _buildSyncIndicator() {
    return Container(
      width: 10,
      height: 10,
      decoration: const BoxDecoration(
        color: Colors.greenAccent,
        shape: BoxShape.circle,
        boxShadow: [
          BoxShadow(
            color: Colors.greenAccent,
            blurRadius: 6,
            spreadRadius: 2,
          ),
        ],
      ),
    );
  }

  // --- Subviews Routing Switch ---
  Widget _buildCurrentTabContent(BuildContext context, RuangKampusProvider provider) {
    switch (_currentTab) {
      case 0:
        return _buildRoomsTab(context, provider);
      case 1:
        return _buildCalendarTab(context, provider);
      case 2:
        return _buildLogsTab(context, provider);
      default:
        return _buildRoomsTab(context, provider);
    }
  }

  // ================= TAB 1: ROOMS / FACILITIES =================
  Widget _buildRoomsTab(BuildContext context, RuangKampusProvider provider) {
    final theme = Theme.of(context);
    return Column(
      children: [
        // Search & Filters Row controls
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            children: [
              TextField(
                style: TextStyle(color: theme.colorScheme.onSurface),
                decoration: InputDecoration(
                  hintText: "Cari nama ruangan, meja, atau lokasi...",
                  hintStyle: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.5), fontSize: 13),
                  prefixIcon: Icon(Icons.search, color: theme.colorScheme.onSurface.withOpacity(0.5)),
                  filled: true,
                  fillColor: theme.colorScheme.surface,
                  contentPadding: const EdgeInsets.all(14),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(color: theme.colorScheme.onSurface.withOpacity(0.08)),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(color: theme.colorScheme.onSurface.withOpacity(0.08)),
                  ),
                ),
                onChanged: (val) {
                  provider.setSearchQuery(val);
                },
              ),
              const SizedBox(height: 12),
              // Segment Filter Chips — scrollable to prevent overflow on narrow screens
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    _buildTypeChip(provider, "ALL", "Semua Fasilitas"),
                    const SizedBox(width: 8),
                    _buildTypeChip(provider, "ROOM", "Ruangan Kelas"),
                    const SizedBox(width: 8),
                    _buildTypeChip(provider, "DESK", "Meja Belajar"),
                  ],
                ),
              ),
            ],
          ),
        ),

        // Grid List items
        Expanded(
          child: provider.filteredRooms.isEmpty
              ? Center(
                  child: Text(
                    "Tidak ada fasilitas yang cocok dengan filter pencarian.",
                    style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.6), fontSize: 13),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                  itemCount: provider.filteredRooms.length,
                  itemBuilder: (ctx, idx) {
                    final room = provider.filteredRooms[idx];
                    return _buildRoomCard(context, provider, room);
                  },
                ),
        ),
      ],
    );
  }

  Widget _buildTypeChip(RuangKampusProvider provider, String target, String label) {
    final isSelected = provider.filterType == target;
    final theme = Theme.of(context);
    return ChoiceChip(
      selectedColor: theme.colorScheme.primary,
      backgroundColor: theme.colorScheme.surface,
      labelStyle: TextStyle(
        color: isSelected ? Colors.white : theme.colorScheme.onSurface,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
        fontSize: 11,
      ),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      side: BorderSide(color: isSelected ? Colors.transparent : theme.colorScheme.onSurface.withOpacity(0.12)),
      label: Text(label),
      selected: isSelected,
      onSelected: (selected) {
        if (selected) provider.setFilterType(target);
      },
    );
  }

  Widget _buildRoomCard(BuildContext context, RuangKampusProvider provider, Room room) {
    final theme = Theme.of(context);
    final user = provider.currentUser;
    final isAdmin = user?.role.toLowerCase() == "admin";

    return Card(
      color: theme.colorScheme.surface, // Clean white surface bg or dark surface bg
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: room.isUnderMaintenance
              ? Colors.redAccent.withOpacity(0.4)
              : (room.priorityOnly ? Colors.amber.withOpacity(0.5) : theme.colorScheme.onSurface.withOpacity(0.08)),
          width: 1.2,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Category Icon indicators
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: room.type == "ROOM" ? Colors.blue.withOpacity(0.12) : Colors.purple.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    room.type == "ROOM" ? Icons.room_preferences : Icons.desk,
                    color: room.type == "ROOM" ? Colors.blue : Colors.purple,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              room.name,
                              style: TextStyle(color: theme.colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: 15),
                            ),
                          ),
                          if (room.priorityOnly)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: Colors.amber.withOpacity(0.15),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: const Text("DOSEN", style: TextStyle(color: Colors.amber, fontSize: 8, fontWeight: FontWeight.bold)),
                            ),
                        ],
                      ),
                      const SizedBox(height: 2),
                      Row(
                        children: [
                          Icon(Icons.location_on_outlined, color: theme.colorScheme.onSurface.withOpacity(0.5), size: 12),
                          const SizedBox(width: 4),
                          Text(room.location, style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.8), fontSize: 11)),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Facilities bullet pills
            Wrap(
              spacing: 6,
              runSpacing: 4,
              children: room.facilities.split(", ").map((f) {
                return Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.onSurface.withOpacity(0.04),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: theme.colorScheme.onSurface.withOpacity(0.08)),
                  ),
                  child: Text(
                    f,
                    style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.8), fontSize: 10),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 14),

            // Capacities & Status
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                RichText(
                  text: TextSpan(
                    style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurface.withOpacity(0.6)),
                    children: [
                      const TextSpan(text: "Kapasitas: "),
                      TextSpan(
                        text: "${room.availableSeats}/${room.totalSeats}",
                        style: TextStyle(
                          color: room.availableSeats == 0 ? Colors.red : theme.colorScheme.onSurface,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const TextSpan(text: " Kursi"),
                    ],
                  ),
                ),
                if (room.isUnderMaintenance)
                  const Text("⚠️ Sedang Diperbaiki", style: TextStyle(color: Colors.redAccent, fontSize: 11, fontWeight: FontWeight.bold))
                else if (room.availableSeats == 0)
                  Text("Penuh", style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.5), fontSize: 11))
                else
                  const Text("Tersedia", style: TextStyle(color: Colors.green, fontSize: 11, fontWeight: FontWeight.bold)),
              ],
            ),
            Divider(color: theme.colorScheme.onSurface.withOpacity(0.08), height: 24),

            // Actions panel based on user login role
            // Use Column on mobile (admin) to prevent Row overflow
            if (isAdmin)
              Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Admin controls row: delete + maintenance toggle
                  Row(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.delete_outline, color: Colors.redAccent, size: 18),
                        tooltip: "Hapus Sarpras Permanen",
                        onPressed: () => _confirmDeleteRoom(context, provider, room),
                      ),
                      Text(
                        "Maintenance",
                        style: TextStyle(
                          color: room.isUnderMaintenance ? Colors.red : theme.colorScheme.onSurface.withOpacity(0.5),
                          fontSize: 11,
                        ),
                      ),
                      const Spacer(),
                      Transform.scale(
                        scale: 0.9,
                        child: Switch(
                          value: room.isUnderMaintenance,
                          activeColor: Colors.redAccent,
                          onChanged: (state) {
                            provider.updateMaintenanceStatus(room, state);
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  // Booking button full-width on mobile for admin
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: room.isUnderMaintenance
                          ? (provider.isDarkMode ? Colors.white12 : Colors.black12)
                          : theme.colorScheme.primary,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 10),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    onPressed: () => _showBookingDialog(context, provider, room),
                    icon: const Icon(Icons.bookmark_add_outlined, size: 14),
                    label: const Text("Pesan Slot", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                  ),
                ],
              )
            else
              // Non-admin: single booking button aligned right
              Align(
                alignment: Alignment.centerRight,
                child: ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: room.isUnderMaintenance
                        ? (provider.isDarkMode ? Colors.white12 : Colors.black12)
                        : theme.colorScheme.primary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  onPressed: room.isUnderMaintenance
                      ? null
                      : () => _showBookingDialog(context, provider, room),
                  icon: const Icon(Icons.bookmark_add_outlined, size: 14),
                  label: const Text("Pesan Slot", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                ),
              ),
          ],
        ),
      ),
    );
  }

  // ================= TAB 2: CALENDAR SCHEDULER =================
  Widget _buildCalendarTab(BuildContext context, RuangKampusProvider provider) {
    final theme = Theme.of(context);
    final dateString = DateFormat('EEEE, dd MMMM yyyy').format(provider.selectedDate);

    return Column(
      children: [
        // Time Picker Bar
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: theme.colorScheme.onSurface.withOpacity(0.08)),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text("Pilih Tanggal Booking:", style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.6), fontSize: 11)),
                      const SizedBox(height: 2),
                      Text(dateString, style: TextStyle(color: theme.colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: 14)),
                    ],
                  ),
                ),
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: theme.colorScheme.primary.withOpacity(0.12),
                    foregroundColor: theme.colorScheme.primary,
                    elevation: 0,
                  ),
                  onPressed: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate: provider.selectedDate,
                      firstDate: DateTime.now().subtract(const Duration(days: 30)),
                      lastDate: DateTime.now().add(const Duration(days: 90)),
                    );
                    if (picked != null) {
                      provider.setSelectedDate(picked);
                    }
                  },
                  icon: const Icon(Icons.calendar_month, size: 16),
                  label: const Text("Pilih", style: TextStyle(fontSize: 12)),
                ),
              ],
            ),
          ),
        ),

        // Selected dates list output
        Expanded(
          child: provider.bookingsForSelectedDate.isEmpty
              ? Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.event_busy, color: theme.colorScheme.onSurface.withOpacity(0.24), size: 48),
                    const SizedBox(height: 12),
                    Text(
                      "Belum ada agenda pemesanan sarpras di hari ini.",
                      style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.6), fontSize: 12),
                    ),
                  ],
                )
              : ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: provider.bookingsForSelectedDate.length,
                  itemBuilder: (ctx, idx) {
                    final booking = provider.bookingsForSelectedDate[idx];
                    return _buildBookingCard(context, provider, booking);
                  },
                ),
        ),
      ],
    );
  }

  Widget _buildBookingCard(BuildContext context, RuangKampusProvider provider, Booking booking) {
    final theme = Theme.of(context);
    final startT = DateTime.fromMillisecondsSinceEpoch(booking.startTime);
    final endT = DateTime.fromMillisecondsSinceEpoch(booking.endTime);
    final hourFormat = DateFormat('HH:mm');
    final active = booking.status == "ACTIVE";
    final isOwnerOrAdmin = provider.currentUser?.role.toLowerCase() == "admin" || provider.currentUser?.fullName == booking.userName;

    return Card(
      color: active ? theme.colorScheme.surface : theme.colorScheme.onSurface.withOpacity(0.03),
      margin: const EdgeInsets.only(bottom: 10),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: booking.isPriorityOverride && active
              ? Colors.amber.withOpacity(0.5)
              : theme.colorScheme.onSurface.withOpacity(0.06),
          width: 1.2,
        ),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.all(12),
        title: Row(
          children: [
            Expanded(
              child: Text(
                booking.roomName,
                style: TextStyle(
                  color: active ? theme.colorScheme.onSurface : theme.colorScheme.onSurface.withOpacity(0.4),
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                  decoration: active ? null : TextDecoration.lineThrough,
                ),
              ),
            ),
            _buildStatusTag(booking.status, booking.isPriorityOverride),
          ],
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 6),
            Row(
              children: [
                Icon(Icons.schedule, color: active ? Colors.blue.shade400 : theme.colorScheme.onSurface.withOpacity(0.24), size: 12),
                const SizedBox(width: 4),
                Text(
                  "${hourFormat.format(startT)} - ${hourFormat.format(endT)} WIB",
                  style: TextStyle(color: active ? theme.colorScheme.onSurface.withOpacity(0.8) : theme.colorScheme.onSurface.withOpacity(0.4), fontSize: 11),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(Icons.person_outline, color: active ? Colors.teal.shade400 : theme.colorScheme.onSurface.withOpacity(0.24), size: 12),
                const SizedBox(width: 4),
                Text(
                  "${booking.userName} (${booking.userRole.toUpperCase()})",
                  style: TextStyle(color: active ? theme.colorScheme.onSurface.withOpacity(0.6) : theme.colorScheme.onSurface.withOpacity(0.3), fontSize: 11),
                ),
              ],
            ),
          ],
        ),
        trailing: active && isOwnerOrAdmin
            ? IconButton(
                icon: const Icon(Icons.cancel_outlined, color: Colors.redAccent, size: 18),
                tooltip: "Batalkan Pesanan",
                onPressed: () {
                  provider.cancelBooking(booking);
                },
              )
            : null,
      ),
    );
  }

  Widget _buildStatusTag(String status, bool isOverride) {
    if (status == "CANCELLED") {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(color: Colors.red.withOpacity(0.12), borderRadius: BorderRadius.circular(4)),
        child: const Text("BATAL", style: TextStyle(color: Colors.red, fontSize: 8, fontWeight: FontWeight.bold)),
      );
    }
    if (isOverride) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(color: Colors.amber.withOpacity(0.12), borderRadius: BorderRadius.circular(4)),
        child: const Text("PRIORITAS OVERRIDE", style: TextStyle(color: Colors.orange, fontSize: 8, fontWeight: FontWeight.bold)),
      );
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: Colors.green.withOpacity(0.12), borderRadius: BorderRadius.circular(4)),
      child: const Text("AKTIF", style: TextStyle(color: Colors.green, fontSize: 8, fontWeight: FontWeight.bold)),
    );
  }

  // ================= TAB 3: SYSTEM SYNC LOGS =================
  Widget _buildLogsTab(BuildContext context, RuangKampusProvider provider) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text("Riwayat Database Lokal SQLite", style: TextStyle(color: theme.colorScheme.onSurface, fontSize: 13, fontWeight: FontWeight.bold)),
              IconButton(
                icon: Icon(Icons.clear_all, color: theme.colorScheme.onSurface.withOpacity(0.5), size: 16),
                onPressed: () => provider.clearNotifications(),
                tooltip: "Bersihkan Notif",
              ),
            ],
          ),
        ),
        Expanded(
          child: provider.syncLogs.isEmpty
              ? Center(
                  child: Text("Belum ada log telemetri sync.", style: TextStyle(color: theme.colorScheme.onSurface.withOpacity(0.4), fontSize: 11)),
                )
              : ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: provider.syncLogs.length,
                  itemBuilder: (ctx, idx) {
                    final log = provider.syncLogs[idx];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 6.0),
                      child: Text(
                        log,
                        style: TextStyle(
                          color: provider.isDarkMode ? const Color(0xFF4ADE80) : const Color(0xFF15803D),
                          fontFamily: 'monospace',
                          fontSize: 11,
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  // ================= DIALOGS & SHARDS ACTIONS =================
  void _showBookingDialog(BuildContext context, RuangKampusProvider provider, Room room) {
    final nameController = TextEditingController(text: provider.currentUser?.fullName ?? "");
    String startSelected = "08:00";
    String endSelected = "09:00";

    final startTimes = ["08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"];
    final endTimes = ["09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00"];

    showDialog(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (sCtx, setDialogState) {
            return AlertDialog(
              backgroundColor: Theme.of(context).colorScheme.surface,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              title: Text("Pemesanan ${room.name}", style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 16)),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  TextField(
                    controller: nameController,
                    style: const TextStyle(color: Colors.black),
                    decoration: const InputDecoration(
                      labelText: "Nama Peminjam",
                      labelStyle: TextStyle(color: Colors.black54),
                      enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.black12)),
                    ),
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    dropdownColor: Theme.of(context).colorScheme.surface,
                    style: const TextStyle(color: Colors.black),
                    value: startSelected,
                    decoration: const InputDecoration(labelText: "Jam Mulai", labelStyle: TextStyle(color: Colors.black54)),
                    items: startTimes.map((t) => DropdownMenuItem(value: t, child: Text(t, style: const TextStyle(color: Colors.black)))).toList(),
                    onChanged: (val) {
                      setDialogState(() {
                        startSelected = val!;
                        // Automatically shift end hour to prevent error
                        final startIdx = startTimes.indexOf(startSelected);
                        if (endTimes.indexOf(endSelected) <= startIdx) {
                          endSelected = endTimes[startIdx];
                        }
                      });
                    },
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    dropdownColor: Theme.of(context).colorScheme.surface,
                    style: const TextStyle(color: Colors.black),
                    value: endSelected,
                    decoration: const InputDecoration(labelText: "Jam Selesai", labelStyle: TextStyle(color: Colors.black54)),
                    items: endTimes.map((t) => DropdownMenuItem(value: t, child: Text(t, style: const TextStyle(color: Colors.black)))).toList(),
                    onChanged: (val) => setDialogState(() => endSelected = val!),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(ctx),
                  child: const Text("Batal"),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.primary),
                  onPressed: () async {
                    final res = await provider.createBooking(room, nameController.text, startSelected, endSelected);
                    Navigator.pop(ctx);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(res["message"]),
                        backgroundColor: res["success"] ? Colors.green : Colors.red,
                      ),
                    );
                  },
                  child: const Text("Simpan Booking", style: TextStyle(color: Colors.white)),
                ),
              ],
            );
          },
        );
      },
    );
  }

  void _showAddRoomDialog(BuildContext context, RuangKampusProvider provider) {
    final nameController = TextEditingController();
    final locationController = TextEditingController();
    final facilitiesController = TextEditingController();
    final seatsController = TextEditingController(text: "30");
    String typeSelected = "ROOM";
    bool priorityOnly = false;

    showDialog(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (sCtx, setDialogState) {
            return AlertDialog(
              backgroundColor: Theme.of(context).colorScheme.surface,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              title: const Text("Tambah Sarana Baru", style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 16)),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    DropdownButtonFormField<String>(
                      dropdownColor: Theme.of(context).colorScheme.surface,
                      style: const TextStyle(color: Colors.black),
                      value: typeSelected,
                      decoration: const InputDecoration(labelText: "Tipe Fasilitas", labelStyle: TextStyle(color: Colors.black54)),
                      items: const [
                        DropdownMenuItem(value: "ROOM", child: Text("Ruangan", style: TextStyle(color: Colors.black))),
                        DropdownMenuItem(value: "DESK", child: Text("Meja Belajar", style: TextStyle(color: Colors.black))),
                      ],
                      onChanged: (val) => setDialogState(() => typeSelected = val!),
                    ),
                    TextField(
                      controller: nameController,
                      style: const TextStyle(color: Colors.black),
                      decoration: const InputDecoration(labelText: "Nama Sarpras (Contoh: Lab Jaringan)", labelStyle: TextStyle(color: Colors.black54)),
                    ),
                    TextField(
                      controller: locationController,
                      style: const TextStyle(color: Colors.black),
                      decoration: const InputDecoration(labelText: "Lokasi Ruang", labelStyle: TextStyle(color: Colors.black54)),
                    ),
                    TextField(
                      controller: facilitiesController,
                      style: const TextStyle(color: Colors.black),
                      decoration: const InputDecoration(labelText: "Fasilitas (pisahkan dengan koma)", labelStyle: TextStyle(color: Colors.black54)),
                    ),
                    TextField(
                      controller: seatsController,
                      style: const TextStyle(color: Colors.black),
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: "Kapasitas Kursi", labelStyle: TextStyle(color: Colors.black54)),
                    ),
                    CheckboxListTile(
                      title: const Text("Khusus Dosen (Prioritas)", style: TextStyle(color: Colors.black87, fontSize: 13)),
                      value: priorityOnly,
                      onChanged: (val) => setDialogState(() => priorityOnly = val!),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Batal")),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.primary),
                  onPressed: () {
                    final seats = int.tryParse(seatsController.text) ?? 30;
                    provider.addNewRoom(
                      nameController.text.trim(),
                      typeSelected,
                      seats,
                      locationController.text.trim(),
                      facilitiesController.text.trim(),
                      priorityOnly,
                    );
                    Navigator.pop(ctx);
                  },
                  child: const Text("Tambahkan", style: TextStyle(color: Colors.white)),
                ),
              ],
            );
          },
        );
      },
    );
  }

  void _confirmDeleteRoom(BuildContext context, RuangKampusProvider provider, Room room) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: Theme.of(context).colorScheme.surface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Row(
            children: [
              Icon(Icons.warning, color: Colors.redAccent),
              SizedBox(width: 8),
              Text("Hapus Sarpras", style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 16)),
            ],
          ),
          content: Text("Apakah Anda yakin ingin menghapus '${room.name}' dari database lokal SQLite secara permanen?", style: const TextStyle(color: Colors.black87, fontSize: 13)),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Batal")),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
              onPressed: () {
                provider.deleteRoom(room);
                Navigator.pop(ctx);
              },
              child: const Text("Hapus Permanen", style: TextStyle(color: Colors.white)),
            ),
          ],
        );
      },
    );
  }
}
