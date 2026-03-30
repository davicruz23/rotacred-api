package tads.ufrn.apigestao.controller;

import com.google.zxing.WriterException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tads.ufrn.apigestao.controller.mapper.CollectorMapper;
import tads.ufrn.apigestao.domain.CollectionAttempt;
import tads.ufrn.apigestao.domain.Collector;
import tads.ufrn.apigestao.domain.Installment;
import tads.ufrn.apigestao.domain.Sale;
import tads.ufrn.apigestao.domain.dto.LocationSaleDTO;
import tads.ufrn.apigestao.domain.dto.collector.*;
import tads.ufrn.apigestao.domain.dto.commissionHistory.CommissionHistoryDTO;
import tads.ufrn.apigestao.domain.dto.inspector.InspectorIdUserDTO;
import tads.ufrn.apigestao.domain.dto.installment.InstallmentPaidDTO;
import tads.ufrn.apigestao.domain.dto.sale.AssignSalesCollectorRequest;
import tads.ufrn.apigestao.domain.dto.sale.CitySalesDTO;
import tads.ufrn.apigestao.domain.dto.sale.SaleCollectorDTO;
import tads.ufrn.apigestao.enums.PaymentType;
import tads.ufrn.apigestao.service.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/collector")
public class CollectorController {

    private final CollectorService service;
    private final CollectionAttemptService collectionAttemptService;
    private final PixService pixService;
    private final ApprovalLocationService approvalLocationService;
    private final SaleService saleService;
    private final CommissionHistoryService commissionHistoryService;

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @PostMapping("/{collectorId}/assign/{city}")
    public ResponseEntity<CollectorSalesAssignedDTO> assignSalesByCity(@PathVariable Long collectorId, @PathVariable String city) {

        return ResponseEntity.ok(service.assignSalesByCity(collectorId, city));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping("/grouped-by-city/assigment")
    public ResponseEntity<List<CitySalesDTO>> salesGroupedByCityAssigment() {
        return ResponseEntity.ok(saleService.salesGroupedByCityAssignment());
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @PostMapping("/assign-sales")
    public ResponseEntity<Void> assignSalesToCollector(@RequestBody AssignSalesCollectorRequest request) {
        saleService.assignSalesToCollector(request.collectorId(), request.saleIds());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/all")
    public ResponseEntity<List<CollectorDTO>> findAll() {
        return ResponseEntity.ok(service.findAll().stream().map(CollectorMapper::mapper).toList());
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/all/sales")
    public ResponseEntity<List<CollectorDTO>> findAll(@RequestParam(required = false) Integer status, @RequestParam(required = false) Long collectorId, @RequestParam(required = false) Boolean fullPaid) {
        return ResponseEntity.ok(service.findAll(status, collectorId, fullPaid));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/name/all")
    public ResponseEntity<List<CollectorDTO>> findAllByName() {
        return ResponseEntity.ok(service.findAll().stream().map(CollectorMapper::mapperName).toList());
    }
//    @GetMapping("/{collectorId}/sales")
//    public ResponseEntity<List<CollectorSalesDTO>> getCollectorSales(@PathVariable Long collectorId) {
//        List<CollectorSalesDTO> salesDTO = service.getSalesByCollectorDTO(collectorId);
//        return ResponseEntity.ok(salesDTO);
//    }

//    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
//    @PutMapping("/{id}/pay")
//    public ResponseEntity<?> payInstallment(@PathVariable Long id, @RequestParam BigDecimal amount) {
//        System.out.println("chamei primeiro payinstallment");
//        try {
//            InstallmentPaidDTO paidInstallment =
//                    service.markAsPaid(id, amount);
//
//            return ResponseEntity.ok(paidInstallment);
//
//        } catch (RuntimeException e) {
//            return ResponseEntity
//                    .badRequest()
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }


    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/{id}/commission")
    public ResponseEntity<CollectorCommissionDTO> getCommissionByPeriod(@PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(defaultValue = "false") boolean saveHistory) {

        CollectorCommissionDTO dto = service.getCommissionByPeriod(id, startDate, endDate, saveHistory);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/{collectorId}/sales")
    public ResponseEntity<Map<String, List<SaleCollectorDTO>>> getSalesByCollector(@PathVariable Long collectorId) {
        Map<String, List<SaleCollectorDTO>> salesByCity = service.findSalesByCollectorId(collectorId);
        return ResponseEntity.ok(salesByCity);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<CollectorIdUserDTO> getCollectorByUserId(@PathVariable Long userId) {
        CollectorIdUserDTO dto = service.getCollectorByUserId(userId);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @PutMapping("/{collectorId}/installment/{installmentId}/collect")
    public ResponseEntity<CollectionAttemptDTO> collectInstallment(@PathVariable Long collectorId, @PathVariable Long installmentId, @RequestBody CollectionAttemptDTO dto) {
        System.out.println("agora chamei o collectInstallment");
        CollectionAttemptDTO attempt = collectionAttemptService.recordAttempt(
                collectorId,
                installmentId,
                dto
        );

        return ResponseEntity.ok(attempt);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/installment/{id}/pix")
    public ResponseEntity<byte[]> getPixQrCode(@PathVariable Long id) {
        try {
            String brCode = pixService.generateBrCodeForInstallment(id);
            byte[] qrImage = pixService.generateQrCodeImage(brCode, 300, 300);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.noCache())
                    .header("Content-Disposition", "inline; filename=\"qrcode.png\"")
                    .body(qrImage);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/installment/{id}/pix/debug")
    public ResponseEntity<String> getPixCodeDebug(@PathVariable Long id) {
        try {
            String brCode = pixService.generateBrCodeForInstallment(id);
            return ResponseEntity.ok(brCode);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/collector/{collectorId}/sale/{saleId}/paid-attempts")
    public ResponseEntity<List<CollectionAttemptMapsDTO>> getPaidAttemptsByCollectorAndSale(@PathVariable Long collectorId, @PathVariable Long saleId) {

        List<CollectionAttemptMapsDTO> list = collectionAttemptService.findPaidAttemptsByCollectorAndSale(collectorId, saleId);

        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/collector/sale/{saleId}/location-sale")
    public ResponseEntity<List<LocationSaleDTO>> getLocationBySaleId(@PathVariable Long saleId) {

        List<LocationSaleDTO> list = approvalLocationService.getLocationSale(saleId);

        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/check-location/{installmentId}")
    public ResponseEntity<Map<String, Object>> checkLocation(@PathVariable Long installmentId) {

        boolean withinRadius = service.isAttemptWithinApprovalLocation(installmentId);

        Map<String, Object> response = new HashMap<>();
        response.put("installmentId", installmentId);
        response.put("withinRadius", withinRadius);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @GetMapping("/commission-history")
    public Page<CommissionHistoryDTO> getCommissionHistory(
            @RequestParam(required = false) Long collectorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return commissionHistoryService.getByCollector( collectorId, page, size);
    }



}
