package com.federico.moneytrack.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.federico.moneytrack.R
import com.federico.moneytrack.databinding.ActivityOnboardingBinding
import com.federico.moneytrack.ui.MainActivity
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()

    private val slides = listOf(
        OnboardingSlide(R.string.onboarding_title_1, R.string.onboarding_desc_1, R.drawable.ic_launcher_foreground),
        OnboardingSlide(R.string.onboarding_title_2, R.string.onboarding_desc_2, R.drawable.ic_launcher_foreground),
        OnboardingSlide(R.string.onboarding_title_3, R.string.onboarding_desc_3, R.drawable.ic_launcher_foreground)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingPagerAdapter(slides)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.btnNext.setText(
                    if (position == slides.lastIndex) R.string.onboarding_start
                    else R.string.onboarding_next
                )
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < slides.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun finishOnboarding() {
        viewModel.markCompleted()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
