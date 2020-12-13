package com.trkapps.api.ws.services.email;

import com.trkapps.api.ws.domain.User;
import com.trkapps.api.ws.domain.VerificationToken;

import javax.mail.internet.MimeMessage;

public interface EmailService {

    void sendHtmlEmail(MimeMessage msg);

    void sendConfirmationHtmlEmail(User user, VerificationToken vToken);
}
