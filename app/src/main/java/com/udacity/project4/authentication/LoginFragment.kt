package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.databinding.FragmentLoginBinding



class LoginFragment : Fragment() {

    // Get a reference to the ViewModel scoped to this Fragment
    private val TAG = LoginFragment::class.java.simpleName
    private val SIGN_IN_RESULT_CODE = 1001
    private val viewModel: LoginViewModel by activityViewModels()
    private lateinit var binding: FragmentLoginBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(context!!, gso)


        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.lifecycleOwner = this
        binding.loginViewModel = viewModel


        viewModel.eventClick.observe(viewLifecycleOwner, { isLoginClicked ->
            if (isLoginClicked) {
                viewModel.loginWithEmail()
            }
        })

        viewModel.usrNotRegistered.observe(viewLifecycleOwner, { usrNotRegistered ->
            if (usrNotRegistered){
                val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
                findNavController().navigate(action)
            }

        })

        viewModel.eventShowErrorSnackbar.observe(viewLifecycleOwner, { eventError ->
            if (eventError) {

                if (viewModel.errCode.value == 1){
                    Snackbar.make(
                            binding.emailEt,
                            getString(R.string.please_fill_all_details),
                            Snackbar.LENGTH_SHORT
                    ).show()
                    viewModel.onSnackbarComplete()

                    }
                }

            if (viewModel.errCode.value == 2){
                Snackbar.make(
                        binding.passwordEt,
                        getString(R.string.valid_password),
                        Snackbar.LENGTH_SHORT
                ).show()
                viewModel.onSnackbarComplete()
            }

        })

        binding.googleButton.setOnClickListener {
            googleSignIn()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthenticationState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SIGN_IN_RESULT_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // User successfully signed in
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:" + account!!.id)
                viewModel.firebaseAuthWithGoogle(account.idToken!!)

            } catch (e: ApiException){
                // Sign in failed
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }




    private fun googleSignIn(){
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, SIGN_IN_RESULT_CODE)
    }


    private fun observeAuthenticationState() {
        // Use authentication state fom LoginViewModel to update the UI accordingly
        viewModel.authenticationState.observe(viewLifecycleOwner, Observer { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                    // Navigate to Reminder Screen
                    val action = LoginFragmentDirections.actionLoginFragmentToReminderListFragment()
                    findNavController().navigate(action)
                    viewModel.onLoginComplete()

                }
            }
        })

    }





}