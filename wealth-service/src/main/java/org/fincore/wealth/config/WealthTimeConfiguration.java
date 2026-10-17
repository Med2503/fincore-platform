package org.fincore.wealth.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WealthTimeConfiguration {

    @Bean
    Clock wealthClock() {
        return Clock.systemUTC();
    }


}