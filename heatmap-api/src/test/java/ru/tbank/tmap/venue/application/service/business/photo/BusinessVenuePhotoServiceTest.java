package ru.tbank.tmap.venue.application.service.business.photo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.tbank.tmap.venue.application.service.photo.VenuePhotoValidator;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.domain.VenueTestFactory;
import ru.tbank.tmap.venue.domain.exception.VenueNotFoundException;
import ru.tbank.tmap.venue.application.port.VenuePhotoStorage;
import ru.tbank.tmap.venue.domain.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class BusinessVenuePhotoServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String NEW_KEY = "venues/" + VENUE_ID + "/new.jpg";

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenuePhotoValidator venuePhotoValidator;

    @Mock
    private VenuePhotoStorage venuePhotoStorage;

    @Mock
    private BusinessVenuePhotoUpdater venuePhotoUpdater;

    private BusinessVenuePhotoService businessVenuePhotoService;

    @BeforeEach
    void setUp() {
        businessVenuePhotoService = new BusinessVenuePhotoService(
                venueRepository,
                venuePhotoValidator,
                venuePhotoStorage,
                venuePhotoUpdater
        );
    }

    @Test
    void uploadVenuePhoto_whenSuccessful_thenReturnsVenueAndDoesNotTouchStorageCleanup() {
        final Venue updated = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        updated.setPhotoObjectKey(NEW_KEY);
        final MultipartFile file = jpegFile();

        given(venuePhotoValidator.validateAndGetExtension(file)).willReturn("jpg");
        given(venueRepository.existsByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(true);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(Optional.of(updated));
        given(venuePhotoStorage.upload(eq(VENUE_ID), any(), anyLong(), eq("image/jpeg"), eq("jpg")))
                .willReturn(NEW_KEY);
        willDoNothing().given(venuePhotoUpdater).swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        final Venue result = businessVenuePhotoService.uploadVenuePhoto(OWNER_ID, VENUE_ID, file);

        assertThat(result.getPhotoObjectKey()).isEqualTo(NEW_KEY);
        verify(venuePhotoStorage, never()).delete(any());
    }

    @Test
    void uploadVenuePhoto_whenDbUpdateFails_thenDeletesNewlyUploadedObject() {
        final MultipartFile file = jpegFile();

        given(venuePhotoValidator.validateAndGetExtension(file)).willReturn("jpg");
        given(venueRepository.existsByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(true);
        given(venuePhotoStorage.upload(eq(VENUE_ID), any(), anyLong(), eq("image/jpeg"), eq("jpg")))
                .willReturn(NEW_KEY);
        willThrow(new RuntimeException("DB write failed"))
                .given(venuePhotoUpdater).swapPhotoKey(VENUE_ID, OWNER_ID, NEW_KEY);

        assertThatThrownBy(() -> businessVenuePhotoService.uploadVenuePhoto(OWNER_ID, VENUE_ID, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB write failed");
        verify(venuePhotoStorage).delete(NEW_KEY);
    }

    @Test
    void uploadVenuePhoto_whenVenueDoesNotBelongToOwner_thenThrowsAndDoesNotTouchStorage() {
        final MultipartFile file = jpegFile();

        given(venueRepository.existsByIdAndOwnerId(VENUE_ID, OWNER_ID)).willReturn(false);

        assertThatThrownBy(() -> businessVenuePhotoService.uploadVenuePhoto(OWNER_ID, VENUE_ID, file))
                .isInstanceOf(VenueNotFoundException.class);
        verify(venuePhotoStorage, never()).upload(any(), any(), anyLong(), any(), any());
        verify(venuePhotoStorage, never()).delete(any());
    }

    @Test
    void deleteVenuePhoto_returnsVenueAndDelegatesCleanupToEvents() {
        final Venue updated = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);

        willDoNothing().given(venuePhotoUpdater).clearPhotoKey(VENUE_ID, OWNER_ID);
        given(venueRepository.findByIdAndOwnerId(VENUE_ID, OWNER_ID))
                .willReturn(Optional.of(updated));

        final Venue result = businessVenuePhotoService.deleteVenuePhoto(OWNER_ID, VENUE_ID);

        assertThat(result.getPhotoObjectKey()).isNull();
        verify(venuePhotoStorage, never()).delete(any());
    }

    private MultipartFile jpegFile() {
        return new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );
    }
}
