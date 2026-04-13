package com.stepandemianenko.sdtfitness

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.stepandemianenko.sdtfitness.progress.CompletedSessionsRoute
import com.stepandemianenko.sdtfitness.progress.ProgressRoutes

class CompletedSessions : ComponentActivity() {

    companion object {
        const val ROUTE = ProgressRoutes.COMPLETED_SESSIONS

        fun createIntent(context: Context): Intent {
            return Intent(context, CompletedSessions::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompletedSessionsRoute(
                    onBackClick = { finish() },
                    onOpenSessionReview = { sessionId ->
                        openSessionReview(sessionId)
                    },
                    onHomeClick = { openHomeWithoutAnimation() },
                    onWorkoutClick = { openWorkoutWithoutAnimation() },
                    onProgressClick = { closeToProgressWithoutAnimation() },
                    onProfileClick = { openProfileWithoutAnimation() }
                )
            }
        }
    }

    private fun openHomeWithoutAnimation() {
        startActivity(Intent(this, Home::class.java))
        overridePendingTransition(0, 0)
    }

    private fun openWorkoutWithoutAnimation() {
        startActivity(Intent(this, StartWorkout::class.java))
        overridePendingTransition(0, 0)
    }

    private fun openProfileWithoutAnimation() {
        startActivity(Intent(this, Profile::class.java))
        overridePendingTransition(0, 0)
    }

    private fun closeToProgressWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun openSessionReview(sessionId: Long) {
        startActivity(SessionReview.createIntent(this, sessionId))
        overridePendingTransition(0, 0)
    }
}
