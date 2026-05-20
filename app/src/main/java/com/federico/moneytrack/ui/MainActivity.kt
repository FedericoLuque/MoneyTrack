package com.federico.moneytrack.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.federico.moneytrack.R
import com.federico.moneytrack.data.local.DataSeeder
import com.federico.moneytrack.databinding.ActivityMainBinding
import com.federico.moneytrack.ui.onboarding.OnboardingActivity
import com.federico.moneytrack.ui.onboarding.OnboardingViewModel
import com.federico.moneytrack.worker.BitcoinPriceAlertWorker
import com.federico.moneytrack.worker.BudgetAlertWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var dataSeeder: DataSeeder

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op — notifications are optional */ }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "budget_alert",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BudgetAlertWorker>(24, TimeUnit.HOURS).build()
        )

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "bitcoin_price_alert",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BitcoinPriceAlertWorker>(1, TimeUnit.HOURS).build()
        )
    }
}