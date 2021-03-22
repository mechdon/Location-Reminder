package com.udacity.project4.utils

class Utils {

    companion object {
        fun isLoginDetailsValid(email: String, password: String): Boolean {
            return email.isNotEmpty() && password.isNotEmpty()
        }

        fun isRegisterDetailsValid(name: String, email: String, password: String): Boolean {
            return name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
        }

        fun isReminderDetailsValid(title: String, description: String): Boolean {
            return title.isNotEmpty() && description.isNotEmpty()
        }

    }

}