package com.example.urlshortner.repository;

import com.example.urlshortner.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

	Optional<UrlMapping> findByShortCode(String shortCode);

	boolean existsByShortCode(String shortCode);

	List<UrlMapping> findByLongUrlOrderByIdDesc(String longUrl);
}
