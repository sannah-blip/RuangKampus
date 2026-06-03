package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "spaces")
data class Space(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "ROOM" (Ruangan) or "DESK" (Meja Belajar)
    val totalSeats: Int,
    val availableSeats: Int,
    val location: String, // e.g., "Gedung A, Lantai 2"
    val facilities: String, // comma separated, e.g., "AC, Projector, WiFi"
    val priorityOnly: Boolean = false, // True if reserved for Lecturers/Dosen priority
    val isUnderMaintenance: Boolean = false
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val spaceId: Int,
    val spaceName: String,
    val spaceType: String,
    val userName: String,
    val userRole: String, // "MAHASISWA" or "DOSEN"
    val startTime: Long, // Epoch timestamp in ms
    val endTime: Long, // Epoch timestamp in ms
    val dateString: String, // "YYYY-MM-DD" for calendar syncing
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "CANCELLED"
    val isPriorityOverride: Boolean = false, // True if a Lecturer overrode a previous booking
    val reminderSent: Boolean = false
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val passwordHash: String, // stored plain or hashed, since the prompt simply says sqlite we can store password plainly for simplicity or clear string matching
    val fullName: String,
    val role: String, // "MAHASISWA", "DOSEN", "ADMIN"
    val faculty: String = "Fakultas Ilmu Komputer"
)

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY type DESC, id ASC")
    fun getAllSpaces(): Flow<List<Space>>

    @Query("SELECT * FROM spaces WHERE id = :id")
    suspend fun getSpaceById(id: Int): Space?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: Space)

    @Update
    suspend fun updateSpace(space: Space)

    @Delete
    suspend fun deleteSpace(space: Space)

    @Query("DELETE FROM spaces")
    suspend fun deleteAllSpaces()
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY startTime DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE dateString = :dateString")
    fun getBookingsByDate(dateString: String): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: Int): Booking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking)

    @Update
    suspend fun updateBooking(booking: Booking)

    @Query("DELETE FROM bookings")
    suspend fun deleteAllBookings()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

@Database(entities = [Space::class, Booking::class, User::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun bookingDao(): BookingDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ruangkampus_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
