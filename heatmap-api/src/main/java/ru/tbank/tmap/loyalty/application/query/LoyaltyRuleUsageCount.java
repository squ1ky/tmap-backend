package ru.tbank.tmap.loyalty.application.query;

import java.util.UUID;

public record LoyaltyRuleUsageCount(UUID ruleId, long usages) {
}