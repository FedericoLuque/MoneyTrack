package com.federico.moneytrack.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.federico.moneytrack.databinding.ItemOnboardingSlideBinding

class OnboardingPagerAdapter(
    private val slides: List<OnboardingSlide>
) : RecyclerView.Adapter<OnboardingPagerAdapter.SlideViewHolder>() {

    inner class SlideViewHolder(private val binding: ItemOnboardingSlideBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(slide: OnboardingSlide) {
            binding.tvSlideTitle.setText(slide.titleRes)
            binding.tvSlideDescription.setText(slide.descriptionRes)
            binding.ivSlideIcon.setImageResource(slide.iconRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val binding = ItemOnboardingSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SlideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size
}
