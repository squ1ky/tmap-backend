package ru.tbank.tmap.loyalty.business.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.tbank.tmap.loyalty.business.BusinessLoyaltyRuleUpdateCommand;

public class AtLeastOneFieldPresentValidator
        implements ConstraintValidator<AtLeastOneFieldPresent, BusinessLoyaltyRuleUpdateCommand> {

    @Override
    public boolean isValid(
            final BusinessLoyaltyRuleUpdateCommand value,
            final ConstraintValidatorContext context
    ) {
        if (value == null) {
            return true;
        }
        return value.description() != null
                || value.discountPercent() != null
                || value.maxUsages() != null
                || value.active() != null;
    }
}
