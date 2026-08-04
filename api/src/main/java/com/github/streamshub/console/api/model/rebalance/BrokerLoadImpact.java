package com.github.streamshub.console.api.model.rebalance;

import java.math.BigDecimal;

public record BrokerLoadImpact(
        BigDecimal before,
        BigDecimal after,
        BigDecimal diff
) {

}

