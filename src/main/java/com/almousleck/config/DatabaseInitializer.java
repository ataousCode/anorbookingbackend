package com.almousleck.config;

import com.almousleck.model.Role;
import com.almousleck.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
    }

    private void initRoles() {
        List<Role.RoleName> roleNames = Arrays.asList(
                Role.RoleName.ROLE_USER,
                Role.RoleName.ROLE_ORGANIZER,
                Role.RoleName.ROLE_ADMIN
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
}
