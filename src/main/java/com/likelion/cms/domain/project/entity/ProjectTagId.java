package com.likelion.cms.domain.project.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class ProjectTagId implements Serializable {

    private Long projectId;
    private Long tagId;

    public static ProjectTagId of(Long projectId, Long tagId) {
        return new ProjectTagId(projectId, tagId);
    }
}
