package com.almousleck.repository;

import com.almousleck.model.Event;
import com.almousleck.model.EventCategory;
import com.almousleck.model.Role;
import com.almousleck.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    private Event testEvent;
    private User organizer;
    private EventCategory category;

    @BeforeEach
    void setUp() {
        // Create role
        Role organizerRole = Role.builder()
                .name(Role.RoleName.ROLE_ORGANIZER)
                .build();
        entityManager.persistAndFlush(organizerRole);

        // Create organizer
        organizer = User.builder()
                .name("Event Organizer")
                .username("organizer")
                .email("organizer@example.com")
                .password("password123")
                .enabled(true)
                .roles(Set.of(organizerRole))
                .build();
        entityManager.persistAndFlush(organizer);

        // Create category
        category = EventCategory.builder()
                .name("Technology")
                .description("Tech events")
                .build();
        entityManager.persistAndFlush(category);

        // Create test event
        testEvent = Event.builder()
                .title("Spring Boot Workshop")
                .description("Learn Spring Boot")
                .location("Online")
                .startDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(7).plusHours(3))
                .basePrice(new BigDecimal("99.99"))
                .published(true)
                .category(category)
                .organizer(organizer)
                .build();
    }

    @Test
    void whenFindByPublishedTrue_thenReturnPublishedEvents() {
        // Given
        entityManager.persistAndFlush(testEvent);

        Event unpublishedEvent = Event.builder()
                .title("Unpublished Event")
                .description("Not published")
                .location("Somewhere")
                .startDate(LocalDateTime.now().plusDays(10))
                .endDate(LocalDateTime.now().plusDays(10).plusHours(2))
                .basePrice(new BigDecimal("50.00"))
                .published(false)
                .category(category)
                .organizer(organizer)
                .build();
        entityManager.persistAndFlush(unpublishedEvent);

        // When
        Page<Event> publishedEvents = eventRepository.findByPublishedTrue(PageRequest.of(0, 10));

        // Then
        assertThat(publishedEvents.getContent()).hasSize(1);
        assertThat(publishedEvents.getContent().get(0).getTitle()).isEqualTo("Spring Boot Workshop");
        assertThat(publishedEvents.getContent().get(0).isPublished()).isTrue();
    }

    @Test
    void whenFindByPublishedTrueAndCategory_thenReturnEventsInCategory() {
        // Given
        entityManager.persistAndFlush(testEvent);

        EventCategory anotherCategory = EventCategory.builder()
                .name("Business")
                .description("Business events")
                .build();
        entityManager.persistAndFlush(anotherCategory);

        Event businessEvent = Event.builder()
                .title("Business Meeting")
                .description("Important meeting")
                .location("Office")
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(5).plusHours(2))
                .basePrice(new BigDecimal("0.00"))
                .published(true)
                .category(anotherCategory)
                .organizer(organizer)
                .build();
        entityManager.persistAndFlush(businessEvent);

        // When
        Page<Event> techEvents = eventRepository.findByPublishedTrueAndCategory(category, PageRequest.of(0, 10));

        // Then
        assertThat(techEvents.getContent()).hasSize(1);
        assertThat(techEvents.getContent().get(0).getTitle()).isEqualTo("Spring Boot Workshop");
        assertThat(techEvents.getContent().get(0).getCategory().getName()).isEqualTo("Technology");
    }

    @Test
    void whenFindByOrganizer_thenReturnOrganizerEvents() {
        // Given
        entityManager.persistAndFlush(testEvent);

        // When
        Page<Event> organizerEvents = eventRepository.findByOrganizer(organizer, PageRequest.of(0, 10));

        // Then
        assertThat(organizerEvents.getContent()).hasSize(1);
        assertThat(organizerEvents.getContent().get(0).getOrganizer().getUsername()).isEqualTo("organizer");
    }

    @Test
    void whenFindUpcomingEvents_thenReturnFutureEvents() {
        // Given
        entityManager.persistAndFlush(testEvent);

        Event pastEvent = Event.builder()
                .title("Past Event")
                .description("Already happened")
                .location("Somewhere")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().minusHours(1))
                .basePrice(new BigDecimal("25.00"))
                .published(true)
                .category(category)
                .organizer(organizer)
                .build();
        entityManager.persistAndFlush(pastEvent);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        List<Event> upcomingEvents = eventRepository.findUpcomingEvents(LocalDateTime.now(), pageable)
                .getContent();

        // Then
        assertThat(upcomingEvents).hasSize(1);
        assertThat(upcomingEvents.get(0).getTitle()).isEqualTo("Spring Boot Workshop");
    }
}