package com.trustflow.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<AccessRequest> findAllByOrderByCreatedAtDesc();
}
