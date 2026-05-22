package com.nabra.backend.security.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendResetCode(String toEmail, String code) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Code");
        message.setText(
            "Your password reset code is:\n\n" + code +
            "\n\nThis code expires in 10 minutes."
        );

        mailSender.send(message);
    }
}
