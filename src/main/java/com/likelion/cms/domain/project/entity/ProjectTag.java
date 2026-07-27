package com.likelion.cms.domain.project.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ProjectTag", indexes = {
        @Index(name = "idx_project_tag_tag_id", columnList = "tagId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTag {

    @EmbeddedId
    private ProjectTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "projectId")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tagId")
    private Tag tag;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ProjectTag(Project project, Tag tag) {
        this.id = ProjectTagId.of(project.getProjectId(), tag.getTagId());
        this.project = project;
        this.tag = tag;
        this.createdAt = LocalDateTime.now();
    }
}
