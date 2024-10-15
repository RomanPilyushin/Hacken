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

// Mark this class as a REST controller that handles requests to `/api/transactions`
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction API", description = "Operations related to transactions") // Swagger tag for API documentation
public class TransactionController {

    // Inject Web3j for interacting with the Ethereum blockchain
    @Autowired
    private Web3j web3j;

    // Inject the repository for interacting with transaction data in the database
    @Autowired
    private TransactionRepository repository;

    // Endpoint to get a transaction by its hash
    @GetMapping("/{hash}")
    @Operation(summary = "Get Transaction by Hash", description = "Retrieve a transaction using its hash") // Swagger operation documentation
    public TransactionEntity getTransaction(@PathVariable String hash) throws IOException {
        // Check if the transaction already exists in the database
        Optional<TransactionEntity> optionalTx = repository.findById(hash);
        if (optionalTx.isPresent()) {
            return optionalTx.get(); // If found, return the transaction
        }

        // Fetch the transaction by hash from the blockchain using Web3j
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(hash).send();
        Optional<Transaction> transactionOptional = ethTransaction.getTransaction();

        // If the transaction exists on the blockchain
        if (transactionOptional.isPresent()) {
            Transaction tx = transactionOptional.get();
            // Map the blockchain transaction to TransactionEntity and save it to the database
            TransactionEntity entity = mapTransaction(tx);
            repository.save(entity); // Save the transaction entity
            return entity; // Return the saved transaction entity
        } else {
            // If the transaction was not found, return null or throw a custom exception
            return null;
        }
    }

    // Endpoint to retrieve all transactions from a specific sender address
    @GetMapping("/from/{address}")
    @Operation(summary = "Get Transactions From Address", description = "Retrieve transactions from a specific address")
    public List<TransactionEntity> getTransactionsFromAddress(@PathVariable String address) throws IOException {
        // Fetch and return all transactions from the specified address
        return repository.findByFromAddress(address);
    }

    // Endpoint to retrieve all transactions sent to a specific recipient address
    @GetMapping("/to/{address}")
    @Operation(summary = "Get Transactions To Address", description = "Retrieve transactions to a specific address")
    public List<TransactionEntity> getTransactionsToAddress(@PathVariable String address) throws IOException {
        // Fetch and return all transactions to the specified address
        return repository.findByToAddress(address);
    }

    // Endpoint to search for transactions using optional parameters (fromAddress, toAddress)
    @GetMapping("/search")
    @Operation(summary = "Search Transactions", description = "Search transactions based on criteria")
    public List<TransactionEntity> searchTransactions(
            @RequestParam(required = false) String fromAddress, // Optional query parameter for the sender's address
            @RequestParam(required = false) String toAddress) { // Optional query parameter for the recipient's address

        // If both fromAddress and toAddress are provided, search for transactions matching both
        if (fromAddress != null && toAddress != null) {
            return repository.findByFromAddressAndToAddress(fromAddress, toAddress);
        }
        // If only fromAddress is provided, search by sender's address
        else if (fromAddress != null) {
            return repository.findByFromAddress(fromAddress);
        }
        // If only toAddress is provided, search by recipient's address
        else if (toAddress != null) {
            return repository.findByToAddress(toAddress);
        }
        // If no parameters are provided, return all transactions
        else {
            return repository.findAll();
        }
    }

    // Helper method to map a Web3j Transaction object to a TransactionEntity for database storage
    private TransactionEntity mapTransaction(Transaction tx) {
        // Create a new TransactionEntity and set its fields
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionHash(tx.getHash()); // Set the transaction hash
        entity.setFromAddress(tx.getFrom()); // Set the sender's address
        entity.setToAddress(tx.getTo()); // Set the recipient's address
        entity.setValue(tx.getValue().toString()); // Set the transaction value as a string
        // Additional fields like block number or timestamp could be set here if needed
        return entity; // Return the mapped TransactionEntity
    }
}
