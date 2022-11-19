package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val args by navArgs<SaveReminderFragmentArgs>()
    private lateinit var reminderData: ReminderDataItem

    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = GeofencingConstants.ACTION_GEOFENCE_EVENT
        val flags: Int = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else -> PendingIntent.FLAG_UPDATE_CURRENT
        }

        PendingIntent.getBroadcast(
            requireContext(),
            1,
            intent,
            flags
        )

}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
}

override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
): View {
    binding = DataBindingUtil.inflate(
        inflater,
        R.layout.fragment_save_reminder,
        container,
        false
    )
    binding.lifecycleOwner = this
    binding.viewModel = _viewModel

    setDisplayHomeAsUpEnabled(true)

    initListeners()

    geofencingClient = LocationServices.getGeofencingClient(requireContext())

    return binding.root
}

private fun initView() {
    args.reminderData?.let {
        reminderData = it
        _viewModel.apply {
            reminderTitle.postValue(it.title)
            reminderDescription.postValue(it.description)
            reminderSelectedLocationStr.postValue(it.location)
            latitude.postValue(it.latitude)
            longitude.postValue(it.longitude)
        }
    }
}

private fun initListeners() {
    binding.selectedLocation.setOnClickListener {
        _viewModel.navigationCommand.value = NavigationCommand.To(
            SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        )
    }

    binding.saveReminder.setOnClickListener {
        if (!::reminderData.isInitialized) {
            reminderData = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            )
        } else {
            reminderData.apply {
                title = _viewModel.reminderTitle.value
                description = _viewModel.reminderDescription.value
                location = _viewModel.reminderSelectedLocationStr.value
                latitude = _viewModel.latitude.value
                longitude = _viewModel.longitude.value
            }
        }
        if (_viewModel.validateAndSaveReminder(reminderData)) {
            addReminderGeofence(
                reminderData.latitude!!,
                reminderData.longitude!!,
                reminderData.id
            )
        }
    }
}

override fun onDestroy() {
    super.onDestroy()
    _viewModel.onClear()
}

@SuppressLint("MissingPermission")
private fun addReminderGeofence(latitude: Double, longitude: Double, reminderId: String) {
    val geofence = Geofence.Builder()
        .setRequestId(reminderId)
        .setCircularRegion(
            latitude,
            longitude,
            GeofencingConstants.GEOFENCE_RADIUS_IN_METERS.toFloat()
        )
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .build()

    val geofencingRequest = GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()

    geofencingClient.removeGeofences(geofencePendingIntent).run {
        addOnCompleteListener {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Timber.run {
                        tag(TAG).d("Added geofence for reminder with id $reminderId successfully.")
                    }
                }
                addOnFailureListener {
                    _viewModel.showSnackBarInt.postValue(R.string.error_adding_geofence)
                    it.message?.let { message ->
                        Timber.tag(TAG).w(message)
                    }
                }
            }
        }
    }
}

companion object {
    private val TAG = SaveReminderFragment::class.java.simpleName
}
}
