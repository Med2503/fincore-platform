package org.fincore.wealth.config;


import org.fincore.wealth.position.service.MarketPricePolicy;
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


}