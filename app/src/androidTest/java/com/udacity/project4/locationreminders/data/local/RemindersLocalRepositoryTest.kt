package com.udacity.project4.locationreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var database: RemindersDatabase
    private lateinit var remindersDao: RemindersDao
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun init() {
        database = Room.inMemoryDatabaseBuilder(
                getApplicationContext(),
                RemindersDatabase::class.java
        )
                .allowMainThreadQueries()
                .build()
        remindersDao = database.reminderDao()

        repository = RemindersLocalRepository(
                remindersDao,
                Dispatchers.Main
        )
    }

    @After
    fun clodeDB() {
        database.close()
    }

    @Test
    fun getReminders() = runBlocking {

        val reminder1 = ReminderDTO("Title", "Description", "Big Ben", 51.5, 0.12)
        repository.saveReminder(reminder1)
        val reminder2 = ReminderDTO("Title", "Description", "Eiffel Tower", 48.86, 2.29)
        repository.saveReminder(reminder2)
        val reminder3 = ReminderDTO("Title", "Description", "Statue of Liberty", 40.69, -74.04)
        repository.saveReminder(reminder3)

        val loaded = repository.getReminders() as Result.Success<List<ReminderDTO>>

        assertThat(loaded.data, hasItem(reminder1))
        assertThat(loaded.data, hasItem(reminder2))
        assertThat(loaded.data, hasItem(reminder3))
        assertThat(loaded, `is`(notNullValue()))
        assertThat(loaded.data.size, `is`(3))

    }

    @Test
    fun saveReminder_getReminderById() = runBlocking {

        val reminder = ReminderDTO("Tours", "Take photos along the bridge", "London Bridge", 51.5, -0.08)
        repository.saveReminder(reminder)

        val loaded = repository.getReminder(reminder.id) as Result.Success<ReminderDTO>

        assertThat(loaded.data, `is`(notNullValue()))
        assertThat(loaded.data.id, `is`(reminder.id))
        assertThat(loaded.data.title, `is`(reminder.title))
        assertThat(loaded.data.description, `is`(reminder.description))
        assertThat(loaded.data.location, `is`(reminder.location))
        assertThat(loaded.data.latitude, `is`(reminder.latitude))
        assertThat(loaded.data.longitude, `is`(reminder.longitude))

    }

    @Test
    fun deleteAllReminders() = runBlocking {

        getReminders()

        repository.deleteAllReminders()

        val loaded = repository.getReminders() as Result.Success<List<ReminderDTO>>

        assertThat(loaded.data.isEmpty(), `is`(true))
        assertThat(loaded.data.size, `is`(0))

    }

    @Test
    fun shouldReturnError() = runBlocking{

        val loaded = repository.getReminder("AD") as Result.Error
        assertThat(loaded.message, `is`(notNullValue()))
        assertThat(loaded.message, `is`("Reminder not found!"))

    }

}