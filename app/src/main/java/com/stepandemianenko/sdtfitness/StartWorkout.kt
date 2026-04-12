package com.stepandemianenko.sdtfitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.stepandemianenko.sdtfitness.startworkout.StartWorkoutRoute

class StartWorkout : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StartWorkoutRoute(
                    onBackClick = { finish() },
                    onStartWorkoutClick = { sessionId ->
                        startActivity(OngoingWorkout.createIntent(this, sessionId))
                    },
                    onShortenSessionClick = { },
                    onEditWorkoutClick = { },
                    onExerciseClick = { },
                    onAddExerciseClick = { },
                    onHomeClick = {
                        startActivity(Intent(this, Home::class.java))
                    },
                    onProgressClick = {
                        openProgressWithoutAnimation()
                    }
                )
            }
        }
    }

    private fun openProgressWithoutAnimation() {
        startActivity(Intent(this, Progress::class.java))
        overridePendingTransition(0, 0)
    }
}
