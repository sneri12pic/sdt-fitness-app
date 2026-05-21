package com.stepandemianenko.sdtfitness.authapi.rate

import com.stepandemianenko.sdtfitness.authapi.domain.RateLimitException
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

class FixedWindowRateLimiter(
    private val maxRequests: Int,
    private val windowMillis: Long,
    private val clock: Clock = Clock.systemUTC()
) {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun requireAllowed(key: String) {
        val now = clock.millis()
        val allowed = buckets.compute(key) { _, current ->
            if (current == null || now >= current.resetAtMillis) {
                Bucket(count = 1, resetAtMillis = now + windowMillis)
            } else {
                current.copy(count = current.count + 1)
            }
        }?.count?.let { it <= maxRequests } ?: false

        if (!allowed) {
            throw RateLimitException()
        }
    }

    private data class Bucket(
        val count: Int,
        val resetAtMillis: Long
    )
}
