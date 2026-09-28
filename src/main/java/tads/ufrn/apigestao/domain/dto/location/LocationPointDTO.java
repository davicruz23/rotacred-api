package tads.ufrn.apigestao.domain.dto.location;

import java.time.LocalDateTime;

public record LocationPointDTO(
        Long id,
        Double latitude,
        Double longitude,
        LocalDateTime capturedAt,
        Long installmentId,
        Long saleId,
        String status,
        String color,
        Boolean withinRadius,
        Double amount
) {
}
