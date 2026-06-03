import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/room.dart';
import '../models/booking.dart';
import '../models/user.dart';

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('ruangkampus.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    return await openDatabase(
      path,
      version: 2,
      onCreate: _createDB,
      onUpgrade: _upgradeDB,
    );
  }

  Future _createDB(Database db, int version) async {
    const idType = 'INTEGER PRIMARY KEY AUTOINCREMENT';
    const textType = 'TEXT NOT NULL';
    const intType = 'INTEGER NOT NULL';

    await db.execute('''
      CREATE TABLE rooms (
        id $idType,
        name $textType,
        type $textType,
        totalSeats $intType,
        availableSeats $intType,
        location $textType,
        facilities $textType,
        priorityOnly $intType,
        isUnderMaintenance $intType
      )
    ''');

    await db.execute('''
      CREATE TABLE bookings (
        id $idType,
        roomId $intType,
        roomName $textType,
        roomType $textType,
        userName $textType,
        userRole $textType,
        startTime $intType,
        endTime $intType,
        dateString $textType,
        status $textType DEFAULT 'ACTIVE',
        isPriorityOverride $intType DEFAULT 0,
        reminderSent $intType DEFAULT 0
      )
    ''');

    await db.execute('''
      CREATE TABLE users (
        username TEXT PRIMARY KEY,
        passwordHash $textType,
        fullName $textType,
        role $textType,
        faculty $textType
      )
    ''');

    // Populate Initial Mock Data
    await _seedDatabase(db);
  }

  Future _upgradeDB(Database db, int oldVersion, int newVersion) async {
    // Basic migration path
    if (oldVersion < 2) {
      // Version 2 additions if any
    }
  }

  Future _seedDatabase(Database db) async {
    // 1. Seed Rooms
    final initialRooms = [
      Room(name: "Lab Komputer Terpadu", type: "ROOM", totalSeats: 40, availableSeats: 28, location: "Gedung Tekno, Lantai 2", facilities: "WiFi 500Mbps, AC, Proyektor, PC Intel i7", priorityOnly: false),
      Room(name: "Ruang Teori Bersama B.101", type: "ROOM", totalSeats: 60, availableSeats: 45, location: "Gedung Kuliah Utama, Lantai 1", facilities: "Papan Tulis, AC, Proyektor, Speaker", priorityOnly: false),
      Room(name: "Ruang Diskusi Perpustakaan A", type: "ROOM", totalSeats: 10, availableSeats: 4, location: "Perpustakaan Utama, Lantai 3", facilities: "AC, WiFi, Whiteboard, Colokan", priorityOnly: false),
      Room(name: "Ruang Audio Visual Pasca", type: "ROOM", totalSeats: 25, availableSeats: 25, location: "Gedung Pascasarjana, Lantai 4", facilities: "Dolby Sound, Projector laser UHD, Sofa", priorityOnly: true),
      Room(name: "Meja Belajar Silent Zone 01", type: "DESK", totalSeats: 1, availableSeats: 1, location: "Perpustakaan Mandiri, Lantai 2", facilities: "Sekat Akrilik, Colokan Daya, Lampu Meja", priorityOnly: false),
      Room(name: "Meja Belajar Silent Zone 02", type: "DESK", totalSeats: 1, availableSeats: 0, location: "Perpustakaan Mandiri, Lantai 2", facilities: "Sekat Akrilik, Colokan Daya, Lampu Meja", priorityOnly: false),
      Room(name: "Meja Belajar Mandiri Pojok 03", type: "DESK", totalSeats: 1, availableSeats: 1, location: "Perpustakaan Mandiri, Lantai 2", facilities: "Colokan Daya, Kursi Ergonomis", priorityOnly: false),
    ];

    for (final room in initialRooms) {
      await db.insert('rooms', room.toMap());
    }

    // 2. Seed Default Users for Testing
    final defaultUsers = [
      User(username: "mhs", passwordHash: "mhs123", fullName: "Ahmad Fauzi", role: "student", faculty: "Fakultas Ilmu Komputer"),
      User(username: "dosen", passwordHash: "dosen123", fullName: "Dr. Ir. Hermawan", role: "lecturer", faculty: "Fakultas Ilmu Komputer"),
      User(username: "admin", passwordHash: "admin123", fullName: "Super Admin Sarpas", role: "admin", faculty: "Direktorat Sarpras Kampus"),
    ];

    for (final user in defaultUsers) {
      await db.insert('users', user.toMap());
    }
  }

  // --- Rooms Repo ---
  Future<List<Room>> getAllRooms() async {
    final db = await instance.database;
    final result = await db.query('rooms', orderBy: 'type DESC, id ASC');
    return result.map((json) => Room.fromMap(json)).toList();
  }

  Future<Room?> getRoomById(int id) async {
    final db = await instance.database;
    final result = await db.query('rooms', where: 'id = ?', whereArgs: [id]);
    if (result.isNotEmpty) {
      return Room.fromMap(result.first);
    }
    return null;
  }

  Future<int> insertRoom(Room room) async {
    final db = await instance.database;
    return await db.insert('rooms', room.toMap());
  }

  Future<int> updateRoom(Room room) async {
    final db = await instance.database;
    return await db.update(
      'rooms',
      room.toMap(),
      where: 'id = ?',
      whereArgs: [room.id],
    );
  }

  Future<int> deleteRoom(int id) async {
    final db = await instance.database;
    return await db.delete(
      'rooms',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  // --- Bookings Repo ---
  Future<List<Booking>> getAllBookings() async {
    final db = await instance.database;
    final result = await db.query('bookings', orderBy: 'startTime DESC');
    return result.map((json) => Booking.fromMap(json)).toList();
  }

  Future<List<Booking>> getBookingsByDate(String dateString) async {
    final db = await instance.database;
    final result = await db.query('bookings', where: 'dateString = ?', whereArgs: [dateString]);
    return result.map((json) => Booking.fromMap(json)).toList();
  }

  Future<Booking?> getBookingById(int id) async {
    final db = await instance.database;
    final result = await db.query('bookings', where: 'id = ?', whereArgs: [id]);
    if (result.isNotEmpty) {
      return Booking.fromMap(result.first);
    }
    return null;
  }

  Future<int> insertBooking(Booking booking) async {
    final db = await instance.database;
    return await db.insert('bookings', booking.toMap());
  }

  Future<int> updateBooking(Booking booking) async {
    final db = await instance.database;
    return await db.update(
      'bookings',
      booking.toMap(),
      where: 'id = ?',
      whereArgs: [booking.id],
    );
  }

  // --- Users Repo ---
  Future<User?> getUser(String username) async {
    final db = await instance.database;
    final result = await db.query('users', where: 'username = ?', whereArgs: [username]);
    if (result.isNotEmpty) {
      return User.fromMap(result.first);
    }
    return null;
  }

  Future<int> insertUser(User user) async {
    final db = await instance.database;
    return await db.insert('users', user.toMap());
  }

  Future close() async {
    final db = await instance.database;
    db.close();
  }
}
