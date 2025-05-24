package com.almousleck.service;

import com.almousleck.dto.admin.AdminDashboardResponse;
import com.almousleck.dto.admin.UserResponse;
import com.almousleck.dto.admin.UserStatusRequest;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.Event;
import com.almousleck.model.User;
import com.almousleck.repository.BookingRepository;
import com.almousleck.repository.EventRepository;
import com.almousleck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;

    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();
        long totalBookings = bookingRepository.count();

        // Get upcoming events
        List<Event> upcomingEvents = eventRepository.findUpcomingEvents(LocalDateTime.now(), Pageable.ofSize(5)).getContent();

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalEvents(totalEvents)
                .totalBookings(totalBookings)
                .upcomingEvents(upcomingEvents.stream()
                        .map(event -> AdminDashboardResponse.EventSummary.builder()
                                .id(event.getId())
                                .title(event.getTitle())
                                .startDate(event.getStartDate())
                                .organizerName(event.getOrganizer().getName())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToUserResponse);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return convertToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setEnabled(request.isEnabled());
        User updatedUser = userRepository.save(user);

        return convertToUserResponse(updatedUser);
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
