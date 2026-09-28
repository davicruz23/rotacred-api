package tads.ufrn.apigestao.domain.dto.location;

import java.time.LocalDateTime;

public record CollectorTrackingDTO(
        Long collectorId,
        Long userId,
        String collectorName,
        Double latitude,
        Double longitude,
        LocalDateTime lastLocationAt,
        Boolean online,
        String status,
        String color,
        Boolean withinRadius
) {
}
