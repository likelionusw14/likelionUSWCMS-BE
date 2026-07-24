package com.likelion.cms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA에서는 기본 생성자가 필수입니다
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    @Builder
    public Account(String provider, String providerId, AccountStatus accountStatus) {
        this.provider = provider;
        this.providerId = providerId;
        this.accountStatus = accountStatus;
    }

    @Column
    private String name;

    @Column
    private Integer cohortId;

    @Column
    private String part;

    public void updateAdditionalInfo(String name, Integer cohortId, String part) {
        this.name = name;
        this.cohortId = cohortId;
        this.part = part;
        this.accountStatus = AccountStatus.ACTIVE;
    }
}