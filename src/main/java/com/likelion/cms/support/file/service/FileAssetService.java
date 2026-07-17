package com.likelion.cms.support.file.service;

import com.likelion.cms.support.file.repository.FileAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileAssetService {

    private final FileAssetRepository fileAssetRepository;
}
