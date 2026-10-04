package org.fincore.wealth;

import org.fincore.wealth.position.service.MarketPriceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MarketPriceProperties.class)
public class WealthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WealthServiceApplication.class, args);
    }

}
