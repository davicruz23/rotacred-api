package tads.ufrn.apigestao.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tads.ufrn.apigestao.domain.Collector;
import tads.ufrn.apigestao.domain.CollectionAttempt;
import tads.ufrn.apigestao.domain.dto.location.CollectorRouteDTO;
import tads.ufrn.apigestao.domain.dto.location.CollectorTrackingDTO;
import tads.ufrn.apigestao.domain.dto.location.LocationPointDTO;
import tads.ufrn.apigestao.enums.AttemptType;
import tads.ufrn.apigestao.repository.CollectorRepository;
import tads.ufrn.apigestao.repository.CollectionAttemptRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final CollectorRepository collectorRepository;
    private final CollectionAttemptRepository collectionAttemptRepository;
    private final CollectorService collectorService;

    @Transactional(readOnly = true)
    public List<CollectorTrackingDTO> getCollectorsTracking() {
        return collectorRepository.findAll()
                .stream()
                .map(this::toTrackingDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CollectorRouteDTO getCollectorRoute(
            Long userId,
            LocalDate start,
            LocalDate end
    ) {
        Collector collector = collectorRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cobrador não encontrado para o usuário informado."
                        )
                );

        List<CollectionAttempt> attempts;

        if (start != null && end != null) {
            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            attempts = collectionAttemptRepository
                    .findAllByCollectorIdAndAttemptAtBetweenOrderByAttemptAtAsc(
                            collector.getId(),
                            startDateTime,
                            endDateTime
                    );
        } else {
            attempts = collectionAttemptRepository
                    .findAllByCollectorIdOrderByAttemptAtAsc(collector.getId());
        }

        List<LocationPointDTO> points = attempts
                .stream()
                .map(this::toLocationPointDTO)
                .toList();

        return new CollectorRouteDTO(
                collector.getId(),
                userId,
                getCollectorName(collector),
                points
        );
    }

    private CollectorTrackingDTO toTrackingDTO(Collector collector) {
        Long userId = collector.getUser().getId();

        CollectionAttempt latestAttempt = collectionAttemptRepository
                        .findTopByCollectorIdOrderByAttemptAtDesc(collector.getId())
                        .orElse(null);

        if (latestAttempt == null) {
            return new CollectorTrackingDTO(
                    collector.getId(),
                    userId,
                    getCollectorName(collector),
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                    null
            );
        }

        String status = statusOf(latestAttempt);
        boolean withinRadius = withinRadius(latestAttempt);
        String color = colorOf(latestAttempt, withinRadius);

        return new CollectorTrackingDTO(
                collector.getId(),
                userId,
                getCollectorName(collector),
                latestAttempt.getLatitude(),
                latestAttempt.getLongitude(),
                latestAttempt.getAttemptAt(),
                true,
                status,
                color,
                withinRadius
        );
    }

    private LocationPointDTO toLocationPointDTO(CollectionAttempt attempt) {
        boolean withinRadius = withinRadius(attempt);

        return new LocationPointDTO(
                attempt.getId(),
                attempt.getLatitude(),
                attempt.getLongitude(),
                attempt.getAttemptAt(),
                attempt.getInstallment().getId(),
                attempt.getInstallment().getSale().getId(),
                statusOf(attempt),
                colorOf(attempt, withinRadius),
                withinRadius,
                attempt.getAmount()
        );
    }

    private String statusOf(CollectionAttempt attempt) {
        return AttemptType.PAYMENT.equals(attempt.getType()) ? "PAID" : "NOT_PAID";
    }

    private String colorOf(CollectionAttempt attempt, boolean withinRadius) {
        return AttemptType.PAYMENT.equals(attempt.getType()) && withinRadius
                ? "GREEN"
                : "RED";
    }

    private boolean withinRadius(CollectionAttempt attempt) {
        try {
            return collectorService.isAttemptWithinApprovalLocation(attempt);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String getCollectorName(Collector collector) {
        return collector.getUser().getName();
    }
}
