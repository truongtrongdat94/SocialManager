package com.socialmanager.repository;

import com.socialmanager.model.AccountDailyInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountDailyInsightRepository extends JpaRepository<AccountDailyInsight, UUID> {
    void deleteBySocialAccount_Id(UUID socialAccountId);
}
