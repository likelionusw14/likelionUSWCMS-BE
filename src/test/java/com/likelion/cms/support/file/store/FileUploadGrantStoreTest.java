package com.likelion.cms.support.file.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.cms.support.file.entity.FilePurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadGrantStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private FileUploadGrantStore store;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        store = new FileUploadGrantStore(redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void savesGrantWithTenMinuteTtlAndHashedObjectKey() {
        FileUploadGrant grant = grant();

        store.save(grant);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                eq(FileUploadGrantStore.TTL)
        );
        assertThat(keyCaptor.getValue())
                .startsWith("file:upload-grant:")
                .doesNotContain(grant.objectKey());
        assertThat(valueCaptor.getValue()).contains("\"actorUserId\":7");
    }

    @Test
    void readsStoredGrant() throws Exception {
        String json = objectMapper.writeValueAsString(grant());
        when(valueOperations.get(anyString())).thenReturn(json);

        Optional<FileUploadGrant> result = store.find(grant().objectKey());

        assertThat(result).contains(grant());
    }

    @Test
    void returnsEmptyForUnknownGrant() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThat(store.find(grant().objectKey())).isEmpty();
    }

    private FileUploadGrant grant() {
        return new FileUploadGrant(
                7L,
                FilePurpose.LEARNING_RESOURCE,
                "learning-resource/2026/07/25/id.pdf",
                "lecture.pdf",
                "application/pdf",
                1024L,
                null
        );
    }
}
