package com.messenger.service

import com.messenger.config.AppConfig
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

class EmailService(
    private val config: AppConfig,
) {
    fun sendVerificationEmail(email: String, code: String) {
        send(email, "Verify your Messenger account", "Your verification code is: $code")
    }

    fun sendPasswordResetEmail(email: String, code: String) {
        send(email, "Reset your Messenger password", "Your password reset code is: $code")
    }

    private fun send(email: String, subject: String, body: String) {
        val host = config.smtpHost ?: return
        val from = config.smtpFrom ?: return
        val properties = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", host)
            put("mail.smtp.port", config.smtpPort.toString())
        }
        val session = Session.getInstance(
            properties,
            object : jakarta.mail.Authenticator() {
                override fun getPasswordAuthentication(): jakarta.mail.PasswordAuthentication {
                    return jakarta.mail.PasswordAuthentication(config.smtpUser, config.smtpPassword)
                }
            },
        )
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(from))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
            setSubject(subject)
            setText(body)
        }
        Transport.send(message)
    }
}
