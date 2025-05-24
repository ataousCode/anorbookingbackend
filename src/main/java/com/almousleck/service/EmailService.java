package com.almousleck.service;

import com.almousleck.config.AppProperties;
import com.almousleck.model.Booking;
import com.almousleck.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Async
    public void sendRegistrationOtp(String to, String name, String otp) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("otp", otp);
        variables.put("expiryMinutes", appProperties.getOtp().getExpiration() / 60);

        sendEmail(to, "Verify Your Registration", "registration-otp", variables);
    }

    @Async
    public void sendPasswordResetOtp(String to, String name, String otp) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("otp", otp);
        variables.put("expiryMinutes", appProperties.getOtp().getExpiration() / 60);

        sendEmail(to, "Reset Your Password", "reset-password-otp", variables);
    }

    @Async
    public void sendEmailChangeOtp(String to, String name, String otp) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("otp", otp);
        variables.put("expiryMinutes", appProperties.getOtp().getExpiration() / 60);

        sendEmail(to, "Verify Email Change", "change-email-otp", variables);
    }

    @Async
    public void sendBookingConfirmation(String to, String name, Booking booking) {
        Event event = booking.getEvent();

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("bookingReference", booking.getBookingReference());
        variables.put("eventTitle", event.getTitle());
        variables.put("eventDate", event.getStartDate().format(DATE_FORMATTER));
        variables.put("eventLocation", event.getLocation());
        variables.put("ticketType", booking.getTicket().getType());
        variables.put("quantity", booking.getQuantity());
        variables.put("totalAmount", booking.getTotalAmount());

        sendEmail(to, "Booking Confirmation", "booking-confirmation", variables);
    }

    @Async
    public void sendBookingCancellation(String to, String name, Booking booking) {
        Event event = booking.getEvent();

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("bookingReference", booking.getBookingReference());
        variables.put("eventTitle", event.getTitle());
        variables.put("eventDate", event.getStartDate().format(DATE_FORMATTER));

        sendEmail(to, "Booking Cancellation", "booking-cancellation", variables);
    }

    @Async
    public void sendEventReminder(String to, String name, Booking booking) {
        Event event = booking.getEvent();

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("eventTitle", event.getTitle());
        variables.put("eventDate", event.getStartDate().format(DATE_FORMATTER));
        variables.put("eventLocation", event.getLocation());
        variables.put("bookingReference", booking.getBookingReference());

        sendEmail(to, "Event Reminder", "event-reminder", variables);
    }

    @Async
    public void sendOrganizerApprovalNotification(String to, String name) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);

        sendEmail(to, "Organizer Application Approved", "organizer-approval", variables);
    }

    @Async
    public void sendOrganizerRejectionNotification(String to, String name, String feedback) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("feedback", feedback);

        sendEmail(to, "Organizer Application Rejected", "organizer-rejection", variables);
    }

    private void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.getEmail().getFrom());
            helper.setTo(to);
            helper.setSubject(subject);

            Context context = new Context();
            variables.put("baseUrl", appProperties.getEmail().getBaseUrl());
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

