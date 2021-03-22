package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private val TAG = SaveReminderFragment::class.simpleName
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private var geofenceEnable = false
    private var editEnable = false
    private var reminder: ReminderDataItem? = null
    private var selectedReminder: ReminderDataItem? = null
    private var selectedReminderId = ""


    companion object {
        internal const val ACTION_GEOFENCE_EVENT = "SaveReminderFragment.locationreminders.action.ACTION_GEOFENCE_EVENT"
    }

    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel


        _viewModel.eventGeofence.observe(viewLifecycleOwner, { isGeofenceEnabled ->
            if (isGeofenceEnabled){
                geofenceEnable = true
            }
        })



        _viewModel.eventEdit.observe(viewLifecycleOwner, { isEditEnabled ->
            if (isEditEnabled){
                editEnable = true
                if (_viewModel.reminder.value != null){

                    selectedReminder = _viewModel.reminder.value
                    binding.reminderTitle.setText(selectedReminder!!.title)
                    binding.reminderDescription.setText(selectedReminder!!.description)
                    binding.selectedLocation.setText(selectedReminder!!.location)
                    _viewModel.reminderSelectedLocationStr.value = selectedReminder!!.location
                    _viewModel.latitude.value = selectedReminder!!.latitude
                    _viewModel.longitude.value = selectedReminder!!.longitude
                    selectedReminderId = selectedReminder!!.id
                }

            }
        })


        return binding.root
    }
    

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            if (_viewModel.eventEdit.value == true) {
                // If edit mode is On
                reminder = ReminderDataItem(title, description, location, latitude, longitude, selectedReminderId)
            } else {
                // If create new reminder
                reminder = ReminderDataItem(title, description, location, latitude, longitude)
            }

            // Validate entered data
            _viewModel.validateEnteredData(reminder!!)

            // If edit mode, remove previous geofence
            if (editEnable){
                var gfIds: MutableList<String> = mutableListOf()
                gfIds.add(selectedReminderId)

                geofencingClient.removeGeofences(gfIds).run { }
            }

            // Add geofencing request

            if (reminder != null && geofenceEnable){
                val geofence = Geofence.Builder()
                    .setRequestId(reminder!!.id)
                    .setCircularRegion(reminder!!.latitude!!,
                        reminder!!.longitude!!, GEOFENCE_RADIUS_IN_METERS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()

                val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()


                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        Toast.makeText(requireContext(), R.string.geofence_added, Toast.LENGTH_SHORT).show()

                        // Save reminder to local db
                        _viewModel.saveReminder(reminder!!)
                        _viewModel.onEditDisable()

                    }
                    addOnFailureListener{
                        Toast.makeText(requireContext(), R.string.geofences_not_added, Toast.LENGTH_SHORT).show()
                        if (it.message != null){
                            Log.w(TAG, it.message!!)
                        }
                    }
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
