package kz.noisemap.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String mailFrom;

    @Value("${app.mail.from-name:NoiseWatch}")
    private String mailFromName;

    @Value("${app.frontend.base-url:https://noisemap.duckdns.org}")
    private String frontendBaseUrl;


    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + resetToken;

        String subject = "Сброс пароля — NoiseWatch";
        String htmlBody = buildPasswordResetHtml(userName, resetUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(mailFrom, mailFromName, StandardCharsets.UTF_8.name()));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            // Не бросаем исключение наружу — это асинхронная операция.
            // Юзеру в API возвращаем успех (защита от email enumeration).
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * HTML-шаблон письма для сброса пароля.
     */
    private String buildPasswordResetHtml(String userName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Сброс пароля</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f4f7;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                        <td align="center" style="padding:40px 20px;">
                            <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background:#ffffff;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.05);">
                                <!-- Header -->
                                <tr>
                                    <td style="padding:32px 32px 16px 32px;text-align:center;">
                                        <h1 style="margin:0;color:#1a73e8;font-size:28px;font-weight:700;">🔊 NoiseWatch</h1>
                                        <p style="margin:8px 0 0 0;color:#5f6368;font-size:14px;">Мониторинг городского шума</p>
                                    </td>
                                </tr>

                                <!-- Body -->
                                <tr>
                                    <td style="padding:16px 32px 32px 32px;">
                                        <h2 style="margin:0 0 16px 0;color:#202124;font-size:20px;">Здравствуйте, %s!</h2>

                                        <p style="margin:0 0 16px 0;color:#3c4043;font-size:16px;line-height:1.5;">
                                            Мы получили запрос на сброс пароля для вашего аккаунта в NoiseWatch.
                                            Нажмите на кнопку ниже, чтобы установить новый пароль:
                                        </p>

                                        <!-- Button -->
                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                            <tr>
                                                <td align="center" style="padding:24px 0;">
                                                    <a href="%s"
                                                       style="display:inline-block;padding:14px 32px;background-color:#1a73e8;color:#ffffff;text-decoration:none;border-radius:6px;font-size:16px;font-weight:500;">
                                                        Сбросить пароль
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>

                                        <p style="margin:16px 0 0 0;color:#5f6368;font-size:14px;line-height:1.5;">
                                            Или скопируйте эту ссылку в браузер:<br>
                                            <a href="%s" style="color:#1a73e8;word-break:break-all;">%s</a>
                                        </p>

                                        <hr style="margin:24px 0;border:none;border-top:1px solid #e8eaed;">

                                        <p style="margin:0;color:#5f6368;font-size:13px;line-height:1.5;">
                                            ⏱ <strong>Ссылка действительна 1 час</strong>.<br>
                                            🔒 Если вы не запрашивали сброс пароля — просто проигнорируйте это письмо.
                                            Ваш пароль не будет изменён.
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="padding:24px 32px;background-color:#f8f9fa;border-radius:0 0 8px 8px;text-align:center;">
                                        <p style="margin:0;color:#80868b;font-size:12px;">
                                            © 2026 NoiseWatch. Это автоматическое письмо — не отвечайте на него.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                escapeHtml(userName),
                resetUrl,
                resetUrl,
                resetUrl
        );
    }

    /**
     * Простая защита от HTML injection в имени пользователя.
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}