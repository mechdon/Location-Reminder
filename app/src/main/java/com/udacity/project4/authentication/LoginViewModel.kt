package com.udacity.project4.authentication

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.udacity.project4.utils.Utils
import com.udacity.project4.R

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = LoginViewModel::class.java.simpleName

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _personName = MutableLiveData<String>()
    val personName: MutableLiveData<String> get() = _personName

    private val _emailAddress = MutableLiveData<String>()
    val emailAddress: MutableLiveData<String> get() = _emailAddress

    private val _password = MutableLiveData<String>()
    val password: MutableLiveData<String> get() = _password

    private val _eventClick= MutableLiveData<Boolean>()
    val eventClick: LiveData<Boolean> get() = _eventClick

    private val _eventLogin = MutableLiveData<Boolean>()
    val eventLogin: LiveData<Boolean> get() = _eventLogin

    private val _eventRegister = MutableLiveData<Boolean>()
    val eventRegister: LiveData<Boolean> get() = _eventRegister

    private val _usrNotRegistered = MutableLiveData<Boolean>()
    val usrNotRegistered: LiveData<Boolean> get() = _usrNotRegistered

    private val _errCode = MutableLiveData<Int>()
    val errCode: LiveData<Int> get() = _errCode

    private val _eventShowErrorSnackbar = MutableLiveData<Boolean>()
    val eventShowErrorSnackbar: LiveData<Boolean> get() = _eventShowErrorSnackbar

    init {
        _personName.value = ""
        _emailAddress.value = ""
        _password.value = ""
    }

    fun onLogin() {
        if (Utils.isLoginDetailsValid(_emailAddress.value.toString(), _password.value.toString()))
            _eventClick.value = true
        else
            _errCode.value = 1
            _eventShowErrorSnackbar.value = true
    }

    fun onRegister() {
        if (Utils.isRegisterDetailsValid(_personName.value.toString(), _emailAddress.value.toString(), _password.value.toString()))
        {
            _eventRegister.value = true
        } else {
            _errCode.value = 1
            _eventShowErrorSnackbar.value = true
        }
    }

    fun onSnackbarComplete() {
        _errCode.value = 0
        _eventShowErrorSnackbar.value = false
    }

    fun onLoginComplete() {
        _eventClick.value = false
        _eventLogin.value = true
        _emailAddress.value = ""
        _password.value = ""
    }

    fun onRegisterComplete() {
        _eventRegister.value = false
        _personName.value = ""
        _emailAddress.value = ""
        _password.value = ""
    }

    fun loginWithEmail(){
        firebaseAuth.signInWithEmailAndPassword(emailAddress.value, password.value)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        _eventLogin.value = true

                    } else {

                        if (task.exception.toString().contentEquals(getNoUserString().toString())){

                            _usrNotRegistered.value = true

                        } else if (task.exception.toString().contentEquals(getInvalidPassword().toString())){

                            _errCode.value = 2
                            _eventShowErrorSnackbar.value = true

                        }

                    }
                }

    }

    fun registerWithEmail(){
        firebaseAuth.createUserWithEmailAndPassword(emailAddress.value, password.value)
                .addOnCompleteListener {  task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Email Registration Success")

                        val user = firebaseAuth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder().apply {
                            displayName = _personName.value
                        }.build()
                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {  task ->
                            if (task.isSuccessful){
                                _eventLogin.value = true

                            } else {
                                Log.w(TAG, "createUserWithEmail: failure", task.exception)

                                _errCode.value = 3
                                _eventShowErrorSnackbar.value = true
                            }
                        }
                    }
                }
    }



    fun firebaseAuthWithGoogle(idToken: String){
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener{ task ->
                    if (task.isSuccessful){
                        _eventLogin.value = true
                        Log.d(TAG, "signInWithCredential: success")

                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                    }
                }
    }

    fun logout(){
         FirebaseAuth.getInstance().signOut()
        _eventLogin.value = false
    }

    fun getNoUserString(): String? {
        return getApplication<Application>().resources.getString(R.string.no_user)
    }

    fun getInvalidPassword(): String? {
        return getApplication<Application>().resources.getString(R.string.invalid_password)
    }


    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null){
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}