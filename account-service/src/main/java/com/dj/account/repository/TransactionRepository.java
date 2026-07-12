package com.dj.account.repository;

import com.dj.account.entity.Account;
import com.dj.account.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByAccountOrderByTimestampDesc(Account account, Pageable pageable);
    List<Transaction> findTop10ByAccountOrderByTimestampDesc(Account account);
}
