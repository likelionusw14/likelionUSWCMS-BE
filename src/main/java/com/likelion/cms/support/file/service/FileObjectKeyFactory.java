package com.likelion.cms.support.file.service;

import com.likelion.cms.support.file.entity.FilePurpose;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class FileObjectKeyFactory {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public String create(FilePurpose purpose, String extension) {
        String purposePath = purpose.name()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
        return "%s/%s/%s.%s".formatted(
                purposePath,
                LocalDate.now(KOREA_ZONE).format(DATE_PATH),
                UUID.randomUUID(),
                extension
        );
    }
}
