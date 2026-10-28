package com.minibank.repository;

import com.minibank.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // Explicit JPQL: derived queries on owner.id add a needless join with users
    @Query("select a from Account a where a.owner.id = :ownerId order by a.id")
    List<Account> findAllByOwnerId(Long ownerId);

    @Query("select a from Account a where a.id = :id and a.owner.id = :ownerId")
    Optional<Account> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByAccountNumber(String accountNumber);
}
