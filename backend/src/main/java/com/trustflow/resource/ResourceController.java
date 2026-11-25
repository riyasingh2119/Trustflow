package com.trustflow.resource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class ResourceController {

    public record ResourceView(Long id, String name, ResourceType type, Environment environment,
                               Sensitivity sensitivity, String path) {
        static ResourceView of(Resource r) {
            return new ResourceView(r.getId(), r.getName(), r.getType(), r.getEnvironment(), r.getSensitivity(), r.getPath());
        }
    }

    public record ResourceRequest(@NotBlank String name, @NotNull ResourceType type,
                                  @NotNull Environment environment, @NotNull Sensitivity sensitivity,
                                  @NotBlank String path) { }

    private final ResourceRepository repo;

    public ResourceController(ResourceRepository repo) {
        this.repo = repo;
    }

    /** GET /api/resources: any authenticated user. */
    @GetMapping("/api/resources")
    public List<ResourceView> list() {
        return repo.findAll().stream().map(ResourceView::of).toList();
    }

    /** CRUD /api/admin/resources: ADMIN only. */
    @GetMapping("/api/admin/resources")
    public List<ResourceView> adminList() {
        return list();
    }

    @PostMapping("/api/admin/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceView create(@Valid @RequestBody ResourceRequest q) {
        return ResourceView.of(repo.save(Resource.of(q.name(), q.type(), q.environment(), q.sensitivity(), q.path())));
    }

    @PutMapping("/api/admin/resources/{id}")
    public ResourceView update(@PathVariable Long id, @Valid @RequestBody ResourceRequest q) {
        Resource r = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        r.setName(q.name()); r.setType(q.type()); r.setEnvironment(q.environment());
        r.setSensitivity(q.sensitivity()); r.setPath(q.path());
        return ResourceView.of(repo.save(r));
    }

    @DeleteMapping("/api/admin/resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        repo.deleteById(id);
    }
}
