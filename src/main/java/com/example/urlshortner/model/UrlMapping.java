package com.example.urlshortner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_mappings", indexes = {
    @Index(columnList = "shortCode"),
    @Index(columnList = "longUrl")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // auto-incremented id
    private Long id;

    @Column(nullable = false, length = 2048)
    // long URL to be shortened
    private String longUrl;

    @Column(unique = true, length = 64)
    // short code for the shortened URL
    private String shortCode;

    private LocalDateTime createdAt = LocalDateTime.now();
    // expiration date of the shortened URL
    private LocalDateTime expiresAt;

    @Transient
    private String shortUrl; 
    // does not persist to database
    @Transient
    private String alias; 

    public UrlMapping() {}

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLongUrl() { return longUrl; }
    public void setLongUrl(String longUrl) { this.longUrl = longUrl; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
}