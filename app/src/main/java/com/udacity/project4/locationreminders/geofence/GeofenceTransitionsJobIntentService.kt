package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private val TAG = "GeofenceTJIS"

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    private lateinit var geofenceClient: GeofencingClient

    companion object {
        private const val JOB_ID = 573

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            geofenceClient = LocationServices.getGeofencingClient(applicationContext)

            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent!!.hasError()) {
                Timber.tag(TAG).e(geofencingEvent.errorCode.toString())
                return
            }
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                if (geofencingEvent.triggeringGeofences!!.isNotEmpty())
                    sendNotificationAndRemoveGeoFences(geofencingEvent.triggeringGeofences!!)
                else
                    Timber.e("No Geofence Trigger Found")
            }
        }
    }

    private fun sendNotificationAndRemoveGeoFences(triggeringGeoFences: List<Geofence>) {
        val remindersLocalRepository: ReminderDataSource by inject()
        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            triggeringGeoFences.forEach {
                val result = remindersLocalRepository.getReminder(it.requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    //send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
                geofenceClient.removeGeofences(triggeringGeoFences.map { geofence -> geofence.requestId })
            }
        }
    }

}