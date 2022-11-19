package com.udacity.project4.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
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

    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        permissions.forEach { permission ->
            if (!isPermissionGranted(context, permission)) {
                return false
            }
        }
        return true
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

    fun requestPermissions(
        fragment: Fragment,
        permissions: Array<String>,
        onResultAction: (Map<String, Boolean>) -> Unit
    ) {
        fragment.registerForActivityResult(RequestMultiplePermissions()) {
            onResultAction(it)
        }.launch(permissions)
    }

    fun requestPermissionss(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ) {
       ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
}
