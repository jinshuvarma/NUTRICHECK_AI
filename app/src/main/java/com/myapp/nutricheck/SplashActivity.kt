package com.myapp.nutricheck

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<View>(R.id.ivSplashLogo)
        val beam = findViewById<View>(R.id.scannerBeam)

        // Logo fade-in and zoom
        logo.scaleX = 0.88f
        logo.scaleY = 0.88f

        val logoFade = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f)
        val zoomX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.88f, 1f)
        val zoomY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.88f, 1f)

        val logoAnimation = AnimatorSet().apply {
            playTogether(logoFade, zoomX, zoomY)
            duration = 650
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Scanner beam moves across the logo

        val density = resources.displayMetrics.density

        beam.alpha = 0f
        beam.translationY = 55f * density

        val scanAnimation = ObjectAnimator.ofFloat(
            beam,
            View.TRANSLATION_Y,
            55f * density,
            -35f * density
        ).apply {
            duration = 1200
            interpolator = android.view.animation.LinearInterpolator()
        }


        logoAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                beam.alpha = 1f

                scanAnimation.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        beam.alpha = 0f
                        beam.translationY = 70f

                        logo.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .withEndAction {
                                startActivity(
                                    Intent(
                                        this@SplashActivity,
                                        MainActivity::class.java
                                    )
                                )
                                finish()
                                overridePendingTransition(
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out
                                )
                            }
                            .start()
                    }
                })

                scanAnimation.start()
            }
        })

        logoAnimation.start()
    }
}
