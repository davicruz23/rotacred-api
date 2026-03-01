package tads.ufrn.apigestao.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tads.ufrn.apigestao.controller.mapper.CollectorMapper;
import tads.ufrn.apigestao.controller.mapper.SellerMapper;
import tads.ufrn.apigestao.domain.dto.charging.UpsertChargingDTO;
import tads.ufrn.apigestao.domain.dto.collector.CollectorCommissionDTO;
import tads.ufrn.apigestao.domain.dto.collector.CollectorDTO;
import tads.ufrn.apigestao.domain.dto.commissionHistory.CommissionHistoryDTO;
import tads.ufrn.apigestao.domain.dto.seller.*;
import tads.ufrn.apigestao.service.CommissionHistoryService;
import tads.ufrn.apigestao.service.PreSaleService;
import tads.ufrn.apigestao.service.SellerService;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerService service;
    private final PreSaleService preSaleService;
    private final CommissionHistoryService commissionHistoryService;

    @PreAuthorize("hasAnyRole('SUPERADMIN','VENDEDOR')")
    @GetMapping("/all")
    public ResponseEntity<List<SellerDTO>> findAll() {
        return ResponseEntity.ok().body(service.findAll().stream().map(SellerMapper::mapper).toList());
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("/name/all")
    public ResponseEntity<List<SellerDetailsDTO>> findAllByName() {
        return ResponseEntity.ok(service.findAll().stream().map(SellerMapper::mapperDetails).toList());
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','COBRADOR')")
    @GetMapping("{id}")
    public ResponseEntity<SellerDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok().body(SellerMapper.mapper(service.findById(id)));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','VENDEDOR')")
    @PostMapping
    public ResponseEntity<UpsertSellerDTO> store(@RequestBody UpsertSellerDTO model){
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}").buildAndExpand(service.store(model).getId()).toUri();
        return ResponseEntity.created(uri).build();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','VENDEDOR')")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<SellerIdUserDTO> getSellerByUserId(@PathVariable Long userId) {
        SellerIdUserDTO dto = service.getSellerByUserId(userId);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','VENDEDOR')")
    @GetMapping("/{id}/commission")
    public ResponseEntity<SellerCommissionDTO> getCommissionByPeriod(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean saveHistory) {

        SellerCommissionDTO dto = preSaleService.getCommissionByPeriod(id, startDate, endDate, saveHistory);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @GetMapping("/commission-history")
    public Page<CommissionHistoryDTO> getCommissionHistory(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return commissionHistoryService.getBySeller( sellerId, page, size);
    }
}
