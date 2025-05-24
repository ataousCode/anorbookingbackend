package com.almousleck.scheduler;

import com.almousleck.model.Booking;
import com.almousleck.model.User;
import com.almousleck.repository.BookingRepository;
import com.almousleck.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReminderScheduler {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    // Run every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendEventReminders() {
        log.info("Starting event reminder job");

        // Get events happening in the next 24 hours
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        List<Booking> upcomingBookings = bookingRepository
                .findBookingsForEventsInTimeRange(now, tomorrow);

        log.info("Found {} bookings for events in the next 24 hours", upcomingBookings.size());

        for (Booking booking : upcomingBookings) {
            try {
                User user = booking.getUser();
                emailService.sendEventReminder(user.getEmail(), user.getName(), booking);
                log.info("Sent reminder for booking {} to user {}", booking.getBookingReference(), user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send reminder for booking {}", booking.getBookingReference(), e);
            }
        }

        log.info("Completed event reminder job");
    }
}

