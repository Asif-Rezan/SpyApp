package com.asifrezan.notificationreader.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.asifrezan.notificationreader.R
import com.asifrezan.notificationreader.databinding.ActivityMainBinding
import com.asifrezan.notificationreader.databinding.ActivityRegistrationBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.findNavController()

        binding.bottomNavigationBarId.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.notesFregment -> navController.navigate(R.id.notesFregment)
                R.id.addNotesFregment -> navController.navigate(R.id.addNotesFregment)
                R.id.settingsFregment -> navController.navigate(R.id.settingsFregment)

            }
            true
        }


        if (!isNotificationServiceEnabled()) {
            // Ask the user to enable the Notification Listener service
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } else {
            // Start the service
            startService(Intent(this, MainActivity::class.java))
        }
    }


    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }


}