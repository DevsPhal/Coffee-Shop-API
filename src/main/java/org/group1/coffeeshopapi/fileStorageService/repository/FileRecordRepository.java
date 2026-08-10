package org.group1.coffeeshopapi.fileStorageService.repository;

import org.group1.coffeeshopapi.fileStorageService.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, UUID> {

    Optional<FileRecord> findByObjectName(String objectName);

    void deleteByObjectName(String objectName);

    List<FileRecord> findAllByOrderByCreatedAtDesc();
}
