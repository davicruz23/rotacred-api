package tads.ufrn.apigestao.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tads.ufrn.apigestao.domain.dto.location.CollectorRouteDTO;
import tads.ufrn.apigestao.domain.dto.location.CollectorTrackingDTO;
import tads.ufrn.apigestao.service.TrackingService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

//    @PreAuthorize("hasAnyRole('COBRADOR')")
    @GetMapping("/collectors")
    public ResponseEntity<List<CollectorTrackingDTO>>
    getCollectorsTracking() {
        return ResponseEntity.ok(
                trackingService.getCollectorsTracking()
        );
    }

//    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping("/collectors/{userId}/route")
    public ResponseEntity<CollectorRouteDTO> getCollectorRoute(
            @PathVariable Long userId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDate start,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDate end
    ) {
        return ResponseEntity.ok(
                trackingService.getCollectorRoute(
                        userId,
                        start,
                        end
                )
        );
    }
}