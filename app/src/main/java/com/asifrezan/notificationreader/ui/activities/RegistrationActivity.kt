package com.asifrezan.notificationreader.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.asifrezan.notificationreader.R
import com.asifrezan.notificationreader.data.models.Users
import com.asifrezan.notificationreader.databinding.ActivityRegistrationBinding
import com.asifrezan.notificationreader.utils.PreferenceUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RegistrationActivity : AppCompatActivity() {
    lateinit var binding: ActivityRegistrationBinding
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.registerButton.setOnClickListener{
            val progressBar = binding.progressBar
            progressBar.visibility = View.VISIBLE
            binding.errorMsg.text=""

        if (!::database.isInitialized) {
            database = FirebaseDatabase.getInstance().getReference("Users")
        }

        val username = binding.userNameEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val phone = binding.phoneEditText.text.toString()
        val address = binding.addressEditText.text.toString()
        val gender = binding.genderEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        val user = Users(username, email,phone,address,gender,password)


        database.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                if (snapshot.exists()) {
                    // The username already exists, show an error message
                    binding.errorMsg.text = "Username already exists. Please choose another username."
                } else {
                    // Username doesn't exist, proceed to save the user data
                    database.child(username).setValue(user).addOnSuccessListener {
                        // User data saved successfully, save username in SharedPreferences
                        PreferenceUtils.saveString(this@RegistrationActivity, "username", username)
                    }.addOnFailureListener {
                        binding.errorMsg.text = "Something went wrong! Try again!"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                // Handle cancellation or errors, if needed
            }
        })


        }


//        database.child(username).setValue(user).addOnSuccessListener {
//            PreferenceUtils.saveString(this, "username", username)
//
//        }.addOnFailureListener{
//            binding.errorMsg.text = "Something went wrong! Try again!"
//        }


    }
}