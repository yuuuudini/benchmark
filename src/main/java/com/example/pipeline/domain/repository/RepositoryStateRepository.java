package com.example.pipeline.domain.repository;

import com.example.pipeline.domain.entity.RepositoryState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryStateRepository extends JpaRepository<RepositoryState, String> {
}