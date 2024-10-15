package org.example.hacken.service;

import io.reactivex.disposables.Disposable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.example.hacken.model.ProcessedBlock;
import org.example.hacken.repository.ProcessedBlockRepository;
import org.example.hacken.model.TransactionEntity;
import org.example.hacken.repository.TransactionRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Mark this class as a service, making it a Spring component
@Service
public class TransactionSyncService {

    // Inject the Web3j instance to interact with the Ethereum blockchain
    @Autowired
    private Web3j web3j;

    // Inject the repository to manage transaction entities in the database
    @Autowired
    private TransactionRepository transactionRepository;

    // Inject the repository to manage processed blocks in the database
    @Autowired
    private ProcessedBlockRepository processedBlockRepository;

    // Disposable to manage the blockchain subscription lifecycle
    private Disposable subscription;

    // Store the last processed block number to ensure continuity
    private BigInteger lastProcessedBlock;

    // This method is executed after the service is initialized (post-construction)
    @PostConstruct
    public void start() {
        // Load the last processed block from the database
        lastProcessedBlock = loadLastProcessedBlock();

        // Subscribe to replay past and future blocks starting from the last processed block
        subscription = web3j.replayPastAndFutureBlocksFlowable(
                        new DefaultBlockParameterNumber(lastProcessedBlock), true)
                .subscribe(block -> {
                    try {
                        // Process each new block
                        processBlock(block);
                        // Update the last processed block number
                        lastProcessedBlock = block.getBlock().getNumber();
                        // Save the last processed block number to the database
                        saveLastProcessedBlock(lastProcessedBlock);
                    } catch (Exception e) {
                        // Log any errors during block processing and continue
                        System.err.println("Error processing block: " + e.getMessage());
                    }
                }, error -> {
                    // Log subscription errors, ensuring the process continues
                    System.err.println("Error in block subscription: " + error.getMessage());
                });
    }

    // Process a single Ethereum block
    private void processBlock(EthBlock block) {
        // Convert the block's transactions into a list of Transaction objects
        List<Transaction> transactions = block.getBlock().getTransactions().stream()
                .map(txResult -> (Transaction) txResult.get())
                .collect(Collectors.toList());

        // If no transactions are present, skip further processing
        if (transactions.isEmpty()) {
            return;
        }

        // Extract the transaction hashes from the block
        List<String> txHashes = transactions.stream()
                .map(Transaction::getHash)
                .collect(Collectors.toList());

        try {
            // Fetch existing transaction hashes from the database to avoid duplicates
            List<String> existingTxHashes = transactionRepository.findExistingTransactionHashes(txHashes);
            Set<String> existingTxHashSet = new HashSet<>(existingTxHashes);

            // Filter out transactions that are already in the database
            List<TransactionEntity> newEntities = transactions.stream()
                    .filter(tx -> !existingTxHashSet.contains(tx.getHash()))
                    .map(this::mapTransaction) // Convert each transaction into a TransactionEntity
                    .collect(Collectors.toList());

            // Save the new transactions with error handling
            saveNewTransactions(newEntities);
        } catch (Exception e) {
            // Log any errors during the transaction processing of this block
            System.err.println("Error processing transactions in block " + block.getBlock().getNumber() + ": " + e.getMessage());
        }
    }

    // Save the new transactions into the database with exception handling for constraint violations
    public void saveNewTransactions(List<TransactionEntity> newEntities) {
        for (TransactionEntity entity : newEntities) {
            try {
                // Attempt to save the entity in the database
                transactionRepository.save(entity);
                System.out.println("Transaction saved: " + entity.getTransactionHash());
            } catch (DataIntegrityViolationException e) {
                // Handle primary key (duplicate transaction hash) violations
                if (e.getCause() instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolation = (ConstraintViolationException) e.getCause();
                    if (constraintViolation.getConstraintName().equals("PUBLIC.PRIMARY_KEY_F")) {
                        // Log the duplicate transaction hash and skip it
                        System.err.println("Duplicate transaction hash found: " + entity.getTransactionHash());
                    } else {
                        // Log any other constraint violations
                        System.err.println("Other constraint violation: " + constraintViolation.getConstraintName());
                    }
                } else {
                    // Log any other data integrity issues
                    System.err.println("Data integrity issue: " + e.getMessage());
                }
            } catch (Exception e) {
                // Catch all other exceptions to ensure that the process continues
                System.err.println("Unexpected error while saving transaction: " + e.getMessage());
            }
        }
    }

    // Convert a Transaction object to a TransactionEntity for database storage
    private TransactionEntity mapTransaction(Transaction tx) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionHash(tx.getHash()); // Set the transaction hash
        entity.setFromAddress(tx.getFrom()); // Set the sender's address
        entity.setToAddress(tx.getTo()); // Set the recipient's address
        entity.setValue(tx.getValue() != null ? tx.getValue().toString() : "0"); // Set the transaction value (or 0 if null)
        return entity; // Return the populated TransactionEntity
    }

    // This method is executed before the service is destroyed (pre-destruction)
    @PreDestroy
    public void stop() {
        // Dispose of the blockchain subscription to stop receiving updates
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    // Load the last processed block number from the database
    private BigInteger loadLastProcessedBlock() {
        // Attempt to retrieve the last processed block from the database
        Optional<ProcessedBlock> optional = processedBlockRepository.findById(1);
        if (optional.isPresent()) {
            return optional.get().getBlockNumber(); // Return the stored block number
        } else {
            // If no block is found, start from the latest block on the blockchain
            try {
                EthBlockNumber latestBlock = web3j.ethBlockNumber().send();
                return latestBlock.getBlockNumber();
            } catch (Exception e) {
                // Throw an exception if there is an error fetching the block number
                throw new RuntimeException("Error fetching latest block number", e);
            }
        }
    }

    // Save the last processed block number to the database
    private void saveLastProcessedBlock(BigInteger blockNumber) {
        // Create a new ProcessedBlock object with the block number
        ProcessedBlock processedBlock = new ProcessedBlock(blockNumber);
        // Save the ProcessedBlock to the database
        processedBlockRepository.save(processedBlock);
    }
}
