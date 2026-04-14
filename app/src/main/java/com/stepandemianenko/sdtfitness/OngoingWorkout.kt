package com.stepandemianenko.sdtfitness

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.stepandemianenko.sdtfitness.startworkout.OngoingWorkoutRoute

class OngoingWorkout : ComponentActivity() {
    private var activeSessionId: Long? = null

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"

        fun createIntent(context: Context, sessionId: Long? = null): Intent {
            return Intent(context, OngoingWorkout::class.java).apply {
                if (sessionId != null) {
                    putExtra(EXTRA_SESSION_ID, sessionId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeSessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L).takeIf { it > 0L }

        setContent {
            MaterialTheme {
                OngoingWorkoutRoute(
                    initialSessionId = activeSessionId,
                    onBackClick = { finish() },
                    onNavigateToStartWorkout = {
                        startActivity(StartWorkout.createIntent(this))
                        overridePendingTransition(0, 0)
                        finish()
                    },
                    onTimerClick = {},
                    onAddExerciseClick = { openStartWorkoutWithoutAnimation() },
                    onLogSetClick = { _, _, _ -> },
                    onSessionCompleted = {
                        openProgressWithoutAnimation()
                        finish()
                    },
                    onHomeClick = {
                        startActivity(Intent(this, Home::class.java))
                    },
                    onProgressClick = {
                        openProgressWithoutAnimation()
                    },
                    onProfileClick = {
                        openProfileWithoutAnimation()
                    }
                )
            }
        }
    }

    private fun openProgressWithoutAnimation() {
        startActivity(Intent(this, Progress::class.java))
        overridePendingTransition(0, 0)
    }

    private fun openProfileWithoutAnimation() {
        startActivity(Intent(this, Profile::class.java))
        overridePendingTransition(0, 0)
    }

    private fun openStartWorkoutWithoutAnimation() {
        startActivity(
            StartWorkout.createIntent(
                this,
                openAddExerciseOnStart = true,
                appendToSessionId = activeSessionId
            )
        )
        overridePendingTransition(0, 0)
    }
}
