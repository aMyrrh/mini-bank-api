package com.minibank.repository;

import com.minibank.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // Explicit JPQL: derived queries on owner.id add a needless join with users
    @Query("select a from Account a where a.owner.id = :ownerId order by a.id")
    List<Account> findAllByOwnerId(Long ownerId);

    @Query("select a from Account a where a.id = :id and a.owner.id = :ownerId")
    Optional<Account> findByIdAndOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(Long id);

    // Returns only the id, so the account is not loaded into the persistence context before it is locked
    @Query("select a.id from Account a where a.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
