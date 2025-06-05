package com.almousleck.config;

import com.almousleck.model.Role;
import com.almousleck.model.EventCategory;
import com.almousleck.model.Event;
import com.almousleck.model.User;
import com.almousleck.model.Ticket;
import com.almousleck.repository.RoleRepository;
import com.almousleck.repository.EventCategoryRepository;
import com.almousleck.repository.EventRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
    }

    private void initRoles() {
        List<Role.RoleName> roleNames = Arrays.asList(
                Role.RoleName.ROLE_USER,
                Role.RoleName.ROLE_ADMIN,
                Role.RoleName.ROLE_ORGANIZER
        );

        for (Role.RoleName roleName : roleNames) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder()
                        .name(roleName)
                        .build();
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }

        log.info("Roles initialization completed");
    }

    private void initCategories() {
        if (categoryRepository.count() == 0) {
            List<String> categoryNames = Arrays.asList(
                    "Music", "Technology", "Business", "Sports", "Arts", "Food", "Education"
            );

            for (String name : categoryNames) {
                EventCategory category = EventCategory.builder()
                        .name(name)
                        .description("Events related to " + name.toLowerCase())
                        .active(true)
                        .build();
                categoryRepository.save(category);
                log.info("Created category: {}", name);
            }
        }
    }

    private void initSampleData() {
        if (eventRepository.count() == 0) {
            // Create sample organizer user
            User organizer = createSampleOrganizer();

            // Create sample events
            createSampleEvents(organizer);
        }
    }

    private User createSampleOrganizer() {
        if (userRepository.findByEmail("organizer@example.com").isEmpty()) {
            Role organizerRole = roleRepository.findByName(Role.RoleName.ROLE_ORGANIZER)
                    .orElseThrow(() -> new RuntimeException("Organizer role not found"));

            User organizer = User.builder()
                    .name("Sample Organizer")
                    .email("organizer@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .enabled(true)
                    .build();
            organizer.getRoles().add(organizerRole);

            return userRepository.save(organizer);
        }
        return userRepository.findByEmail("organizer@example.com").get();
    }

    private void createSampleEvents(User organizer) {
        List<EventCategory> categories = categoryRepository.findAll();

        if (!categories.isEmpty()) {
            EventCategory musicCategory = categories.get(0);
            EventCategory techCategory = categories.size() > 1 ? categories.get(1) : musicCategory;

            // Create upcoming events
            createEvent("Summer Music Festival", "Join us for an amazing summer music festival",
                    "Central Park, New York", LocalDateTime.now().plusDays(30),
                    LocalDateTime.now().plusDays(30).plusHours(8),
                    new BigDecimal("50.00"), musicCategory, organizer, true);

            createEvent("Tech Conference 2024", "Latest trends in technology",
                    "Convention Center, San Francisco", LocalDateTime.now().plusDays(45),
                    LocalDateTime.now().plusDays(45).plusHours(6),
                    new BigDecimal("100.00"), techCategory, organizer, true);

            createEvent("Jazz Night", "Smooth jazz evening with local artists",
                    "Blue Note Club, Chicago", LocalDateTime.now().plusDays(15),
                    LocalDateTime.now().plusDays(15).plusHours(4),
                    new BigDecimal("25.00"), musicCategory, organizer, true);

            createEvent("Food Festival", "Taste the best local cuisine",
                    "Downtown Square, Austin", LocalDateTime.now().plusDays(60),
                    LocalDateTime.now().plusDays(60).plusHours(10),
                    new BigDecimal("15.00"), musicCategory, organizer, true);

            log.info("Sample events created successfully");
        }
    }

    private void createEvent(String title, String description, String location,
                             LocalDateTime startDate, LocalDateTime endDate,
                             BigDecimal basePrice, EventCategory category,
                             User organizer, boolean published) {
        Event event = Event.builder()
                .title(title)
                .description(description)
                .location(location)
                .startDate(startDate)
                .endDate(endDate)
                .basePrice(basePrice)
                .imageUrl("https://via.placeholder.com/400x300")
                .published(published)
                .category(category)
                .organizer(organizer)
                .build();

        Event savedEvent = eventRepository.save(event);

        // Create default ticket
        Ticket ticket = Ticket.builder()
                .type("General Admission")
                .price(basePrice)
                .totalQuantity(100)
                .availableQuantity(100)
                .event(savedEvent)
                .build();

        ticketRepository.save(ticket);
    }
}