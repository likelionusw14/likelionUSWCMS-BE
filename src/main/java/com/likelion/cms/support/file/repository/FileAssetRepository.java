package com.likelion.cms.support.file.repository;

import com.likelion.cms.support.file.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {
    Optional<FileAsset> findByObjectKey(String objectKey);
}
