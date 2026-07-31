package com.likelion.cms.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    /**
     * 활동증명서 발급(CertificateService.issueCertificate)의 DB 쓰기 전용 템플릿.
     * issueCertificate는 클래스 레벨 @Transactional(readOnly = true) 아래에서 실행되므로,
     * 쓰기를 기본 전파(REQUIRED)로 감싸면 바깥 readOnly 트랜잭션에 참여해 버려서
     * (1) INSERT가 readOnly 트랜잭션에서 실행되고 (2) 실제 커밋이 메서드 종료 시점으로 밀려
     * Redis complete() 호출보다 뒤로 가는 문제가 있다.
     * REQUIRES_NEW로 쓰기를 독립 트랜잭션에서 즉시 커밋시켜 두 문제를 모두 방지한다.
     * (현재 이 빈은 위 용도로만 쓰인다. REQUIRED 동작이 필요하면 별도 빈을 추가할 것.)
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }
}
