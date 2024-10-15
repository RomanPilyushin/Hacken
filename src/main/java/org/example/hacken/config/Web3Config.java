package org.example.hacken.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {

    @Value("${infura.project.id}")
    private String infuraProjectId;

    @Bean
    public Web3j web3j() {
        if (infuraProjectId == null || infuraProjectId.isEmpty()) {
            throw new IllegalStateException("Infura Project ID is not set in application.properties.");
        }

        String infuraUrl = "https://mainnet.infura.io/v3/" + infuraProjectId;
        return Web3j.build(new HttpService(infuraUrl));
    }
}
