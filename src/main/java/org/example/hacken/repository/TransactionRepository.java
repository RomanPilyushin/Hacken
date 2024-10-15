package org.example.hacken.repository;

import org.example.hacken.model.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Defining the TransactionRepository interface which extends JpaRepository
// JpaRepository provides basic CRUD (Create, Read, Update, Delete) operations for the entity
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    // Finds all transactions by the sender's address (fromAddress)
    List<TransactionEntity> findByFromAddress(String fromAddress);

    // Finds all transactions by the recipient's address (toAddress)
    List<TransactionEntity> findByToAddress(String toAddress);

    // Finds all transactions by both sender's and recipient's addresses
    List<TransactionEntity> findByFromAddressAndToAddress(String fromAddress, String toAddress);

    // Custom query to find existing transaction hashes by a list of transaction hashes
    // This method uses a JPQL (Java Persistence Query Language) query to select only the transaction hashes
    // from the TransactionEntity table where the transaction hash is in the provided list (hashes).
    @Query("SELECT t.transactionHash FROM TransactionEntity t WHERE t.transactionHash IN :hashes")
    List<String> findExistingTransactionHashes(@Param("hashes") List<String> hashes);
}
