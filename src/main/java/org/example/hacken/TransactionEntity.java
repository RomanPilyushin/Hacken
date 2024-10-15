package org.example.hacken;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_from_address", columnList = "from_address"),
        @Index(name = "idx_to_address", columnList = "to_address")
})
@Data
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @Column(name = "transaction_hash", nullable = false, unique = true)
    private String transactionHash;

    @Column(name = "from_address", nullable = false)
    private String fromAddress;

    @Column(name = "to_address")
    private String toAddress;

    @Column(name = "tx_value", nullable = false)
    private String value;
}

