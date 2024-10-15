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

@Service
public class Web3Service {

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
                    processBlock(block);
                    // Update last processed block number
                    lastProcessedBlock = block.getBlock().getNumber();
                    saveLastProcessedBlock(lastProcessedBlock);
                }, error -> {
                    // Handle errors
                    System.err.println("Error in block subscription: " + error.getMessage());
                });
    }

    private void processBlock(EthBlock block) {
        List<TransactionEntity> entities = block.getBlock().getTransactions().stream()
                .map(txResult -> mapToEntity((Transaction) txResult.get()))
                .collect(Collectors.toList());
        transactionRepository.saveAll(entities);
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    private TransactionEntity mapToEntity(Transaction tx) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionHash(tx.getHash());
        entity.setFromAddress(tx.getFrom());
        entity.setToAddress(tx.getTo());
        entity.setValue(tx.getValue() != null ? tx.getValue().toString() : BigInteger.ZERO.toString());
        return entity;
    }

    // Implementing loadLastProcessedBlock
    private BigInteger loadLastProcessedBlock() {
        Optional<ProcessedBlock> optional = processedBlockRepository.findById(1);
        if (optional.isPresent()) {
            return optional.get().getBlockNumber();
        } else {
            // If no record exists, start from the latest block
            try {
                EthBlockNumber latestBlock = web3j.ethBlockNumber().send();
                return latestBlock.getBlockNumber();
            } catch (IOException e) {
                throw new RuntimeException("Error fetching latest block number", e);
            }
        }
    }

    // Implementing saveLastProcessedBlock
    private void saveLastProcessedBlock(BigInteger blockNumber) {
        ProcessedBlock processedBlock = new ProcessedBlock(blockNumber);
        processedBlockRepository.save(processedBlock);
    }
}
