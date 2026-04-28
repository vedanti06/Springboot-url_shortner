package com.example.urlshortner.repository;

import com.example.urlshortner.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

	// find by short code
	Optional<UrlMapping> findByShortCode(String shortCode);

	// check if short code exists
	boolean existsByShortCode(String shortCode);

	// find by long url
	List<UrlMapping> findByLongUrlOrderByIdDesc(String longUrl);

	// delete expired links
	@Modifying
	@Transactional
	@Query("DELETE FROM UrlMapping u WHERE u.expiresAt < :now")
	int deleteByExpiresAtBefore(LocalDateTime now);
}
