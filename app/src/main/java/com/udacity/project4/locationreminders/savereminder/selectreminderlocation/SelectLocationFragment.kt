package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.TargetApi
import android.app.Service
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.net.ConnectivityManagerCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private val TAG = SelectLocationFragment::class.java.simpleName
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedMarker: Marker? = null
    private var iMarker: Marker? = null
    private var selectedLocation = ""
    private var selectedLatLng: LatLng? = null
    private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    private val LOCATION_PERMISSION_INDEX = 0
    private val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    private var snackbar: Snackbar? = null
    private var connectivity: ConnectivityManager? = null
    private var info: NetworkInfo? = null
    private var connected: Boolean = false
    private var addressStr = ""


    private val runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())


        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveLocationButton.setOnClickListener {
            // Call function after the user confirms on the selected location
            _viewModel.onGeofenceEnable()
            onLocationSelected()
        }

        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectivity = requireActivity().getSystemService(Service.CONNECTIVITY_SERVICE)
                        as ConnectivityManager

        if (connectivity != null){

            info = connectivity!!.activeNetworkInfo
            if (info != null){
                if (info!!.state == NetworkInfo.State.CONNECTED){
                    Log.d(TAG, "CONNECTED")
                    connected = true
                } else {
                    Log.d(TAG, "NOT CONNECTED")
                    connected = false
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        snackbar!!.dismiss()
    }



    private fun onLocationSelected() {
        // Pass selected location details to view model
        _viewModel.reminderSelectedLocationStr.value = selectedLocation
        _viewModel.latitude.value = selectedLatLng!!.latitude
        _viewModel.longitude.value = selectedLatLng!!.longitude

        // Navigate back to previous fragment
        activity?.onBackPressed()

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Selection between map types
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

        if (foregroundAndBackgroundLocationPermissionApproved()){
            checkDeviceLocationSettings()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }

        showMyLocation()


        snackbar = Snackbar.make(
                binding.fragmentMaps,
                R.string.instruct_select_location, Snackbar.LENGTH_INDEFINITE
        ).setAction(android.R.string.ok) {
        }

        snackbar!!.show()


        setMapClick(map)
        setPoiClick(map)

        setMapStyle(map)

    }

    private fun checkDeviceLocationSettings(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
                settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(requireActivity(),
                            REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                        binding.fragmentMaps,
                        R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                showMyLocation()
            }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireActivity(),
                                Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
                if (runningQOrLater) {
                    PackageManager.PERMISSION_GRANTED ==
                            ActivityCompat.checkSelfPermission(
                                    requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                } else {
                    true
                }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {

        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {

                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }


        requestPermissions(
                permissionsArray,
                resultCode
        )

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettings(false)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {

        if (
                grantResults.isEmpty() ||
                grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
                (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                        grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                        PackageManager.PERMISSION_DENIED))
        {
            // Permission denied.
            Snackbar.make(
                    binding.fragmentMaps,
                    R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                    .setAction(R.string.settings) {
                        // Displays App settings screen.
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
        } else {
            checkDeviceLocationSettings()
        }
    }



    private fun placeUserMarkerOnMap(location: LatLng) {

        if (iMarker != null){
            iMarker!!.remove()
        }

        val markerOptions = MarkerOptions()
                .title("Me")
                .position(location)

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.mipmap.ic_user_location)))

        var myMarker = map.addMarker(markerOptions)
        iMarker = myMarker

    }


    private fun showMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestForegroundAndBackgroundLocationPermissions()
            return
        }
        map.isMyLocationEnabled = true
        map.setMyLocationEnabled(true)

        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
            if (location != null){
                val currLatLng = LatLng(location.latitude, location.longitude)
                placeUserMarkerOnMap(currLatLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currLatLng, 17f))
            }
        }
    }




    // Set Poi Click
    private fun setPoiClick(map: GoogleMap){
        map.setOnPoiClickListener {  poi ->

            if (selectedMarker != null) {
                selectedMarker!!.remove()
            }

            selectedLatLng = poi.latLng

            val snippet = String.format(
                    Locale.getDefault(),
                    "lat: %1$.5f, Long: %2$.5f",
                    poi.latLng.latitude,
                    poi.latLng.longitude
            )

            selectedLocation = poi.name

            var poiMarker = map.addMarker(
                    MarkerOptions()
                            .position(poi.latLng)
                            .title(selectedLocation)
                            .snippet(snippet)
            )

            poiMarker.showInfoWindow()

            snackbar!!.dismiss()

            binding.saveLocationButton.isVisible = true

            selectedMarker = poiMarker

        }

    }


    // Set Map Click
    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->

            if (getAddressFromLocation(latLng) != null){

                if (selectedMarker != null){
                    selectedMarker!!.remove()
                }

                selectedLatLng = latLng
                selectedLocation = getAddressFromLocation(latLng)

                val snippet = String.format(
                        Locale.getDefault(),
                        "lat: %1$.5f, Long: %2$.5f",
                        latLng.latitude,
                        latLng.longitude
                )

                var sMarker = map.addMarker(
                        MarkerOptions()
                                .position(latLng)
                                .title(selectedLocation)
                                .snippet(snippet)
                )

                sMarker.showInfoWindow()

                snackbar!!.dismiss()

                binding.saveLocationButton.isVisible = true

                selectedMarker = sMarker
            }
        }

    }


    // Set Map Style
    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(),
                    R.raw.map_style))
            if (!success){
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException){
            Log.e(TAG, "Can't find style. Error: ", e)
        }

    }


    private fun getAddressFromLocation(latLng: LatLng): String {


        if (connected) {

            try {

                var geocoder: Geocoder
                var addressList = ArrayList<Address>()

                geocoder = Geocoder(requireContext(), Locale.ENGLISH)

                addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) as ArrayList<Address>

                if (addressList != null) {

                    addressStr = addressList.get(0).getAddressLine(0)

                    return addressStr

                } else {

                    return "Selected Location"
                }

                } catch (e: IOException) {

                    Log.e(TAG, "error is "+e)
                return "Selected Location"

            } catch (e: Exception) {
                Log.e(TAG, "error is "+e)

                return "Selected Location"
            }

        }


       return "Selected Location"

    }


}
