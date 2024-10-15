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
import java.util.*;
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
        lastProcessedBlock = loadLastProcessedBlock();  // Load last processed block from the DB

        subscription = web3j.replayPastAndFutureBlocksFlowable(
                        new DefaultBlockParameterNumber(lastProcessedBlock), true)
                .subscribe(block -> {
                    try {
                        processBlock(block);  // Process each new block
                        lastProcessedBlock = block.getBlock().getNumber();  // Update last processed block number
                        saveLastProcessedBlock(lastProcessedBlock);  // Save to DB after processing each block
                    } catch (Exception e) {
                        System.err.println("Error processing block: " + e.getMessage());
                    }
                }, error -> {
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
        int batchSize = 100;  // You can adjust the batch size based on the database performance
        List<List<TransactionEntity>> batches = partitionList(newEntities, batchSize);

        for (List<TransactionEntity> batch : batches) {
            try {
                transactionRepository.saveAll(batch);  // Save the batch of transactions
                // Print each transaction's details after it's saved
                batch.forEach(entity -> {
                    System.out.println("Transaction saved: From Address: " + entity.getFromAddress() +
                            " To Address: " + entity.getToAddress());
                });
            } catch (Exception e) {
                System.err.println("Error saving batch: " + e.getMessage());
            }
        }
    }

    // Helper method to partition the list into smaller batches
    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
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
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        // Ensure the last processed block is saved before the service shuts down
        if (lastProcessedBlock != null) {
            saveLastProcessedBlock(lastProcessedBlock);
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
