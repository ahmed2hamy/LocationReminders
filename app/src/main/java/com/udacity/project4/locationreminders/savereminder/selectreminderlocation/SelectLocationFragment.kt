package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.LocationGetter
import com.udacity.project4.utils.PermissionUtils
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.truncate
import org.koin.android.ext.android.inject
import timber.log.Timber


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback, MenuProvider {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var locationGetter: LocationGetter

    private lateinit var map: GoogleMap
    private var marker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_select_location, container, false
        )
        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setDisplayHomeAsUpEnabled(true)

        locationGetter = LocationGetter(requireContext())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkPermissions()

        binding.btnSave.setOnClickListener {
            if (marker == null) {
                showSnackBar(getString(R.string.select_poi))
            } else {
                _viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requestPermissionsAgain()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onMenuItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> false
    }

    private fun checkPermissions() {
        if (isPermissionApproved()) {
            checkDeviceLocationSettings()
        } else {
            requestLocationPermission()
        }
    }
    private fun requestPermissionsAgain() {
        if (isPermissionApproved()) {
            checkDeviceLocationSettings()
        } else {
            recreateFragment()
        }
    }

    private fun recreateFragment() {
        parentFragmentManager.beginTransaction().detach(this).attach(this).commit()
    }

    private fun isPermissionApproved(): Boolean {
        return PermissionUtils.isPermissionGranted(
            requireContext(), requiredPermission
        )
    }

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {

        locationGetter.checkDeviceLocationSettings(requireActivity(),
            REQUEST_TURN_DEVICE_LOCATION_ON,
            resolve,
            {
                enableMyLocation()

                locationGetter.getLastLocation {
                    map.setPadding(0, 0, 0, 200)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude), 15.0f
                        )
                    )

                    addMarkerCircle(LatLng(it.latitude, it.longitude))
                }
            },
            {
                showSnackBar(
                    getString(R.string.location_required_error),
                    Snackbar.LENGTH_INDEFINITE,
                    android.R.string.ok
                ) {
                    checkDeviceLocationSettings()
                }
            })


    }

    private fun requestLocationPermission() {
        if (isPermissionApproved()) return

        Timber.tag(TAG).d("Requesting location permission")

        PermissionUtils.requestPermission(
            this, requiredPermission
        ) { isGranted ->
            if (isGranted) {
                checkDeviceLocationSettings()
            } else {
                // Permission denied.
                showSnackBar(
                    getString(R.string.permission_denied_explanation),
                    Snackbar.LENGTH_INDEFINITE,
                    R.string.settings,
                ) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettings(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (locationGetter.isLocationEnabled()) {
            map.uiSettings.isZoomControlsEnabled = true
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }

        map.setOnMyLocationButtonClickListener {
            checkDeviceLocationSettings()
            true
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapStyle()
        initMapView()


        map.setOnPoiClickListener {
            marker?.remove()
            map.clear()

            val locationSnippet = it.name
            _viewModel.updateSelectedLocation(it.latLng, locationSnippet, it)

            marker = map.addMarker(
                MarkerOptions().position(it.latLng).title(it.name).snippet(locationSnippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            addMarkerCircle(it.latLng)

            marker?.showInfoWindow()
        }

        map.setOnMapClickListener {
            marker?.remove()
            map.clear()

            val locationSnippet = "${it.latitude.truncate(6)}, ${it.longitude.truncate(6)}"
            _viewModel.updateSelectedLocation(it, locationSnippet)

            marker = map.addMarker(
                MarkerOptions().position(it).title(getString(R.string.dropped_pin))
                    .snippet(locationSnippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            addMarkerCircle(it)
            marker?.showInfoWindow()
        }
    }

    private fun addMarkerCircle(center: LatLng) {
        val circleOptions = CircleOptions().apply {
            center(center)
            radius(GeofencingConstants.GEOFENCE_RADIUS_IN_METERS)
            strokeColor(Color.argb(255, 255, 99, 105))
            strokeWidth(2f)
            fillColor(Color.argb(60, 255, 99, 105))

        }
        map.addCircle(circleOptions)
    }

    private fun initMapView() {
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val locationSnippet = _viewModel.reminderSelectedLocationStr.value

        if (latitude != null && longitude != null && locationSnippet != null) {
            val latLng = LatLng(latitude, longitude)
            marker = map.addMarker(
                MarkerOptions().position(latLng).title(getString(R.string.dropped_pin))
                    .snippet(locationSnippet)
            )
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 15.0f)
            )
        } else if (PermissionUtils.isPermissionGranted(requireContext(), requiredPermission)) {
            locationGetter.getLastLocation {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15.0f)
                )
            }
        }
    }

    private fun setMapStyle() {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )

            if (!success) {
                Timber.tag(TAG).e("Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Timber.tag(TAG).e(e, "Can't find style. Error: ")
        }
    }


    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

        val requiredPermission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            else -> Manifest.permission.ACCESS_FINE_LOCATION
        }
    }
}