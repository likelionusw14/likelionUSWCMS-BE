package com.likelion.cms.domain.project.entity;

import com.likelion.cms.domain.user.entity.AppUser;
import com.likelion.cms.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ProjectParticipation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participation_user_project",
                columnNames = {"userId", "projectId"}),
        indexes = {
                @Index(name = "idx_participation_project_id", columnList = "projectId")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectParticipation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projectId", nullable = false)
    private Project project;

    @Column(nullable = false, length = 100)
    private String role;

    @Builder
    private ProjectParticipation(AppUser user, Project project, String role) {
        this.user = user;
        this.project = project;
        this.role = role;
    }
}
