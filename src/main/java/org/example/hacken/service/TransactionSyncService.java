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
@Service
public class TransactionSyncService {

    @Autowired
    private Web3j web3j;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProcessedBlockRepository processedBlockRepository;

    private Disposable subscription;

    private BigInteger lastProcessedBlock;

    @PostConstruct
    public void start() {
        lastProcessedBlock = loadLastProcessedBlock();

        subscription = web3j.replayPastAndFutureBlocksFlowable(
                        new DefaultBlockParameterNumber(lastProcessedBlock), true)
                .subscribe(block -> {
                    try {
                        processBlock(block);
                        // Update last processed block number
                        lastProcessedBlock = block.getBlock().getNumber();
                        saveLastProcessedBlock(lastProcessedBlock);
                    } catch (Exception e) {
                        // Log and continue processing other blocks
                        System.err.println("Error processing block: " + e.getMessage());
                    }
                }, error -> {
                    // Log subscription errors and continue processing other blocks
                    System.err.println("Error in block subscription: " + error.getMessage());
                });
    }

    private void processBlock(EthBlock block) {
        List<Transaction> transactions = block.getBlock().getTransactions().stream()
                .map(txResult -> (Transaction) txResult.get())
                .collect(Collectors.toList());

        if (transactions.isEmpty()) {
            return;
        }

        // Get the list of transaction hashes from the block
        List<String> txHashes = transactions.stream()
                .map(Transaction::getHash)
                .collect(Collectors.toList());

        try {
            // Fetch existing transaction hashes from the database
            List<String> existingTxHashes = transactionRepository.findExistingTransactionHashes(txHashes);
            Set<String> existingTxHashSet = new HashSet<>(existingTxHashes);

            // Filter out transactions that already exist
            List<TransactionEntity> newEntities = transactions.stream()
                    .filter(tx -> !existingTxHashSet.contains(tx.getHash()))
                    .map(this::mapTransaction)
                    .collect(Collectors.toList());

            // Save only new transactions with exception handling
            saveNewTransactions(newEntities);
        } catch (Exception e) {
            // Log and continue with the next block
            System.err.println("Error processing transactions in block " + block.getBlock().getNumber() + ": " + e.getMessage());
        }
    }

    public void saveNewTransactions(List<TransactionEntity> newEntities) {
        for (TransactionEntity entity : newEntities) {
            try {
                transactionRepository.save(entity);
                System.out.println("Transaction saved: " + entity.getTransactionHash());
            } catch (DataIntegrityViolationException e) {
                // Handle primary key violations specifically
                if (e.getCause() instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolation = (ConstraintViolationException) e.getCause();
                    if (constraintViolation.getConstraintName().equals("PUBLIC.PRIMARY_KEY_F")) {
                        // Log the duplicate and skip it
                        System.err.println("Duplicate transaction hash found: " + entity.getTransactionHash());
                    } else {
                        System.err.println("Other constraint violation: " + constraintViolation.getConstraintName());
                    }
                } else {
                    System.err.println("Data integrity issue: " + e.getMessage());
                }
            } catch (Exception e) {
                // Catch all other exceptions to ensure the process continues
                System.err.println("Unexpected error while saving transaction: " + e.getMessage());
            }
        }
    }






    private TransactionEntity mapTransaction(Transaction tx) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionHash(tx.getHash());
        entity.setFromAddress(tx.getFrom());
        entity.setToAddress(tx.getTo());
        entity.setValue(tx.getValue() != null ? tx.getValue().toString() : "0");
        return entity;
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    private BigInteger loadLastProcessedBlock() {
        Optional<ProcessedBlock> optional = processedBlockRepository.findById(1);
        if (optional.isPresent()) {
            return optional.get().getBlockNumber();
        } else {
            // If no record exists, start from the latest block
            try {
                EthBlockNumber latestBlock = web3j.ethBlockNumber().send();
                return latestBlock.getBlockNumber();
            } catch (Exception e) {
                throw new RuntimeException("Error fetching latest block number", e);
            }
        }
    }

    private void saveLastProcessedBlock(BigInteger blockNumber) {
        ProcessedBlock processedBlock = new ProcessedBlock(blockNumber);
        processedBlockRepository.save(processedBlock);
    }
}

