package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.launch
import org.koin.core.logger.KOIN_TAG

class RemindersListViewModel(
    app: Application,
    private val dataSource: ReminderDataSource
) : BaseViewModel(app) {
    // list that holds the reminder data to be displayed on the UI
    val remindersList = MutableLiveData<List<ReminderDataItem>>()

    private val _reminder = MutableLiveData<ReminderDataItem>()
    val reminder: MutableLiveData<ReminderDataItem> get() = _reminder

    private val _eventShowToast = MutableLiveData<Boolean>()
    val eventShowToast: MutableLiveData<Boolean> get() = _eventShowToast

    private val _toastMsg = MutableLiveData<String>()
    val toastMsg: MutableLiveData<String> get() = _toastMsg


    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */
    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {
            //interacting with the dataSource has to be through a coroutine
            val result = dataSource.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    val dataList = ArrayList<ReminderDataItem>()
                    dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                        //map the reminder data from the DB to the be ready to be displayed on the UI
                        ReminderDataItem(
                            reminder.title,
                            reminder.description,
                            reminder.location,
                            reminder.latitude,
                            reminder.longitude,
                            reminder.id
                        )
                    })
                    remindersList.value = dataList
                }
                is Result.Error ->
                    showSnackBar.value = result.message
            }

            //check if no data has to be shown
            invalidateShowNoData()
        }
    }

    fun getReminder(id: String){
        viewModelScope.launch {
           val result = dataSource.getReminder(id)

            when (result) {
                is Result.Success<*> -> {
                    val rem = result.data as ReminderDTO
                    _reminder.value = ReminderDataItem(rem.title, rem.description, rem.location, rem.latitude, rem.longitude, rem.id)
                    Log.d(KOIN_TAG, "reminder is "+_reminder.value)
                }
                is Result.Error ->
                    showSnackBar.value = result.message
            }
        }

    }

    fun deleteReminder(id: String){
        viewModelScope.launch {
            dataSource.deleteReminder(id)
        }
    }


    fun deleteAllReminders(){
        viewModelScope.launch {
            dataSource.deleteAllReminders()
        }
    }

    /**
     * Inform the user that there's not any data if the remindersList is empty
     */
    private fun invalidateShowNoData() {
        showNoData.value = remindersList.value == null || remindersList.value!!.isEmpty()
    }
}