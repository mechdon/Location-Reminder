package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class RemindersListViewModelTest {

    private lateinit var remindersDataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        remindersDataSource = FakeDataSource()

        // Given a fresh new ViewModel
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), remindersDataSource)
    }

    @After
    fun stop_koin() = runBlocking {
        stopKoin()
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {

        // When loading reminders

        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()

        // Then the loading event is triggered

        val value1 = remindersListViewModel.showLoading.getOrAwaitValue()
        assertThat(value1, `is` (true))

        mainCoroutineRule.resumeDispatcher()
        val value2 = remindersListViewModel.showLoading.getOrAwaitValue()
        assertThat(value2, `is` (false))
    }


    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {

        // When error is true
        remindersDataSource.setReturnError(true)

        remindersListViewModel.loadReminders()

        // Then snackbar to show error

        val value = remindersListViewModel.showSnackBar.getOrAwaitValue()
        assertThat(value, `is`(R.string.test_exception.toString()))

    }


}