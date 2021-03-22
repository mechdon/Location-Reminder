package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun init() = runBlocking {
        // Stop original Koin
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
           viewModel {
               RemindersListViewModel(appContext, get() as ReminderDataSource)
           }
            single {
                SaveReminderViewModel(appContext, get() as ReminderDataSource)
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }

    // Start a new Koin module
    startKoin {
        modules(listOf(myModule))
    }

    // Get from repository
    repository = get()

    // Clear all previous data
    runBlocking { repository.deleteAllReminders() }

    }


    @Test
    fun reminderList_displayedInUI() {
        // Save new reminders
        val reminder1 = ReminderDTO("Title", "Description", "Big Ben", 51.5, 0.12)
        val reminder2 = ReminderDTO("Title", "Description", "Eiffel Tower", 48.86, 2.29)
        val reminder3 = ReminderDTO("Title", "Description", "Statue of Liberty", 40.69, -74.04)

        runBlocking {
            repository.saveReminder(reminder1)
            repository.saveReminder(reminder2)
            repository.saveReminder(reminder3)
        }

        // Check that all reminders are displayed
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.reminderssRecyclerView))
               .check(matches(isDisplayed()))

        onView(withId(R.id.reminderssRecyclerView))
                .check(matches(hasChildCount(3)))
                .check(matches(hasDescendant(withText("Big Ben"))))
                .check(matches(hasDescendant(withText("Eiffel Tower"))))
                .check(matches(hasDescendant(withText("Statue of Liberty"))))

    }

    @Test
    fun addBtn_navigateToSaveReminderFragment(){

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB))
                .perform(click())
        verify(navController).navigate(
                ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun noData_displaysNoData()  {

        runBlocking {
            repository.deleteAllReminders()
        }

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(withText(R.string.no_data)))

    }


}