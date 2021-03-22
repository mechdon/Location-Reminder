package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest {

    private lateinit var remindersDataSource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupModel() {
        remindersDataSource = FakeDataSource()

        // Given a fresh new ViewModel
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), remindersDataSource)
    }

    @After
    fun stop_koin() = runBlocking {
        stopKoin()
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {

        // When saving reminder
        mainCoroutineRule.pauseDispatcher()

        val reminder = ReminderDataItem(
            "Title", "Description", "Big Ben", 51.5, 0.12)

        saveReminderViewModel.saveReminder(reminder)

        // Then the loading is triggered
        val value1 = saveReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(value1, `is`(true))

        mainCoroutineRule.resumeDispatcher()
        val value2 = saveReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(value2, `is`(false))

        val value3 = saveReminderViewModel.showToast.value
        assertThat(value3, `is` ("Reminder Saved !"))

    }

    @Test
    fun onClear_nullValues() = mainCoroutineRule.runBlockingTest{
        // When clearing all fields
        saveReminderViewModel.onClear()

        // Then all values set to null
        val title = saveReminderViewModel.reminderTitle.value
        assertThat(title, `is`(nullValue()))

        val description = saveReminderViewModel.reminderDescription.value
        assertThat(description, `is`(nullValue()))

        val location = saveReminderViewModel.reminderSelectedLocationStr.value
        assertThat(location, `is`(nullValue()))

        val poi = saveReminderViewModel.selectedPOI.value
        assertThat(poi, `is`(nullValue()))

        val latitude = saveReminderViewModel.latitude.value
        assertThat(latitude, `is`(nullValue()))

        val longitude = saveReminderViewModel.longitude.value
        assertThat(longitude, `is`(nullValue()))

    }


   @Test
   fun onGeofenceEnable_boolean() = mainCoroutineRule.runBlockingTest {
       // When enabling geofence
       saveReminderViewModel.onGeofenceEnable()

       // Then boolean flag set to true
       val gfEnable = saveReminderViewModel.eventGeofence.value
       assertThat(gfEnable, `is`(true))
   }


   @Test
   fun validateEnteredData_nullTitle_returnsFalse() {
       // When validating entered data with null title
       val reminder = ReminderDataItem(null, "Description", "Big Ben", 51.5, 0.12)
       val result = saveReminderViewModel.validateEnteredData(reminder)

       // Then return false and show snackbar error
       assertThat(result, `is`(false))
       val errTitle = saveReminderViewModel.showSnackBarInt.value
       assertThat(errTitle, `is`(R.string.err_enter_title))
   }


    @Test
    fun validateEnteredData_emptyTitle_returnsFalse() {
        // When validating entered data with empty title
        val reminder = ReminderDataItem("", "Description", "Big Ben", 51.5, 0.12)
        val result = saveReminderViewModel.validateEnteredData(reminder)

        // Then return false and show snackbar error
        assertThat(result, `is`(false))
        val errTitle = saveReminderViewModel.showSnackBarInt.value
        assertThat(errTitle, `is`(R.string.err_enter_title))

    }



    @Test
    fun validateEnteredData_nullLocation_returnsFalse() {
        // When validating entered data with null location
        val reminder = ReminderDataItem("Title", "Description", null, 51.5, 0.12)
        val result = saveReminderViewModel.validateEnteredData(reminder)

        // Then return false and show snackbar error
        assertThat(result, `is`(false))
        val errTitle = saveReminderViewModel.showSnackBarInt.value
        assertThat(errTitle, `is`(R.string.err_select_location))
    }

    @Test
    fun validateEnteredData_emptyLocation_returnsFalse() {
        // When validating entered data with empty location
        val reminder = ReminderDataItem("Title", "Description", "", 51.5, 0.12)
        val result = saveReminderViewModel.validateEnteredData(reminder)

        // Then return false and show snackbar error
        assertThat(result, `is`(false))
        val errTitle = saveReminderViewModel.showSnackBarInt.value
        assertThat(errTitle, `is`(R.string.err_select_location))

    }

    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {

        // When there is a null title error
        remindersDataSource.setReturnError(true)
        val reminder = ReminderDataItem(null, "Description", "Big Ben", 51.5, 0.12)

        // Then returns false and snackbar to show error
        val result = saveReminderViewModel.validateEnteredData(reminder)
        assertThat(result, `is`(false))
        val errTitle = saveReminderViewModel.showSnackBarInt.value
        assertThat(errTitle, `is`(R.string.err_enter_title))
    }

}