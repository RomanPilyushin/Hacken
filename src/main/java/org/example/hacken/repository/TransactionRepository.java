package org.example.hacken.repository;

import org.example.hacken.model.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findByFromAddress(String fromAddress);

    List<TransactionEntity> findByToAddress(String toAddress);

    List<TransactionEntity> findByFromAddressAndToAddress(String fromAddress, String toAddress);

    @Query("SELECT t.transactionHash FROM TransactionEntity t WHERE t.transactionHash IN :hashes")
    List<String> findExistingTransactionHashes(List<String> hashes);
}
