package com.example.pipeline.domain.repository;

import com.example.pipeline.domain.entity.ExecutionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionBatchRepository extends JpaRepository<ExecutionBatch, Long> {
}