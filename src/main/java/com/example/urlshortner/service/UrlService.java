package com.example.urlshortner.service;

import com.example.urlshortner.model.UrlMapping;
import com.example.urlshortner.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class UrlService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final UrlRepository repository;

    @Value("${app.base.url}")
    private String baseUrl;

    public UrlService(UrlRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UrlMapping shorten(UrlMapping body) {
        if (body.getLongUrl() == null || body.getLongUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "longUrl is required");
        }

        // 1. Handle Custom Alias
        if (body.getAlias() != null && !body.getAlias().isBlank()) {
            return shortenWithAlias(body.getLongUrl(), body.getAlias().strip(), body.getExpiresAt());
        }

        // 2. Reuse existing non-expired mapping if available
        return repository.findByLongUrlOrderByIdDesc(body.getLongUrl()).stream()
                .filter(m -> !m.isExpired())
                .findFirst()
                .map(this::enrich)
                .orElseGet(() -> createNewShortUrl(body.getLongUrl(), body.getExpiresAt()));
    }
// Create a new short URL
    private UrlMapping createNewShortUrl(String longUrl, LocalDateTime expires) {
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl(longUrl);
        mapping.setExpiresAt(expires);

        // Save first to get the auto-increment ID
        mapping = repository.save(mapping);

        // Shuffle the ID to make the URL look random (Security)
        long shuffledId = shuffle(mapping.getId());
        mapping.setShortCode(encodeBase62(shuffledId));

        return enrich(repository.save(mapping));
    }
// Shorten the long URL with a custom alias
    private UrlMapping shortenWithAlias(String longUrl, String alias, LocalDateTime expires) {
        if (repository.existsByShortCode(alias)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alias already in use");
        }
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl(longUrl);
        mapping.setShortCode(alias);
        mapping.setExpiresAt(expires);
        return enrich(repository.save(mapping));
    }
// Get the redirect location
    @Transactional(readOnly = true)
    public String getRedirectLocation(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        if (mapping.isExpired()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Link expired");
        }
        return mapping.getLongUrl();
    }

    // Bit-shuffle to prevent predictable sequential URLs
    private long shuffle(long id) {
        long mask = 0xFFFFFFFFFFL; // 40-bit limit for ~7 char codes
        id = (id ^ 0x5DEECE66DL) & mask;
        id = (id * 2862933555777941757L) & mask;
        return (id ^ 0xBEA00DEADL) & mask;
    }
// Encode the id to a base62 string
    private String encodeBase62(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(ALPHABET.charAt((int) (num % 62)));
            num /= 62;
        }
        return sb.reverse().toString();
    }
// Enrich the URL mapping with the short URL
    private UrlMapping enrich(UrlMapping m) {
        m.setShortUrl(baseUrl.replaceAll("/$", "") + "/" + m.getShortCode());
        return m;
    }
}