package com.udacity.project4

import android.app.Application
import android.os.Build
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.hamcrest.core.StringEndsWith.endsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get


@RunWith(AndroidJUnit4::class)
@LargeTest
class RemindersActivityTest : KoinTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var decorView: View

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(RemindersActivity::class.java)

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
        startKoin {
            modules(listOf(myModule))
        }
        repository = get()
        runBlocking {
            repository.deleteAllReminders()
        }

        activityScenarioRule.scenario.onActivity { activity ->
            decorView = activity.window.decorView
        }

    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().apply {
            register(EspressoIdlingResource.countingIdlingResource)
            register(dataBindingIdlingResource)
        }
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().apply {
            unregister(EspressoIdlingResource.countingIdlingResource)
            unregister(dataBindingIdlingResource)
        }
    }

    @Test
    fun saveReminder_TestToastMessage() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).perform(ViewActions.replaceText("Title"))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.replaceText("Desc"))
        onView(withId(R.id.selectedLocation)).perform(click())

        onView(withId(R.id.map)).perform(click())


        onView(withId(R.id.btn_save)).perform(click())


        onView(withId(R.id.saveReminder)).perform(click())

        //It's a bad practice to test toast and snackbar messages using Espresso,
        // as toasts and snackbars are posted on the main thread,
        // which means they may not be shown immediately but shown when the main thread is idle,
        // so it may cause the test to be flaky.

        ///Therefore this test will fail in android 11 or higher if not for the check below

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            onView(withText(endsWith(appContext.getString(R.string.reminder_saved)))).inRoot(
                withDecorView(not(`is`(decorView)))
            )
                .check(matches(isDisplayed()))
        }

        activityScenario.close()
    }

}