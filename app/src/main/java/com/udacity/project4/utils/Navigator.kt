package com.udacity.project4.utils

import android.app.Activity
import android.content.Intent
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.locationreminders.RemindersActivity

object Navigator {

    fun navigateToRemindersActivity(activity: Activity) {
        activity.apply {
            startActivity(Intent(this, RemindersActivity::class.java))
            finish()
        }
    }

    fun navigateToAuthenticationActivity(activity: Activity) {
        activity.apply {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finishAffinity()
        }
    }
}
