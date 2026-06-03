package com.example.data

import kotlinx.coroutines.flow.Flow

class SpaceRepository(
    private val spaceDao: SpaceDao,
    private val bookingDao: BookingDao,
    private val userDao: UserDao
) {
    val allSpaces: Flow<List<Space>> = spaceDao.getAllSpaces()
    val allBookings: Flow<List<Booking>> = bookingDao.getAllBookings()
    val allUsers: Flow<List<User>> = userDao.getAllUsersFlow()

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun clearUsers() {
        userDao.deleteAllUsers()
    }

    fun getBookingsByDate(dateString: String): Flow<List<Booking>> {
        return bookingDao.getBookingsByDate(dateString)
    }

    suspend fun getSpaceById(id: Int): Space? {
        return spaceDao.getSpaceById(id)
    }

    suspend fun insertSpace(space: Space) {
        spaceDao.insertSpace(space)
    }

    suspend fun updateSpace(space: Space) {
        spaceDao.updateSpace(space)
    }

    suspend fun deleteSpace(space: Space) {
        spaceDao.deleteSpace(space)
    }

    suspend fun getBookingById(id: Int): Booking? {
        return bookingDao.getBookingById(id)
    }

    suspend fun insertBooking(booking: Booking) {
        bookingDao.insertBooking(booking)
    }

    suspend fun updateBooking(booking: Booking) {
        bookingDao.updateBooking(booking)
    }

    suspend fun resetDatabase() {
        spaceDao.deleteAllSpaces()
        bookingDao.deleteAllBookings()
    }
}
