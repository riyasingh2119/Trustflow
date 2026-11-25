package com.trustflow.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByUserIdAndFingerprint(Long userId, String fingerprint);
    List<Device> findByUserId(Long userId);
}
