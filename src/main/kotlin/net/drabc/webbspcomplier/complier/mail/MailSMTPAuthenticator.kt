package net.drabc.webbspcomplier.complier.mail

import javax.mail.Authenticator
import javax.mail.PasswordAuthentication

class MailSMTPAuthenticator : Authenticator() {
    var Name: String? = null
    var Password: String? = null
    public override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(Name, Password)
    }
}