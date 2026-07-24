package com.likelion.cms.repository;

import com.likelion.cms.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByProviderAndProviderId(String provider, String providerId);
}