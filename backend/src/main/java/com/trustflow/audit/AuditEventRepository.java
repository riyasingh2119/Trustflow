package com.trustflow.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    @Query("select e.decision, count(e) from AuditEvent e group by e.decision")
    List<Object[]> countByDecision();

    @Query("select coalesce(avg(e.riskScore), 0.0) from AuditEvent e")
    Double averageScore();
}
