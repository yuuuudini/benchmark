package com.example.pipeline.domain.repository;

import com.example.pipeline.domain.entity.ExecutionRun;
import com.example.pipeline.domain.enumeration.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRunRepository extends JpaRepository<ExecutionRun, Long> {
    boolean existsByDatasetCommitHashAndScriptCommitHashAndStatus(
            String datasetCommitHash, 
            String scriptCommitHash, 
            RunStatus status
    );
}