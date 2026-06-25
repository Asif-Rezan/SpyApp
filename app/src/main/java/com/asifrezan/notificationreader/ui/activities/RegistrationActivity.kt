package com.asifrezan.notificationreader.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.asifrezan.notificationreader.data.models.Users
import com.asifrezan.notificationreader.databinding.ActivityRegistrationBinding
import android.content.Intent
import com.asifrezan.notificationreader.utils.PreferenceUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegistrationActivity : AppCompatActivity() {
    lateinit var binding: ActivityRegistrationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        binding.loginTextView.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        binding.registerButton.setOnClickListener {
            val progressBar = binding.progressBar
            progressBar.visibility = View.VISIBLE
            binding.errorMsg.text = ""

            val username = binding.userNameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            val address = binding.addressEditText.text.toString().trim()
            val gender = binding.genderEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                binding.errorMsg.text = "Please fill all required fields"
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.errorMsg.text = "Password must be at least 6 characters"
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val user = Users(
                            username = username,
                            email = email,
                            phone = phone,
                            address = address,
                            gender = gender
                        )
                        
                        if (userId != null) {
                            database.child(userId).setValue(user).addOnSuccessListener {
                                PreferenceUtils.saveString(this, PreferenceUtils.USER_ID_KEY, userId)
                                progressBar.visibility = View.GONE
                                openMain()
                            }.addOnFailureListener {
                                progressBar.visibility = View.GONE
                                binding.errorMsg.text = "Failed to save user data"
                            }
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        binding.errorMsg.text = "Authentication failed: ${task.exception?.message}"
                    }
                }
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
