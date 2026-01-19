package com.smartparentlock

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class WelcomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    
    private val pages = listOf(
        OnboardingPage(
            "Smart Parent Lock",
            "Transform screen time into learning time! \n\nThis app replaces the standard lock screen with educational challenges, turning every unlock into a brain-building moment.",
            R.drawable.ic_onboarding_fun
        ),
        OnboardingPage(
            "Learning Challenges",
            "Choose what your child learns:\n• Math (Add, Sub, Mul, Div)\n• Vocabulary & Spelling\n• Logic Patterns\n• Language Translation\n\nQuestions adapt to their age!",
            R.drawable.ic_onboarding_subjects
        ),
        OnboardingPage(
            "Session Timer Explained",
            "How it works:\n1. Screen Locks automatically.\n2. Child solves 1 question to UNLOCK.\n3. Screen stays unlocked for your set 'Re-lock Time' (e.g., 15 mins).\n4. After time is up, it locks again.",
            R.drawable.ic_onboarding_timer
        ),
        OnboardingPage(
            "Secure & Safe",
            "You are in full control.\n\n• Protected by Parent PIN\n• Anti-Uninstall Security\n• Strict Exit Validation\n\nYour settings stay safe from curious little hands.",
            R.drawable.ic_onboarding_security
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)

        val adapter = OnboardingAdapter(pages)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Custom view for dot if needed, or default
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == pages.size - 1) {
                    btnNext.text = "Get Started"
                } else {
                    btnNext.text = "Next"
                }
                updateBackgroundColor(position)
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                finishOnboarding()
            }
        }
        // Initial color
        updateBackgroundColor(0)
    }
    
    private fun updateBackgroundColor(position: Int) {
        val colors = listOf(
            "#E3F2FD", // Fun Blue
            "#F3E5F5", // Learning Purple
            "#E0F2F1", // Timer Teal
            "#E8EAF6"  // Security Indigo
        )
        
        val color = try {
            android.graphics.Color.parseColor(colors[position])
        } catch (e: Exception) {
            android.graphics.Color.WHITE
        }
        
        findViewById<View>(android.R.id.content).setBackgroundColor(color)
        
        // Update Button color to match or contrast? 
        // Let's keep button standard primary color for consistency.
    }

    private fun finishOnboarding() {
        val intent = Intent(this, PinActivity::class.java)
        intent.putExtra("FROM_WELCOME", true)
        startActivity(intent)
        finish()
    }
}

data class OnboardingPage(val title: String, val description: String, val imageRes: Int)

class OnboardingAdapter(private val pages: List<OnboardingPage>) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgOnboarding: ImageView = view.findViewById(R.id.imgOnboarding)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)

        fun bind(page: OnboardingPage) {
            tvTitle.text = page.title
            tvDescription.text = page.description
            imgOnboarding.setImageResource(page.imageRes)
            
            // Tint system icons if needed or leave as is
            // Do not tint our custom 3D illustrations
            val isCustomImage = page.imageRes == R.drawable.ic_onboarding_fun ||
                                page.imageRes == R.drawable.ic_onboarding_subjects ||
                                page.imageRes == R.drawable.ic_onboarding_timer ||
                                page.imageRes == R.drawable.ic_onboarding_security
                                
            if (!isCustomImage) {
                 imgOnboarding.setColorFilter(android.graphics.Color.parseColor("#4E8DFF"))
            } else {
                 imgOnboarding.clearColorFilter()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return PageViewHolder(view)
    }

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }
}
