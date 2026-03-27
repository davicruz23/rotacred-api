package tads.ufrn.apigestao.domain.dto.collector;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tads.ufrn.apigestao.domain.CollectionAttempt;
import tads.ufrn.apigestao.domain.Collector;
import tads.ufrn.apigestao.domain.Installment;
import tads.ufrn.apigestao.enums.AttemptType;
import tads.ufrn.apigestao.enums.PaymentType;

import java.time.LocalDateTime;
import java.util.Locale;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionAttemptDTO {

    private Long id;
    private LocalDateTime attemptAt = LocalDateTime.now();
    private AttemptType type;
    private Double amount;
    private PaymentType paymentMethod;
    private Double latitude;
    private Double longitude;
    private LocalDateTime newDueDate;
    private String note;
    private LocalDateTime serverRecordedAt = LocalDateTime.now();
    private String mapsUrl;

    public CollectionAttemptDTO(CollectionAttempt entity) {
        this.id = entity.getId();
        this.amount = entity.getAmount();
        this.paymentMethod = entity.getPaymentMethod();
        this.latitude = entity.getLatitude();
        this.longitude = entity.getLongitude();
        this.note = entity.getNote();
        this.newDueDate = entity.getNewDueDate();
        this.mapsUrl = generateMapsUrl(entity.getLatitude(), entity.getLongitude());
    }

    private String generateMapsUrl(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return null;
        return String.format(Locale.US,
                "https://www.google.com/maps/search/?api=1&query=%f,%f",
                latitude, longitude
        );
    }
}
