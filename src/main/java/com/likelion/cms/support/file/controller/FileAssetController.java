package com.likelion.cms.support.file.controller;

import com.likelion.cms.support.file.service.FileAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileAssetController {

    private final FileAssetService fileAssetService;
}
