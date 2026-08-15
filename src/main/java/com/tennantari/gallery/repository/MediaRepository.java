package com.tennantari.gallery.repository;

import com.tennantari.gallery.model.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findAllByOrderByCreatedAtDesc();
    Page<Media> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Media> findByMediaTypeOrderByCreatedAtDesc(Media.MediaType mediaType);
    Page<Media> findByMediaTypeOrderByCreatedAtDesc(Media.MediaType mediaType, Pageable pageable);
    long countByMediaType(Media.MediaType mediaType);

    @Query("SELECT m FROM Media m WHERE m NOT IN (SELECT cm FROM Collection c JOIN c.mediaItems cm) ORDER BY m.createdAt DESC")
    List<Media> findUncategorizedMedia();
}
