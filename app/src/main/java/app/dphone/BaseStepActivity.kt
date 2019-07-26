package app.dphone

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton


abstract class BaseStepActivity : BaseActivity() {

    abstract fun next()

    open fun previous() {
        finish()
    }

    override fun onCreateContent(content: View) {
        hideToolbar()

        val nextBtn = findViewById<FloatingActionButton>(R.id.nextBtn)
        if (nextBtn != null) {
            nextBtn.setOnClickListener {
                setNext()
            }
        }

        val skipText = findViewById<TextView>(R.id.skipText)
        if (skipText != null) {
            skipText.setOnClickListener {
                navigateToSignIn()
            }
        }

        val stepPage = findViewById<ConstraintLayout>(R.id.stepPage)
        if (stepPage != null) {
            stepPage.setOnTouchListener(object : OnSwipeTouchListener(this) {
                override fun onSwipeLeft() {
                    setNext()
                }

                override fun onSwipeRight() {
                    setPrevious()
                }
            })
        }
    }

    private fun setNext() {
        next()
        overridePendingTransition(0, 0)
    }

    private fun setPrevious() {
        previous()
        overridePendingTransition(0, 0)
    }

    override fun checkLoggedUser() {
    }

    override fun onResume() {
        super.onResume()
        if (blockstack().isUserSignedIn()) {
            finish()
            navigateToHome()
        }
    }
}