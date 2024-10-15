package org.example.hacken;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Setter;

import java.math.BigInteger;

@Entity
@Table(name = "processed_block")
public class ProcessedBlock {

    @Id
    private int id = 1; // Using a fixed ID since there's only one record

    @Setter
    private BigInteger blockNumber;

    // Constructors
    public ProcessedBlock() {}

    public ProcessedBlock(BigInteger blockNumber) {
        this.blockNumber = blockNumber;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public BigInteger getBlockNumber() {
        return blockNumber;
    }

}
