package com.likelion.cms.domain.resource.controller;

import com.likelion.cms.domain.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;
}
