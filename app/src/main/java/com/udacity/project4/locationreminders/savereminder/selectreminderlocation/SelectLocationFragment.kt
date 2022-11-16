package com.udacity.project4.locationreminders.savereminder.selectreminderlocation
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "SaveReminderFragment"

        private const val FINE_LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TURN_DEVICE_LOCATION_ON_REQUEST_CODE = 29

        private val DEFAULT_LAT_LNG = LatLng(-34.0, 151.0)  // Sydney
    }

    enum class MapZoomLevel(val level: Float) {
        World(1f),
        Landmass(5f),
        City(10f),
        Streets(15f),
        Buildings(20f)
    }

    override val viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private var marker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnSave.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {

        marker?.let {
            viewModel.reminderSelectedLocationStr.value = it.title
            viewModel.latitude.value = it.position.latitude
            viewModel.longitude.value = it.position.longitude
        }

        findNavController().popBackStack()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
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
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // map marker
        map.setOnMapLongClickListener { latLng ->
            addMapMarker(latLng)
            marker!!.showInfoWindow()
        }

        // poi marker
        map.setOnPoiClickListener { poi ->
            addPoiMarker(poi)
            marker!!.showInfoWindow()
        }

        // map style
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(
            requireContext(),
            R.raw.map_style))

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            enableMapMyLocation()

        } else {

            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                FINE_LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun addPoiMarker(poi: PointOfInterest) {
        marker?.remove()
        map.clear()
        marker = map.addMarker(MarkerOptions()
            .position(poi.latLng)
            .title(poi.name)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )

        val circleOptions = CircleOptions().apply {
            center(poi.latLng)
            radius(100.0)
            strokeColor(Color.argb(255, 255, 99, 105))
            strokeWidth(2f)
            fillColor(Color.argb(60, 255, 99, 105))

        }
        map.addCircle(circleOptions)
    }

    private fun addMapMarker(latLng: LatLng) {
        val snippet = String.format(
            Locale.getDefault(),
            "Lat: %1$.5f, Long: %2$.5f",
            latLng.latitude,
            latLng.longitude
        )

        marker?.remove()
        map.clear()

        marker = map.addMarker(MarkerOptions()
            .position(latLng)
            .title(getString(R.string.dropped_pin))
            .snippet(snippet)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        val circleOptions = CircleOptions().apply {
            center(latLng)
            radius(100.0)
            strokeColor(Color.argb(255, 255, 99, 105))
            strokeWidth(2f)
            fillColor(Color.argb(60, 255, 99, 105))

        }
        map.addCircle(circleOptions)
    }

    @SuppressLint("MissingPermission")
    private fun enableMapMyLocation() {
        map.isMyLocationEnabled = true
        addLastLocationCallback()
    }

    @SuppressLint("MissingPermission")
    private fun addLastLocationCallback() {

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val lastLocationTask = fusedLocationProviderClient.lastLocation

        lastLocationTask.addOnCompleteListener(requireActivity()) { task ->

            if(task.isSuccessful) {
                val taskResult = task.result
                taskResult?.run {

                    val latLng = LatLng(latitude, longitude)
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            MapZoomLevel.Streets.level
                        )
                    )

                    addMapMarker(latLng)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == FINE_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                enableMapMyLocation()

            } else {
                viewModel.showSnackBarInt.value = R.string.permission_denied_explanation
            }
        }
    }
}