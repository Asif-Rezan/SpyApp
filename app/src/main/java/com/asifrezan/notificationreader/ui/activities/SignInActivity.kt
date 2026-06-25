package com.asifrezan.notificationreader.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.asifrezan.notificationreader.databinding.ActivitySignInBinding
import android.content.Intent
import com.asifrezan.notificationreader.utils.PreferenceUtils
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            PreferenceUtils.saveString(this, PreferenceUtils.USER_ID_KEY, auth.currentUser?.uid.orEmpty())
            openMain()
            return
        }

        binding.registerTextView.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }

        binding.forgotPasswordTextView.setOnClickListener {
            val email = binding.usernameEditText.text.toString().trim()
            if (email.isEmpty()) {
                binding.errorMsg.text = "Enter your email first"
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    binding.errorMsg.text = "Password reset email sent"
                }
                .addOnFailureListener {
                    binding.errorMsg.text = "Reset failed: ${it.message}"
                }
        }

        binding.loginButton.setOnClickListener {
            val progressBar = binding.progressBar
            progressBar.visibility = View.VISIBLE
            binding.errorMsg.text = ""

            val email = binding.usernameEditText.text.toString().trim() // Assuming username field is used for email
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                binding.errorMsg.text = "Please enter email and password"
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        PreferenceUtils.saveString(this, PreferenceUtils.USER_ID_KEY, task.result.user?.uid.orEmpty())
                        openMain()
                    } else {
                        binding.errorMsg.text = "Login failed: ${task.exception?.message}"
                    }
                }
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
