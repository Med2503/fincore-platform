package org.fincore.wealth.portfolio.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePortfolioRequest(

        @NotBlank
        @Size(min = 3, max = 120)
        String name,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3}$")
        String baseCurrency
) {
}