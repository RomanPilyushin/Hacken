package org.example.hacken.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.example.hacken.model.TransactionEntity;
import org.example.hacken.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction API", description = "Operations related to transactions")
public class TransactionController {

    @Autowired
    private Web3j web3j;

    @Autowired
    private TransactionRepository repository;

    @GetMapping("/{hash}")
    @Operation(summary = "Get Transaction by Hash", description = "Retrieve a transaction using its hash")
    public TransactionEntity getTransaction(@PathVariable String hash) throws IOException {
        // Check if the transaction exists in the database
        Optional<TransactionEntity> optionalTx = repository.findById(hash);
        if (optionalTx.isPresent()) {
            return optionalTx.get();
        }

        // Fetch transaction by hash using Web3j
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(hash).send();
        Optional<Transaction> transactionOptional = ethTransaction.getTransaction();

        if (transactionOptional.isPresent()) {
            Transaction tx = transactionOptional.get();
            // Map the transaction and store in the database
            TransactionEntity entity = mapTransaction(tx);
            repository.save(entity);
            return entity;
        } else {
            // Transaction not found
            return null; // Or throw a custom exception if preferred
        }
    }


    @GetMapping("/from/{address}")
    @Operation(summary = "Get Transactions From Address", description = "Retrieve transactions from a specific address")
    public List<TransactionEntity> getTransactionsFromAddress(@PathVariable String address) throws IOException {
        // Fetch transactions from the database
        return repository.findByFromAddress(address);
    }

    @GetMapping("/to/{address}")
    @Operation(summary = "Get Transactions To Address", description = "Retrieve transactions to a specific address")
    public List<TransactionEntity> getTransactionsToAddress(@PathVariable String address) throws IOException {
        return repository.findByToAddress(address);
    }

    // Additional search endpoint
    @GetMapping("/search")
    @Operation(summary = "Search Transactions", description = "Search transactions based on criteria")
    public List<TransactionEntity> searchTransactions(
            @RequestParam(required = false) String fromAddress,
            @RequestParam(required = false) String toAddress) {

        if (fromAddress != null && toAddress != null) {
            return repository.findByFromAddressAndToAddress(fromAddress, toAddress);
        } else if (fromAddress != null) {
            return repository.findByFromAddress(fromAddress);
        } else if (toAddress != null) {
            return repository.findByToAddress(toAddress);
        } else {
            return repository.findAll();
        }
    }

    // Helper method to map Web3j Transaction to TransactionEntity
    private TransactionEntity mapTransaction(Transaction tx) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionHash(tx.getHash());
        entity.setFromAddress(tx.getFrom());
        entity.setToAddress(tx.getTo());
        entity.setValue(tx.getValue().toString());
        // Set other fields if necessary, like block number, timestamp, etc.
        return entity;
    }
}
