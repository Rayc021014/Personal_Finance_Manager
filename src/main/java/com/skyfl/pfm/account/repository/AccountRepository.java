package com.skyfl.pfm.account.repository;

import com.skyfl.pfm.account.entity.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdAndArchivedFalseOrderByCreatedAtDesc(UUID userId);

    Optional<Account> findByIdAndUserIdAndArchivedFalse(UUID id, UUID userId);
}
