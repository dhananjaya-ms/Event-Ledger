package com.dj.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private String id; // assigned from the gateway/path


    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    public Account() {
    }

    public Account(String id,  BigDecimal balance) {
        this.id = id;
        this.balance = balance == null ? BigDecimal.ZERO : balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

   
    public BigDecimal getBalance() {
        return balance == null ? BigDecimal.ZERO : balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance == null ? BigDecimal.ZERO : balance;
    }
}
