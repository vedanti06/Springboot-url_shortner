package com.example.urlshortner.controller;

import com.example.urlshortner.model.UrlMapping;
import com.example.urlshortner.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }
	// shorten the long URL
    @PostMapping("api/v1/shorten")
    public ResponseEntity<UrlMapping> shorten(@RequestBody UrlMapping body) {
        return ResponseEntity.ok(urlService.shorten(body));
    }
	// redirect to the long URL
    @GetMapping("{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String location = urlService.getRedirectLocation(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND) // 302 Redirect
                .location(URI.create(location))
                .build();
    }
}