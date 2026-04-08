package com.socialmanager.repository;

import com.socialmanager.model.PostInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PostInsightRepository extends JpaRepository<PostInsight, UUID> {

    List<PostInsight> findByDate(LocalDate date);

}