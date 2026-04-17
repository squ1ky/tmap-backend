package ru.tbank.tmap.generator.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.tbank.tmap.generator.TestcontainersConfiguration;
import ru.tbank.tmap.generator.TestFactory;
import ru.tbank.tmap.generator.domain.Venue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VenueRepositoryTest {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findAllActive_whenMixedStatuses_thenReturnsOnlyActive() {
        entityManager.persistAndFlush(TestFactory.venueWithStatus("Active Cafe", "ACTIVE"));
        entityManager.persistAndFlush(TestFactory.venueWithStatus("Pending Bar", "PENDING"));
        entityManager.persistAndFlush(TestFactory.venueWithStatus("Rejected Pub", "REJECTED"));
        entityManager.persistAndFlush(TestFactory.venueWithStatus("Active Restaurant", "ACTIVE"));

        List<Venue> active = venueRepository.findAllActive();

        assertThat(active)
                .hasSize(2)
                .extracting(Venue::getName)
                .containsExactlyInAnyOrder("Active Cafe", "Active Restaurant");
    }

    @Test
    void findAllActive_whenNoActiveVenues_thenReturnsEmptyList() {
        entityManager.persistAndFlush(TestFactory.venueWithStatus("Pending Bar", "PENDING"));
        entityManager.persistAndFlush(TestFactory.venueWithStatus("Rejected Pub", "REJECTED"));

        List<Venue> active = venueRepository.findAllActive();

        assertThat(active).isEmpty();
    }

    @Test
    void findAllActive_whenNoVenuesAtAll_thenReturnsEmptyList() {
        List<Venue> active = venueRepository.findAllActive();

        assertThat(active).isEmpty();
    }
}