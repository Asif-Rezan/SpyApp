package com.asifrezan.notificationreader.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.asifrezan.notificationreader.databinding.ActivitySignInBinding
import com.asifrezan.notificationreader.utils.PreferenceUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    lateinit var binding: ActivitySignInBinding
    private lateinit var database: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.loginButton.setOnClickListener {
            val progressBar = binding.progressBar
            progressBar.visibility = View.VISIBLE
            binding.errorMsg.text = ""

            if (!::database.isInitialized) {
                database = FirebaseDatabase.getInstance().getReference("Users")
            }

            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            CoroutineScope(Dispatchers.IO).launch {


                database.child(username)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            progressBar.visibility = View.GONE

                            if (snapshot.exists()) {
                                val storedPassword =
                                    snapshot.child("password").getValue(String::class.java)
                                if (password == storedPassword) {
                                    // Successful login
                                    binding.errorMsg.text = "Successfully logged in"
                                    PreferenceUtils.saveString(
                                        this@SignInActivity,
                                        "username",
                                        username
                                    )
                                } else {
                                    // Incorrect password
                                    binding.errorMsg.text = "Incorrect password"
                                }
                            } else {
                                // No user found
                                binding.errorMsg.text = "No user found"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            progressBar.visibility = View.GONE
                            // Handle cancellation or errors, if needed
                        }
                    })
            }
        }


    }
}