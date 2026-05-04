package ru.tbank.tmap.loyalty.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willDoNothing;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.openapitools.model.LoyaltyActivationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ru.tbank.tmap.loyalty.application.command.RedeemLoyaltyRuleCommand;
import ru.tbank.tmap.loyalty.application.port.VenueOwnershipPort;
import ru.tbank.tmap.loyalty.domain.LoyaltyRule;
import ru.tbank.tmap.loyalty.domain.LoyaltyRuleRepository;
import ru.tbank.tmap.loyalty.domain.LoyaltyVerificationRepository;

@SpringBootTest
@Import(BusinessLoyaltyRuleActivationConcurrencyTest.PostgresTestConfiguration.class)
@ActiveProfiles("test")
class BusinessLoyaltyRuleActivationConcurrencyTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FIRST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private BusinessLoyaltyRuleService businessLoyaltyRuleService;

    @Autowired
    private LoyaltyRuleRepository loyaltyRuleRepository;

    @Autowired
    private LoyaltyVerificationRepository loyaltyVerificationRepository;

    @MockitoBean
    private VenueOwnershipPort venueOwnershipPort;

    @Test
    void redeemLoyaltyRule_whenConcurrentRequestsWithMaxUsageOne_thenOnlyOneSucceeds() throws Exception {
        willDoNothing().given(venueOwnershipPort).requireOwner(VENUE_ID, OWNER_ID);

        final UUID ruleId = UUID.randomUUID();
        loyaltyRuleRepository.save(new LoyaltyRule(ruleId, VENUE_ID, "Concurrent test rule", 15, 1));

        final RedeemLoyaltyRuleCommand firstCommand =
                new RedeemLoyaltyRuleCommand(OWNER_ID, ruleId, FIRST_USER_ID, VENUE_ID);
        final RedeemLoyaltyRuleCommand secondCommand =
                new RedeemLoyaltyRuleCommand(OWNER_ID, ruleId, SECOND_USER_ID, VENUE_ID);

        final List<LoyaltyActivationStatus> statuses = runConcurrently(firstCommand, secondCommand);

        assertThat(statuses).containsExactlyInAnyOrder(
                LoyaltyActivationStatus.SUCCESS,
                LoyaltyActivationStatus.LIMIT_EXCEEDED
        );
        assertThat(loyaltyVerificationRepository.countByRuleId(ruleId)).isEqualTo(1);
    }

    @Test
    void redeemLoyaltyRule_whenConcurrentRequestsForSameUser_thenSecondIsRejectedAsAlreadyUsed() throws Exception {
        willDoNothing().given(venueOwnershipPort).requireOwner(VENUE_ID, OWNER_ID);

        final UUID ruleId = UUID.randomUUID();
        loyaltyRuleRepository.save(new LoyaltyRule(ruleId, VENUE_ID, "Same user race test", 10, 10));

        final RedeemLoyaltyRuleCommand firstCommand =
                new RedeemLoyaltyRuleCommand(OWNER_ID, ruleId, FIRST_USER_ID, VENUE_ID);
        final RedeemLoyaltyRuleCommand secondCommand =
                new RedeemLoyaltyRuleCommand(OWNER_ID, ruleId, FIRST_USER_ID, VENUE_ID);

        final List<LoyaltyActivationStatus> statuses = runConcurrently(firstCommand, secondCommand);

        assertThat(statuses).containsExactlyInAnyOrder(
                LoyaltyActivationStatus.SUCCESS,
                LoyaltyActivationStatus.ALREADY_USED
        );
        assertThat(loyaltyVerificationRepository.countByRuleId(ruleId)).isEqualTo(1);
    }

    private List<LoyaltyActivationStatus> runConcurrently(
            final RedeemLoyaltyRuleCommand first,
            final RedeemLoyaltyRuleCommand second
    ) throws InterruptedException, ExecutionException {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch startGate = new CountDownLatch(1);
        try {
            final Future<LoyaltyActivationStatus> firstFuture = executor.submit(task(first, startGate));
            final Future<LoyaltyActivationStatus> secondFuture = executor.submit(task(second, startGate));

            startGate.countDown();

            final LoyaltyActivationStatus firstStatus = firstFuture.get();
            final LoyaltyActivationStatus secondStatus = secondFuture.get();
            return List.of(firstStatus, secondStatus);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private Callable<LoyaltyActivationStatus> task(
            final RedeemLoyaltyRuleCommand command,
            final CountDownLatch startGate
    ) {
        return () -> {
            startGate.await(5, TimeUnit.SECONDS);
            return businessLoyaltyRuleService.redeemLoyaltyRule(command).status();
        };
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresTestConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>(DockerImageName
                    .parse("postgres:16-alpine")
                    .asCompatibleSubstituteFor("postgres"));
        }
    }
}
