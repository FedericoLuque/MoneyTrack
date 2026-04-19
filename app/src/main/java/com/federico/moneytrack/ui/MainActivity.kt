package com.federico.moneytrack.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.federico.moneytrack.R
import com.federico.moneytrack.data.local.DataSeeder
import com.federico.moneytrack.databinding.ActivityMainBinding
import com.federico.moneytrack.ui.onboarding.OnboardingActivity
import com.federico.moneytrack.ui.onboarding.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var dataSeeder: DataSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("moneytrack_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Seed data if database is empty
        lifecycleScope.launch {
            dataSeeder.seedIfEmpty()
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup Bottom Navigation
        binding.bottomNavView.setupWithNavController(navController)

        // Hide Bottom Navigation on destination change for specific fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboardFragment, R.id.accountsFragment, R.id.bitcoinFragment, R.id.chartsFragment -> {
                    binding.bottomNavView.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavView.visibility = View.GONE
                }
            }
        }
    }
}