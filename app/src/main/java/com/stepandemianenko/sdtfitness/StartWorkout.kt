package com.stepandemianenko.sdtfitness

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.stepandemianenko.sdtfitness.startworkout.StartWorkoutRoute

class StartWorkout : ComponentActivity() {
    companion object {
        private const val EXTRA_OPEN_ADD_EXERCISE = "extra_open_add_exercise"
        private const val EXTRA_APPEND_TO_SESSION_ID = "extra_append_to_session_id"

        fun createIntent(
            context: Context,
            openAddExerciseOnStart: Boolean = false,
            appendToSessionId: Long? = null
        ): Intent {
            return Intent(context, StartWorkout::class.java).apply {
                putExtra(EXTRA_OPEN_ADD_EXERCISE, openAddExerciseOnStart)
                if (appendToSessionId != null) {
                    putExtra(EXTRA_APPEND_TO_SESSION_ID, appendToSessionId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openAddExerciseOnStart = intent.getBooleanExtra(EXTRA_OPEN_ADD_EXERCISE, false)
        val appendToSessionId = intent.getLongExtra(EXTRA_APPEND_TO_SESSION_ID, -1L).takeIf { it > 0L }

        setContent {
            MaterialTheme {
                StartWorkoutRoute(
                    onBackClick = { finish() },
                    onStartWorkoutClick = { sessionId ->
                        val destinationIntent = OngoingWorkout.createIntent(this, sessionId).apply {
                            if (openAddExerciseOnStart) {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        }
                        startActivity(destinationIntent)
                        if (openAddExerciseOnStart) {
                            finish()
                        }
                    },
                    openAddExerciseOnStart = openAddExerciseOnStart,
                    appendToSessionId = appendToSessionId,
                    onShortenSessionClick = { },
                    onEditWorkoutClick = { },
                    onExerciseClick = { },
                    onAddExerciseClick = { },
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
}
