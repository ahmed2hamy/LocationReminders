package com.udacity.project4.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        val deviceVersion = Build.VERSION.SDK_INT
        return if (deviceVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestPermission(
        fragment: Fragment,
        permission: String,
        onResultAction: (Boolean) -> Unit
    ) {
        fragment.registerForActivityResult(RequestPermission()) {
            onResultAction(it)
        }.launch(permission)
    }
}
