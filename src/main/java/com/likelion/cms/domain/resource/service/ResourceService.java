package com.likelion.cms.domain.resource.service;

import com.likelion.cms.domain.resource.repository.LearningResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final LearningResourceRepository learningResourceRepository;
}
