package com.udacity.project4.locationreminders.reminderslist

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.authentication.LoginViewModel
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.logger.KOIN_TAG

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance
    private val loginViewModel: LoginViewModel by activityViewModels()
    private val saveReminderViewModel: SaveReminderViewModel by viewModel()
    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding
    private var adapter: RemindersListAdapter? = null
    private lateinit var geofencingClient: GeofencingClient
    var selectedReminder: ReminderDataItem? = null
    var numberReminders = 0

    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = SaveReminderFragment.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminders, container, false
            )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))


        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        loginViewModel.eventLogin.observe(viewLifecycleOwner, { isLoggedIn ->
            if (isLoggedIn){
            } else {
                navigateToLoginFragment()
            }
        })

        loginViewModel.authenticationState.observe(viewLifecycleOwner, Observer {  authenticationState ->
            when (authenticationState){
                LoginViewModel.AuthenticationState.UNAUTHENTICATED -> {
                    navigateToLoginFragment()
                }
            }
        })

        _viewModel.reminder.observe(viewLifecycleOwner, Observer {  reminder ->
            saveReminderViewModel.setSelectedReminder(reminder)
        })

        _viewModel.eventShowToast.observe(viewLifecycleOwner, Observer {  show ->
            if (show) {
                Toast.makeText(context, _viewModel.toastMsg.value, Toast.LENGTH_SHORT).show()
                _viewModel.eventShowToast.value = false
            }
        })

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }

    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun setupRecyclerView() {
        adapter = RemindersListAdapter { item ->

            _viewModel.getReminder(item.id)

            alertDeleteOrEditReminder(item.id)

        }

//        setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.deleteList -> {
                alertDeleteAll()
            }

            R.id.logout -> {
                alertLogout()
            }
        }
        return super.onOptionsItemSelected(item)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }


    fun navigateToLoginFragment(){
        findNavController().navigate(R.id.loginFragment)
    }

    fun alertLogout() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(getString(R.string.confirm_logout))
            .setCancelable(false)
            .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
                loginViewModel.logout()
                dialogInterface.dismiss()
            })

        val alert = builder.create()
        alert.setTitle("Alert")
        alert.show()
    }

    fun alertDeleteAll() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(getString(R.string.confirm_delete_all))
            .setCancelable(false)
            .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->

                // Remove geofences
                geofencingClient.removeGeofences(geofencePendingIntent).run {
                    addOnSuccessListener {
                        _viewModel.toastMsg.value = "Geofences removed"
                        _viewModel.eventShowToast.value = true
                    }
                }

                // Delete reminders
                _viewModel.deleteAllReminders()
                Toast.makeText(requireContext(), R.string.reminders_deleted, Toast.LENGTH_SHORT).show()
                adapter!!.notifyDataSetChanged()

                activity?.onBackPressed()
                dialogInterface.dismiss()
            })
        val alert = builder.create()
        alert.setTitle("Alert")
        alert.show()

    }

    fun alertDeleteOrEditReminder(reminderId: String) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(getString(R.string.delete_edit_reminder))
            .setCancelable(false)
            .setNeutralButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .setNegativeButton("Delete", DialogInterface.OnClickListener { dialogInterface, i ->
                // Remove geofence
               var gfIds: MutableList<String> = mutableListOf()
                gfIds.add(reminderId)
                geofencingClient.removeGeofences(gfIds).run {
                    addOnSuccessListener {
                        _viewModel.toastMsg.value = "Geofence removed"
                        _viewModel.eventShowToast.value = true
                    }
                }

                // Delete reminder
                _viewModel.deleteReminder(reminderId)
                Toast.makeText(requireContext(), R.string.reminder_deleted, Toast.LENGTH_SHORT).show()
                adapter!!.notifyDataSetChanged()

                activity?.onBackPressed()
                dialogInterface.dismiss()
            })
            .setPositiveButton("Edit", DialogInterface.OnClickListener { dialogInterface, i ->
                saveReminderViewModel.onEditEnable()
                navigateToAddReminder()
                dialogInterface.dismiss()
            })

        val alert = builder.create()
        alert.setTitle("Alert")
        alert.show()
    }

}
