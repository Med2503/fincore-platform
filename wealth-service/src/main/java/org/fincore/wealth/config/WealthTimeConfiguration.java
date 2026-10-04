package org.fincore.wealth.config;


import org.fincore.wealth.portfolio.domain.MarketPricePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class WealthTimeConfiguration {

    @Bean
    Clock wealthClock() {
        return Clock.systemUTC();
    }

    @Bean
    MarketPricePolicy marketPricePolicy() {
        return new MarketPricePolicy(Duration.ofMinutes(15));
    }
}