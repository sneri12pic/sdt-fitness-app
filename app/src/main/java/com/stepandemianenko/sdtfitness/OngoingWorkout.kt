package com.stepandemianenko.sdtfitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.stepandemianenko.sdtfitness.startworkout.OngoingWorkoutRoute

class OngoingWorkout : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OngoingWorkoutRoute(
                    onBackClick = { finish() },
                    onEditClick = { },
                    onLogSetClick = { _, _, _ -> },
                    onHomeClick = {
                        startActivity(Intent(this, Home::class.java))
                    },
                    onCommunityClick = {
                    }
                )
            }
        }
    }
}
