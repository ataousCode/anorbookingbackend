package com.almousleck.repository;

import com.almousleck.model.OrganizerApplication;
import com.almousleck.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizerApplicationRepository extends JpaRepository<OrganizerApplication, Long> {

    Optional<OrganizerApplication> findByUser(User user);

    Page<OrganizerApplication> findByStatus(OrganizerApplication.ApplicationStatus status, Pageable pageable);

    boolean existsByUserAndStatus(User user, OrganizerApplication.ApplicationStatus status);
}
