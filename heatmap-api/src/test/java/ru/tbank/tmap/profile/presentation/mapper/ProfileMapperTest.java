package ru.tbank.tmap.profile.presentation.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import ru.tbank.tmap.loyalty.application.query.LoyaltyHistoryProjection;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileMapperTest {

    private final ProfileMapper profileMapper = new ProfileMapper();

    @Test
    void toHistoryPageResponse_whenVenueCategoryPresent_thenMapsCategory() {
        final LoyaltyHistoryProjection item = new LoyaltyHistoryProjection(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Cafe One",
                "FOOD",
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "15% off cappuccino",
                15,
                OffsetDateTime.parse("2026-05-16T12:30:00Z")
        );

        final var response = profileMapper.toHistoryPageResponse(new PageImpl<>(java.util.List.of(item)));

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getCategory()).isEqualTo(
                org.openapitools.model.LoyaltyVerificationResponse.CategoryEnum.FOOD
        );
    }
}
