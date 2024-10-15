package org.example.hacken.service;

import io.reactivex.disposables.Disposable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.example.hacken.model.ProcessedBlock;
import org.example.hacken.model.TransactionEntity;
import org.example.hacken.repository.TransactionRepository;
import org.example.hacken.repository.ProcessedBlockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Marking this class as a Spring service component.
@Service
public class Web3Service {

    // Automatically inject the Web3j instance for Ethereum blockchain interaction.
    @Autowired
    private Web3j web3j;

    // Inject the repository for interacting with the transactions table.
    @Autowired
    private TransactionRepository transactionRepository;

    // Inject the repository for interacting with the processed blocks table.
    @Autowired
    private ProcessedBlockRepository processedBlockRepository;

    // Disposable object to manage the blockchain subscription.
    private Disposable subscription;

    // Variable to store the number of the last processed block.
    private BigInteger lastProcessedBlock;

    // This method is called when the service starts.
    @PostConstruct
    public void start() {
        // Load the last processed block from the database.
        lastProcessedBlock = loadLastProcessedBlock();

        // Subscribe to the blockchain to replay past and future blocks starting from the last processed block.
        subscription = web3j.replayPastAndFutureBlocksFlowable(
                        new DefaultBlockParameterNumber(lastProcessedBlock), true)
                // For each new block, process it and update the last processed block number.
                .subscribe(block -> {
                    processBlock(block);
                    // Update the last processed block number.
                    lastProcessedBlock = block.getBlock().getNumber();
                    saveLastProcessedBlock(lastProcessedBlock); // Save it to the database.
                }, error -> {
                    // Handle any errors that occur during the subscription.
                    System.err.println("Error in block subscription: " + error.getMessage());
                });
    }

    // Method to process each block received from the blockchain.
    private void processBlock(EthBlock block) {
        // Convert each transaction in the block into a TransactionEntity and collect them into a list.
        List<TransactionEntity> entities = block.getBlock().getTransactions().stream()
                .map(txResult -> mapToEntity((Transaction) txResult.get()))
                .collect(Collectors.toList());
        // Save all the transaction entities into the database.
        transactionRepository.saveAll(entities);
    }

    // This method is called when the service stops, e.g., during application shutdown.
    @PreDestroy
    public void stop() {
        // Dispose of the subscription to stop receiving block updates.
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    // Method to map a blockchain Transaction to a TransactionEntity object.
    private TransactionEntity mapToEntity(Transaction tx) {
        TransactionEntity entity = new TransactionEntity();
        // Set the transaction hash.
        entity.setTransactionHash(tx.getHash());
        // Set the from address.
        entity.setFromAddress(tx.getFrom());
        // Set the to address.
        entity.setToAddress(tx.getTo());
        // Set the value of the transaction, or 0 if the value is null.
        entity.setValue(tx.getValue() != null ? tx.getValue().toString() : BigInteger.ZERO.toString());
        return entity; // Return the populated TransactionEntity.
    }

    // Load the number of the last processed block from the database.
    private BigInteger loadLastProcessedBlock() {
        // Try to fetch the last processed block from the database.
        Optional<ProcessedBlock> optional = processedBlockRepository.findById(1);
        if (optional.isPresent()) {
            // If found, return the block number.
            return optional.get().getBlockNumber();
        } else {
            // If not found, fetch the latest block number from the blockchain.
            try {
                EthBlockNumber latestBlock = web3j.ethBlockNumber().send();
                return latestBlock.getBlockNumber();
            } catch (IOException e) {
                // Throw a runtime exception if fetching the latest block number fails.
                throw new RuntimeException("Error fetching latest block number", e);
            }
        }
    }

    // Save the last processed block number to the database.
    private void saveLastProcessedBlock(BigInteger blockNumber) {
        // Create a ProcessedBlock object and set its block number.
        ProcessedBlock processedBlock = new ProcessedBlock(blockNumber);
        // Save the ProcessedBlock object to the database.
        processedBlockRepository.save(processedBlock);
    }
}
