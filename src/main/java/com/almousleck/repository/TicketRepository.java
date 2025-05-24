package com.almousleck.repository;

import com.almousleck.model.Event;
import com.almousleck.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByEvent(Event event);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithLock(Long id);

    @Query("SELECT SUM(t.availableQuantity) FROM Ticket t WHERE t.event.id = :eventId")
    Integer countAvailableTicketsByEvent(Long eventId);
}
