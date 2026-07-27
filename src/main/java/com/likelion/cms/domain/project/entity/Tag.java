package com.likelion.cms.domain.project.entity;

import com.likelion.cms.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tag_scope_name",
                columnNames = {"scope", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tagId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagScope scope;

    @Column(nullable = false, length = 50)
    private String name;

    @Builder
    private Tag(TagScope scope, String name) {
        this.scope = scope != null ? scope : TagScope.PROJECT;
        this.name = name;
    }
}
