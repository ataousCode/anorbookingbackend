package com.almousleck.repository;

import com.almousleck.model.Event;
import com.almousleck.model.EventCategory;
import com.almousleck.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByPublishedTrue(Pageable pageable);

    Page<Event> findByPublishedTrueAndCategory(EventCategory category, Pageable pageable);

    Page<Event> findByOrganizer(User organizer, Pageable pageable);

    List<Event> findByOrganizer(User organizer);

    @Query("SELECT e FROM Event e WHERE e.published = true AND e.startDate > :now")
    Page<Event> findUpcomingEvents(LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.published = true AND e.startDate > :now AND e.category = :category")
    Page<Event> findUpcomingEventsByCategory(EventCategory category, LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.published = true AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> searchEvents(String keyword, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.organizer = :organizer AND e.startDate > :now")
    List<Event> findUpcomingEventsByOrganizer(User organizer, LocalDateTime now);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.category = :category AND e.published = true")
    Long countByCategoryAndPublishedTrue(EventCategory category);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId AND e.published = true")
    List<Event> findPublishedEventsByOrganizerId(Long organizerId);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId AND e.startDate > :now")
    List<Event> findUpcomingEventsByOrganizerId(Long organizerId, LocalDateTime now);



    /// new

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId")
    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId")
    List<Event> findByOrganizerId(Long organizerId);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.organizer.id = :organizerId")
    Long countByOrganizerId(Long organizerId);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId AND e.published = :published")
    Page<Event> findByOrganizerIdAndPublished(Long organizerId, boolean published, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId AND e.published = :published")
    List<Event> findByOrganizerIdAndPublished(Long organizerId, boolean published);

    // Add this new method
    @Query("SELECT COUNT(e) FROM Event e WHERE e.category = :category")
    Long countByCategory(EventCategory category);
}