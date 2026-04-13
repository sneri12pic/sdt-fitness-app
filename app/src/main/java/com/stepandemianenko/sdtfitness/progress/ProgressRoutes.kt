package com.stepandemianenko.sdtfitness.progress

object ProgressRoutes {
    const val COMPLETED_SESSIONS = "progress/completedSessions"
    const val SESSION_REVIEW = "progress/sessionReview"

    fun sessionReview(sessionId: Long): String {
        return "$SESSION_REVIEW/$sessionId"
    }
}
