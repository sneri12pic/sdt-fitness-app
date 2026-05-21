package com.stepandemianenko.sdtfitness.authapi.security

import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordHasher(
    private val cost: Int
) {
    fun hash(password: String): String {
        return BCrypt.withDefaults().hashToString(cost, password.toCharArray())
    }

    fun verify(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}
