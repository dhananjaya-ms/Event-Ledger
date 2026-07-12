package com.dj.account.service;

import com.dj.account.dto.AccountDetailsResponse;
import com.dj.account.dto.TransactionRequest;
import com.dj.account.dto.TransactionResponse;
import com.dj.account.entity.Account;
import com.dj.account.entity.Transaction;
import com.dj.account.repository.AccountRepository;
import com.dj.account.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TransactionResponse applyTransaction(String accountId, TransactionRequest req) {
        Account account = accountRepository.findById(accountId).orElseGet(() -> {
            Account a = new Account();
            a.setId(accountId);
            a.setBalance(BigDecimal.ZERO);
            return accountRepository.save(a);
        });

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setAmount(req.getAmount());
        tx.setType(req.getType());
        tx.setCurrency(req.getCurrency());
        tx.setTimestamp(LocalDateTime.now());

        Transaction saved = transactionRepository.save(tx);

        BigDecimal newBalance = account.getBalance().add(saved.getAmount());
        account.setBalance(newBalance == null ? BigDecimal.ZERO : newBalance);
        accountRepository.save(account);

        return new TransactionResponse(saved.getId(), saved.getAmount(), saved.getTimestamp(), saved.getType(), saved.getCurrency());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountId) {
        return accountRepository.findById(accountId).map(Account::getBalance).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDetailsResponse getAccountDetails(String accountId, int recentTxCount) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return null;
        }

        List<Transaction> txs = transactionRepository.findByAccountOrderByTimestampDesc(account, PageRequest.of(0, Math.max(1, recentTxCount))).getContent();

        List<TransactionResponse> txResponses = txs.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getAmount(), t.getTimestamp(), t.getType(), t.getCurrency()))
                .collect(Collectors.toList());

        return new AccountDetailsResponse(account.getId(), account.getBalance(), txResponses);
    }
}
