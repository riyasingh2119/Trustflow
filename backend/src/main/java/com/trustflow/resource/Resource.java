package com.trustflow.resource;

import jakarta.persistence.*;

@Entity
@Table(name = "resources")
public class Resource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ResourceType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Environment environment;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Sensitivity sensitivity;
    @Column(nullable = false)
    private String path;

    protected Resource() { }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public Sensitivity getSensitivity() { return sensitivity; }
    public void setSensitivity(Sensitivity sensitivity) { this.sensitivity = sensitivity; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public static Resource of(String name, ResourceType t, Environment e, Sensitivity s, String path) {
        Resource r = new Resource();
        r.name = name; r.type = t; r.environment = e; r.sensitivity = s; r.path = path;
        return r;
    }
}
