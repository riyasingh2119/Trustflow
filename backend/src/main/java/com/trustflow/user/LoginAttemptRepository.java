package com.trustflow.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    long countByUsernameAndSuccessFalseAndAttemptedAtAfter(String username, Instant since);
}
