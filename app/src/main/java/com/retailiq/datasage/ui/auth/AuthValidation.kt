package com.retailiq.datasage.ui.auth

object AuthValidation {
    private val mobileRegex = Regex("^[6-9][0-9]{9}$")

    fun isValidMobile(mobile: String): Boolean = mobileRegex.matches(mobile.trim())

    fun isStrongPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isDigit() }

    fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
}
