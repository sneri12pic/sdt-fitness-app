package com.stepandemianenko.sdtfitness

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.progress.CompletedSessionsRoute
import com.stepandemianenko.sdtfitness.progress.ProgressRoutes
import kotlinx.coroutines.launch

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
        lifecycleScope.launch {
            val activeSessionId = AppGraph.workoutSessionRepository(this@CompletedSessions).getActiveSessionId()
            val intent = if (activeSessionId != null) {
                OngoingWorkout.createIntent(this@CompletedSessions, activeSessionId)
            } else {
                StartWorkout.createIntent(this@CompletedSessions)
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
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
