package com.tennantari.gallery.repository;

import com.tennantari.gallery.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
    List<Collection> findAllByOrderByCreatedAtDesc();
}
