package com.loyalstring.rfid.worker

import java.io.File
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.activation.DataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import android.util.Log

object EmailSender {

    /**
     * Sends an email with optional file attachments using Hostinger SMTP.
     *
     * @param fromEmail Sender email (Hostinger mailbox)
     * @param fromPassword Sender email password
     * @param toEmails List of recipient email addresses
     * @param subject Email subject
     * @param body Email body text or HTML
     * @param isHtml Whether body is HTML (default = false)
     * @param attachments Optional map of attachments (filename → file)
     */
    fun sendEmailWithAttachment(
        fromEmail: String = "android@loyalstring.com",
        fromPassword: String = "Android@456#",
        toEmails: List<String>,
        subject: String,
        body: String,
        isHtml: Boolean = false,
        attachments: Map<String, File> = emptyMap()
    ) {
        Thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.hostinger.com")
                    put("mail.smtp.port", "465")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.ssl.enable", "true")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(fromEmail, fromPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(fromEmail))
                    setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(toEmails.joinToString(","))
                    )
                    setSubject(subject)
                }

                val multipart = MimeMultipart()

                // Body part
                val bodyPart = MimeBodyPart().apply {
                    if (isHtml)
                        setContent(body, "text/html; charset=utf-8")
                    else
                        setText(body)
                }
                multipart.addBodyPart(bodyPart)

                // Attachments
                attachments.forEach { (filename, file) ->
                    if (file.exists()) {
                        val attachPart = MimeBodyPart()
                        val source: DataSource = FileDataSource(file)
                        attachPart.dataHandler = DataHandler(source)
                        attachPart.fileName = filename
                        multipart.addBodyPart(attachPart)
                    } else {
                        Log.w("EmailSender", "⚠️ Attachment not found: ${file.absolutePath}")
                    }
                }

                message.setContent(multipart)

                // Send email
                Transport.send(message)

                Log.d("EmailSender", "✅ Email sent successfully to ${toEmails.joinToString()}")

            } catch (e: Exception) {
                Log.e("EmailSender", "❌ Failed to send email: ${e.message}", e)
            }
        }.start()
    }
}
