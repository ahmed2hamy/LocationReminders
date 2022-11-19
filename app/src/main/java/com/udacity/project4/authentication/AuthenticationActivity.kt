package com.udacity.project4.authentication

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.utils.Navigator
import kotlinx.android.synthetic.main.activity_authentication.*

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    private val loginResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            val idpResponse = IdpResponse.fromResultIntent(result.data)

            if (result.resultCode == Activity.RESULT_OK) {
                Navigator.navigateToRemindersActivity(this)
            } else {
                if (idpResponse == null) {
                    showSnackBar("Login cancelled")
                } else {
                    showSnackBar(
                        idpResponse.error?.localizedMessage
                            ?: "An unknown error occurred while trying to log in."
                    )
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser != null) {
            Navigator.navigateToRemindersActivity(this)
        } else {
            setContentView(R.layout.activity_authentication)
            launchSignInFlow()
        }
    }

    private fun launchSignInFlow() {
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        btn_login.setOnClickListener {
            loginResultLauncher.launch(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setIsSmartLockEnabled(false)
                    .setAvailableProviders(providers)
                    .setLogo(R.drawable.map)
                    .build()
            )
        }
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}