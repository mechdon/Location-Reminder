package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
        AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()

        val myModule = module {
            viewModel {
                RemindersListViewModel(
                        appContext,
                        get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                        appContext,
                        get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


    @Test
    fun saveReminder() = runBlocking {

        val reminder = ReminderDTO("Tour", "Take Photos", "Big Ben", 51.5, 0.12)
        repository.saveReminder(reminder)

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).perform(replaceText("Tour"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Take Photos"))

        onView(withId(R.id.selectLocation)).perform(click())

        onView(withId(R.id.map)).check(matches(isDisplayed()))
        onView(withId(R.id.map)).perform(click())

        pressBack()
        delay(1000)

        onView(withId(R.id.saveReminder)).perform(click())

        pressBack()
        delay(1000)

        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasDescendant(withText("Tour"))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasDescendant(withText("Take Photos"))))

        activityScenario.close()
    }


    @Test
    fun pullToRefresh() = runBlocking {

        val reminder1 = ReminderDTO("Tour", "Sight Seeing", "Big Ben", 51.5, 0.12)
        repository.saveReminder(reminder1)
        val reminder2 = ReminderDTO("Vacation", "Take Photos", "Eiffel Tower", 48.86, 2.29)
        repository.saveReminder(reminder2)
        val reminder3 = ReminderDTO("Trip", "Buy tickets", "Statue of Liberty", 40.69, -74.04)
        repository.saveReminder(reminder3)

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.reminderssRecyclerView)).perform(ViewActions.swipeDown())
        delay(1000)

        activityScenario.close()

    }


    @Test
    fun deleteAllReminders() = runBlocking {

        val reminder1 = ReminderDTO("Tour", "Sight Seeing", "Big Ben", 51.5, 0.12)
        repository.saveReminder(reminder1)
        val reminder2 = ReminderDTO("Vacation", "Take Photos", "Eiffel Tower", 48.86, 2.29)
        repository.saveReminder(reminder2)
        val reminder3 = ReminderDTO("Trip", "Buy tickets", "Statue of Liberty", 40.69, -74.04)
        repository.saveReminder(reminder3)

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.deleteList)).perform(click())

        onView(withText("OK")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed())).perform(click())

        activityScenario.close()

    }

    @Test
    fun logout() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.logout)).perform(click())

        onView(withText("OK")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed())).perform(click())

        activityScenario.close()
    }


    /**
     * Login is tested only when the app has been logged out
    */

    @Test
    fun loginEmail() = runBlocking {
        val activityScenario = ActivityScenario.launch(AuthenticationActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.etEmailAddress)).perform(replaceText("tester2@gmail.com"))
        onView(withId(R.id.etPassword)).perform(replaceText("password2"))

        onView(withId(R.id.login_button)).perform(click())

        activityScenario.close()
    }



}
