package fr.pace.activity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SortieControllerTest {
    @Test
    @DisplayName("Devrait retourner directement les sorties avec la limite demandée")
    void recent_shouldReturnActivities_whenLimitIsProvided() {
        // GIVEN
        SortieReadService service = mock(SortieReadService.class);
        List<SortieResponse> expected = List.of(new SortieResponse(
                UUID.randomUUID(),
                Instant.parse("2026-08-21T07:00:00Z"),
                "running",
                10_000L,
                3_600L,
                "GARMIN_PERSONNEL"
        ));
        when(service.recent(3)).thenReturn(expected);
        SortieController controller = new SortieController(service, mock(SortieDetailService.class));

        // WHEN
        List<SortieResponse> result = controller.recent(3);

        // THEN
        assertThat(result).isEqualTo(expected);
        verify(service).recent(3);
    }

    @Test
    @DisplayName("Devrait déléguer la consultation du détail d'une sortie")
    void get_shouldReturnDetail_whenActivityExists() {
        // GIVEN
        SortieReadService readService = mock(SortieReadService.class);
        SortieDetailService detailService = mock(SortieDetailService.class);
        UUID id = UUID.randomUUID();
        SortieDetailResponse expected = new SortieDetailResponse(
                id, null, null, null, "GARMIN_PERSONNEL", "A_DECODER", null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), 0, false
        );
        when(detailService.get(id)).thenReturn(expected);

        // WHEN
        SortieDetailResponse result = new SortieController(readService, detailService).get(id);

        // THEN
        assertThat(result).isEqualTo(expected);
        verify(detailService).get(id);
    }
}
