package com.insightflow.config;

import com.insightflow.model.DataSource;
import com.insightflow.model.User;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.model.enums.Role;
import com.insightflow.repository.DataSourceRepository;
import com.insightflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DataSourceRepository dataSourceRepository;

    @Override
    public void run(String... args) {
        log.info("Checking for seed data...");

        // 1. Seed Admin User
        User admin = seedUser("admin@amalitech.com", "Admin User", "$2b$12$8.xl7j8FaEukj4Q3JU18heVk/3EpVlWRaW.LBak9sG4oCNuahovcG", Role.ADMIN);
        seedUser("user@amalitech.com", "Test User", "$2b$12$8.xl7j8FaEukj4Q3JU18heVk/3EpVlWRaW.LBak9sG4oCNuahovcG", Role.USER);

        // 2. Seed Default Data Sources
        seedDataSource("Sales CSV", "Monthly sales data from CSV upload", DataSourceType.CSV, admin);
        seedDataSource("User Analytics API", "User behavior data from analytics API", DataSourceType.API, admin);

        log.info("Seed data check complete.");
    }

    private User seedUser(String email, String name, String password, Role role) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .name(name)
                    .password(password)
                    .role(role)
                    .build();
            log.info("Seeding user: {}", email);
            return userRepository.save(user);
        }
        return existing.get();
    }

    private void seedDataSource(String name, String description, DataSourceType type, User createdBy) {
        Optional<DataSource> existing = dataSourceRepository.findFirstByName(name);
        if (existing.isEmpty()) {
            DataSource ds = DataSource.builder()
                    .name(name)
                    .description(description)
                    .type(type)
                    .createdBy(createdBy)
                    .isActive(true)
                    .build();
            log.info("Seeding data source: {}", name);
            dataSourceRepository.save(ds);
        }
    }
}
