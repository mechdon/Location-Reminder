package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initdb(){
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderAndGetById() = runBlockingTest {
        // GIVEN - Save a reminder
        val reminder = ReminderDTO("Title", "Description", "Big Ben", 51.5, 0.12)
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get reminder id from database
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The reminder contains the expected values
        assertThat<ReminderDTO>(loaded as ReminderDTO, `is`(notNullValue()))
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun saveRemindersAndGetReminders() = runBlockingTest {
        // GIVEN - Save a set of reminders
        val reminder1 = ReminderDTO("Title", "Description", "Big Ben", 51.5, 0.12)
        database.reminderDao().saveReminder(reminder1)
        val reminder2 = ReminderDTO("Title", "Description", "Eiffel Tower", 48.86, 2.29)
        database.reminderDao().saveReminder(reminder2)
        val reminder3 = ReminderDTO("Title", "Description", "Statue of Liberty", 40.69, -74.04)
        database.reminderDao().saveReminder(reminder3)

        // WHEN - Get all reminders
        val loaded = database.reminderDao().getReminders()

        // THEN - The loaded list contains the expected values
        assertThat(loaded, hasItem(reminder1))
        assertThat(loaded, hasItem(reminder2))
        assertThat(loaded, hasItem(reminder3))
        assertThat(loaded, `is`(notNullValue()))
        assertThat(loaded.size, `is`(3))

    }

    @Test
    fun saveRemindersAndDeleteAllReminders() = runBlockingTest {
        // GIVEN - Save and delete a set of reminders
        val reminder1 = ReminderDTO("Title", "Description", "Big Ben", 51.5, 0.12)
        database.reminderDao().saveReminder(reminder1)
        val reminder2 = ReminderDTO("Title", "Description", "Eiffel Tower", 48.86, 2.29)
        database.reminderDao().saveReminder(reminder2)
        val reminder3 = ReminderDTO("Title", "Description", "Statue of Liberty", 40.69, -74.04)
        database.reminderDao().saveReminder(reminder3)
        database.reminderDao().deleteAllReminders()

        // WHEN - All reminders are deleted
        val loaded = database.reminderDao().getReminders()

        // THEN - The list will be empty
        assertThat(loaded.isEmpty(), `is`(true))
        assertThat(loaded.size, `is`(0))

    }

    @Test
    fun getRemindersWhenNoDataFound() = runBlockingTest {
        // GIVEN - No reminder, all reminders are deleted
        database.reminderDao().deleteAllReminders()

        // WHEN - Getting reminders
        val loaded = database.reminderDao().getReminders()

        // THEN - The list will be empty
        assertThat(loaded.isEmpty(), `is`(true))
        assertThat(loaded.size, `is`(0))
    }



}