package ru.tbank.tmap.loyalty.repository;

import java.util.UUID;

public record LoyaltyRuleUsageCount(UUID ruleId, long usages) {
}
