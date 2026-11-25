package com.trustflow.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/admin/roles lets the admin screens show role names instead of ids (ADMIN only). */
@RestController
public class RoleController {

    public record RoleView(Long id, String name) { }

    private final RoleRepository roles;

    public RoleController(RoleRepository roles) {
        this.roles = roles;
    }

    @GetMapping("/api/admin/roles")
    public List<RoleView> list() {
        return roles.findAll().stream().map(r -> new RoleView(r.getId(), r.getName())).toList();
    }
}
