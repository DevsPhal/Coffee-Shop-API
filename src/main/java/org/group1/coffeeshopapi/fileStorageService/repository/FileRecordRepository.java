package org.group1.coffeeshopapi.fileStorageService.repository;

import org.group1.coffeeshopapi.fileStorageService.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    Optional<FileRecord> findByObjectName(String objectName);

    void deleteByObjectName(String objectName);

    List<FileRecord> findAllByOrderByCreatedAtDesc();
}
