package com.stepandemianenko.sdtfitness

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.stepandemianenko.sdtfitness.progress.ProgressRoutes
import com.stepandemianenko.sdtfitness.progress.SessionReviewRoute

class SessionReview : ComponentActivity() {

    companion object {
        private const val EXTRA_SESSION_ID = "extra_session_id"
        const val ROUTE = ProgressRoutes.SESSION_REVIEW

        fun createIntent(context: Context, sessionId: Long): Intent {
            return Intent(context, SessionReview::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (sessionId <= 0L) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                SessionReviewRoute(
                    sessionId = sessionId,
                    onBackClick = { finish() }
                )
            }
        }
    }
}
