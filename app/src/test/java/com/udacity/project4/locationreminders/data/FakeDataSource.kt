package com.udacity.project4.locationreminders.data

import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError)
            return Result.Error(R.string.test_exception.toString())

            reminders?.let {return Result.Success(ArrayList(it))
        }
        return Result.Error(R.string.reminders_not_found.toString())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = reminders?.find { it.id == id }
        if (reminder != null){
            return Result.Success(reminder)
        } else {
            return Result.Error("Reminder ${id} not found")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    override suspend fun deleteReminder(id: String) {
        val reminder = reminders?.find { it.id == id }
        reminders?.remove(reminder)
    }

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }


}