package org.example.hacken.repository;

import org.example.hacken.model.ProcessedBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedBlockRepository extends JpaRepository<ProcessedBlock, Integer> {
}
