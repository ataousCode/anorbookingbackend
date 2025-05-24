package com.almousleck.repository;

import com.almousleck.model.Booking;
import com.almousleck.model.Event;
import com.almousleck.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByUser(User user, Pageable pageable);

    List<Booking> findByUser(User user);

    List<Booking> findByEvent(Event event);

    @Query("SELECT b FROM Booking b WHERE b.event.organizer = :organizer")
    List<Booking> findByEventOrganizer(User organizer);

    @Query("SELECT b FROM Booking b WHERE b.event.organizer.id = :organizerId")
    Page<Booking> findByOrganizerId(Long organizerId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.user = :user AND b.event.startDate > :now AND b.status = 'CONFIRMED'")
    List<Booking> findUpcomingBookingsByUser(User user, LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.event = :event AND b.status = 'CONFIRMED'")
    Long countConfirmedBookingsByEvent(Event event);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.event.startDate BETWEEN :startTime AND :endTime")
    List<Booking> findBookingsForEventsInTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    List<Booking> findByEventAndStatus(Event event, Booking.BookingStatus status);
}

