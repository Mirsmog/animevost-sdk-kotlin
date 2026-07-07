package com.animevost.sdk.model

data class RegistrationRequest(
    val username: String,
    val password: String,
    val email: String,
)

data class RegistrationResult(
    val username: String,
    val session: AuthSession?,
)
