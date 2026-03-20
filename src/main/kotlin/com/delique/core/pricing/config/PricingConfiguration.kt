package com.delique.core.pricing.config

import com.delique.core.pricing.domain.pipeline.DefaultPricingStrategy
import com.delique.core.pricing.domain.pipeline.FullMarkupPriceStep
import com.delique.core.pricing.domain.pipeline.PricingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PricingConfiguration {
    @Bean
    fun defaultPricingStrategy(): PricingStrategy =
        DefaultPricingStrategy(listOf(FullMarkupPriceStep()))
}
