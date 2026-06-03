import 'package:flutter/material.dart';
import '../models/room.dart';
import '../models/booking.dart';
import '../models/user.dart';
import '../services/database_helper.dart';

class RuangKampusProvider extends ChangeNotifier {
  final _db = DatabaseHelper.instance;

  User? _currentUser;
  List<Room> _rooms = [];
  List<Booking> _bookings = [];
  List<String> _syncLogs = [];
  List<String> _notifications = [];

  String _searchQuery = "";
  String _filterType = "ALL"; // "ALL", "ROOM", "DESK"
  DateTime _selectedDate = DateTime.now();
  bool _isDarkMode = false;

  RuangKampusProvider() {
    _initData();
    _addSyncLog("Sistem RuangKampus diinisialisasi.");
  }

  // Getters
  User? get currentUser => _currentUser;
  List<Room> get allRooms => _rooms;
  List<Booking> get allBookings => _bookings;
  List<String> get syncLogs => _syncLogs;
  List<String> get notifications => _notifications;
  String get searchQuery => _searchQuery;
  String get filterType => _filterType;
  DateTime get selectedDate => _selectedDate;
  bool get isDarkMode => _isDarkMode;

  void toggleTheme() {
    _isDarkMode = !_isDarkMode;
    _addSyncLog("Tema diubah menjadi: ${_isDarkMode ? 'Malam (Gelap)' : 'Siang (Terang)'}");
    notifyListeners();
  }

  // Filtered lists
  List<Room> get filteredRooms {
    return _rooms.where((room) {
      final matchesSearch = room.name.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          room.location.toLowerCase().contains(_searchQuery.toLowerCase());
      final matchesType = _filterType == "ALL" || room.type == _filterType;
      return matchesSearch && matchesType;
    }).toList();
  }

  List<Booking> get bookingsForSelectedDate {
    final ymd = "${_selectedDate.year}-${_selectedDate.month.toString().padLeft(2, '0')}-${_selectedDate.day.toString().padLeft(2, '0')}";
    return _bookings.where((b) => b.dateString == ymd).toList();
  }

  // Load Initial Data
  Future<void> _initData() async {
    _rooms = await _db.getAllRooms();
    _bookings = await _db.getAllBookings();
    notifyListeners();
  }

  // Filter methods
  void setSearchQuery(String query) {
    _searchQuery = query;
    notifyListeners();
  }

  void setFilterType(String type) {
    _filterType = type;
    notifyListeners();
  }

  void setSelectedDate(DateTime date) {
    _selectedDate = date;
    notifyListeners();
  }

  // Auth Operations
  Future<bool> login(String username, String password) async {
    final user = await _db.getUser(username);
    if (user != null && user.passwordHash == password) {
      _currentUser = user;
      _addSyncLog("User '${user.fullName}' (${user.role}) berhasil masuk.");
      _addNotification("Selamat datang kembali, ${user.fullName}!");
      notifyListeners();
      return true;
    }
    return false;
  }

  Future<bool> signUp(String username, String password, String name, String role, String faculty) async {
    final existing = await _db.getUser(username);
    if (existing != null) {
      _addNotification("Username sudah terdaftar!");
      return false;
    }
    final newUser = User(
      username: username,
      passwordHash: password,
      fullName: name,
      role: role.toLowerCase(), // Save normalized lowercase role
      faculty: faculty,
    );
    await _db.insertUser(newUser);
    _currentUser = newUser;
    _addSyncLog("Anggota baru terdaftar: ${newUser.fullName} ($role)");
    _addNotification("Akun berhasil dibuat!");
    notifyListeners();
    return true;
  }

  void logout() {
    if (_currentUser != null) {
      _addSyncLog("User '${_currentUser!.fullName}' telah keluar.");
    }
    _currentUser = null;
    notifyListeners();
  }

  // Room Management (Admin Only)
  Future<void> addNewRoom(String name, String type, int seats, String location, String facilities, bool priorityOnly) async {
    final room = Room(
      name: name,
      type: type,
      totalSeats: seats,
      availableSeats: seats,
      location: location,
      facilities: facilities,
      priorityOnly: priorityOnly,
      isUnderMaintenance: false,
    );
    await _db.insertRoom(room);
    await _initData();
    _addSyncLog("Mengunggah sarpras baru: $name oleh Admin.");
    _addNotification("Sarpras berhasil ditambahkan!");
  }

  Future<void> deleteRoom(Room room) async {
    if (room.id != null) {
      await _db.deleteRoom(room.id!);
      await _initData();
      _addSyncLog("Menghapus sarpras permanen: ${room.name} oleh Admin.");
      _addNotification("Sarpras ${room.name} berhasil dihapus.");
    }
  }

  Future<void> updateMaintenanceStatus(Room room, bool isUnderMaintenance) async {
    final updated = room.copyWith(isUnderMaintenance: isUnderMaintenance);
    await _db.updateRoom(updated);
    await _initData();
    _addSyncLog("Status maintenance ${room.name} diubah menjadi: $isUnderMaintenance");
    _addNotification("${room.name} dalam status ${isUnderMaintenance ? 'Perbaikan' : 'Siap Digunakan'}");
  }

  // Booking Logic (Core Business Rules Translation)
  Future<Map<String, dynamic>> createBooking(Room room, String requestedName, String startTimeStr, String endTimeStr) async {
    try {
      final user = _currentUser;
      final targetRole = (user?.role ?? "student").toLowerCase();
      final finalUserName = requestedName.trim().isEmpty ? (user?.fullName ?? "Pengguna Sarpras") : requestedName.trim();

      // Parse hours to epoch ms relative to the selected date
      final startHours = int.parse(startTimeStr.split(":")[0]);
      final endHours = int.parse(endTimeStr.split(":")[0]);

      final startEpoch = DateTime(_selectedDate.year, _selectedDate.month, _selectedDate.day, startHours).millisecondsSinceEpoch;
      final endEpoch = DateTime(_selectedDate.year, _selectedDate.month, _selectedDate.day, endHours).millisecondsSinceEpoch;

      if (startEpoch >= endEpoch) {
        return {"success": false, "message": "Waktu mulai harus mendahului waktu selesai!"};
      }

      // 1. Conflict checking with already booked active slots of the same room
      final ymd = "${_selectedDate.year}-${_selectedDate.month.toString().padLeft(2, '0')}-${_selectedDate.day.toString().padLeft(2, '0')}";
      final activeConflicts = _bookings.where((b) {
        return b.roomId == room.id &&
            b.dateString == ymd &&
            b.status == "ACTIVE" &&
            ((startEpoch >= b.startTime && startEpoch < b.endTime) ||
                (endEpoch > b.startTime && endEpoch <= b.endTime) ||
                (startEpoch <= b.startTime && endEpoch >= b.endTime));
      }).toList();

      bool didPerformOverride = false;

      if (activeConflicts.isNotEmpty) {
        switch (targetRole) {
          case "admin":
            // Admin has highest priority override: dismisses any conflicting bookings
            for (final conflict in activeConflicts) {
              final canceled = conflict.copyWith(status: "CANCELLED", isPriorityOverride: true);
              await _db.updateBooking(canceled);
              _addNotification("⚠️ Pemesanan $conflict.userRole (${conflict.userName}) dibatalkan oleh Admin!");
              _addSyncLog("Admin override: Membatalkan reservasi ${conflict.userRole} '${conflict.userName}'");
            }
            didPerformOverride = true;
            break;
          case "lecturer":
            // Lecturers can override Students ("student"), but not fellow lecturers or admins
            final unoverrideable = activeConflicts.where((c) => c.userRole.toLowerCase() == "lecturer" || c.userRole.toLowerCase() == "admin").toList();
            if (unoverrideable.isNotEmpty) {
              return {
                "success": false,
                "message": "Slot waktu sudah dipesan oleh sesama Dosen atau Admin (${unoverrideable.first().userName})."
              };
            }

            // Cancel students slots
            for (final conflict in activeConflicts) {
              if (conflict.userRole.toLowerCase() == "student") {
                final canceled = conflict.copyWith(status: "CANCELLED", isPriorityOverride: true);
                await _db.updateBooking(canceled);
                _addNotification("⚠️ Pemesanan Mahasiswa (${conflict.userName}) dibatalkan otomatis untuk Prioritas Dosen!");
                _addSyncLog("Dosen override: Membatalkan reservasi Mhs '${conflict.userName}'");
              }
            }
            didPerformOverride = true;
            break;
          default: // student
            return {
              "success": false,
              "message": "Waktu ini sudah dipesan oleh ${activeConflicts.first().userName} (${activeConflicts.first().userRole})."
            };
        }
      }

      // 2. Maintenance Bypass Block
      if (room.isUnderMaintenance && targetRole != "admin") {
        return {"success": false, "message": "Ruang sedang dalam masa perbaikan/maintenance."};
      }

      // 3. Check exclusive priority room rules (e.g. priorityOnly rooms)
      if (room.priorityOnly && targetRole != "lecturer" && targetRole != "admin") {
        return {"success": false, "message": "Akses Terbatas: Ruangan khusus Dosen (Prioritas Utama)!"};
      }

      // 4. Update the room seat decrement
      if (room.availableSeats > 0) {
        await _db.updateRoom(room.copyWith(availableSeats: room.availableSeats - 1));
      }

      // 5. Store booking to local sqlite database
      final newBooking = Booking(
        roomId: room.id!,
        roomName: room.name,
        roomType: room.type,
        userName: finalUserName,
        userRole: targetRole,
        startTime: startEpoch,
        endTime: endEpoch,
        dateString: ymd,
        status: "ACTIVE",
        isPriorityOverride: didPerformOverride,
      );

      await _db.insertBooking(newBooking);
      await _initData();

      _addSyncLog("Pemesanan disimpan: ${room.name} oleh $finalUserName");
      return {
        "success": true,
        "message": didPerformOverride
            ? "Pemesanan berhasil disimpan (dengan Overriding Prioritas)!"
            : "Pemesanan berhasil disimpan!"
      };
    } catch (e) {
      return {"success": false, "message": "Terjadi kesalahan: ${e.toString()}"};
    }
  }

  Future<void> cancelBooking(Booking booking) async {
    final updated = booking.copyWith(status: "CANCELLED");
    await _db.updateBooking(updated);

    final room = await _db.getRoomById(booking.roomId);
    if (room != null) {
      await _db.updateRoom(room.copyWith(availableSeats: (room.availableSeats + 1).clamp(0, room.totalSeats)));
    }
    await _initData();
    _addSyncLog("Membatalkan pemesanan: ${booking.roomName} oleh ${booking.userName}");
    _addNotification("Pemesanan Anda berhasil dibatalkan.");
  }

  // Sync log helper helpers
  void _addSyncLog(String log) {
    final now = DateTime.now();
    final timeStr = "${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}:${now.second.toString().padLeft(2, '0')}";
    _syncLogs.insert(0, "[$timeStr] $log");
    notifyListeners();
  }

  void _addNotification(String message) {
    _notifications.insert(0, message);
    if (_notifications.length > 20) _notifications.removeLast();
    notifyListeners();
  }

  void clearNotifications() {
    _notifications.clear();
    notifyListeners();
  }
}
