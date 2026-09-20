package tads.ufrn.apigestao.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tads.ufrn.apigestao.controller.mapper.InspectorMapper;
import tads.ufrn.apigestao.controller.mapper.PreSaleMapper;
import tads.ufrn.apigestao.controller.mapper.SaleMapper;
import tads.ufrn.apigestao.domain.Inspector;
import tads.ufrn.apigestao.domain.PreSale;
import tads.ufrn.apigestao.domain.Sale;
import tads.ufrn.apigestao.domain.dto.inspector.InspectorDTO;
import tads.ufrn.apigestao.domain.dto.inspector.InspectorHistoryPreSaleDTO;
import tads.ufrn.apigestao.domain.dto.inspector.InspectorIdUserDTO;
import tads.ufrn.apigestao.domain.dto.inspector.UpsertInspectorDTO;
import tads.ufrn.apigestao.domain.dto.preSale.PreSaleDTO;
import tads.ufrn.apigestao.domain.dto.sale.ApprovePreSaleDTO;
import tads.ufrn.apigestao.domain.dto.sale.SaleDTO;
import tads.ufrn.apigestao.domain.dto.seller.SellerIdUserDTO;
import tads.ufrn.apigestao.enums.PreSaleStatus;
import tads.ufrn.apigestao.service.InspectorService;
import tads.ufrn.apigestao.service.PreSaleService;
import tads.ufrn.apigestao.service.SaleService;

import java.net.URI;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/inspector")
public class InspectorController {

    private final InspectorService service;
    private final PreSaleService preSaleService;
    private final SaleService saleService;

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @GetMapping("/all")
    public ResponseEntity<List<InspectorDTO>> findAll(){
        return ResponseEntity.ok(service.findAllDTO());
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @GetMapping("/{id}")
    public ResponseEntity<InspectorDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findByIdDTO(id));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @PostMapping
    public ResponseEntity<Void> store(@RequestBody UpsertInspectorDTO model){
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(service.store(model).getId())
                .toUri();
        return ResponseEntity.created(uri).build();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @DeleteMapping("{id}/delete")
    public ResponseEntity<Void> deleteById(@PathVariable Long id){
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @GetMapping("/{inspectorId}/pre-sales/pending")
    public ResponseEntity<List<PreSaleDTO>> listPending(@PathVariable Long inspectorId) {

        List<PreSaleDTO> list = preSaleService
                .listAllPreSales(inspectorId, PreSaleStatus.PENDENTE)
                .stream()
                .map(PreSaleMapper::mapper)
                .toList();

        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @PostMapping("/pre-sales/{preSaleId}/approve")
    public ResponseEntity<SaleDTO> approve(
            @PathVariable Long preSaleId,
            @RequestBody @Valid ApprovePreSaleDTO dto) {

        Inspector inspector = service.findEntityById(dto.getInspectorId());

        Sale sale = saleService.approvePreSale(
                preSaleId,
                inspector,
                dto.getPaymentMethod(),
                dto.getInstallments(),
                dto.getCashPaid(),
                dto.getLatitude(),
                dto.getLongitude(),
                null
        );

        return ResponseEntity.ok(SaleMapper.toDTO(sale));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @PostMapping("/pre-sales/{preSaleId}/reject")
    public ResponseEntity<PreSaleDTO> reject(@PathVariable Long preSaleId) {
        PreSale preSale = preSaleService.rejectPreSale(preSaleId);
        return ResponseEntity.ok(PreSaleMapper.mapper(preSale));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<InspectorIdUserDTO> getInspectorByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getInspectorByUserId(userId));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @GetMapping("/{inspectorId}/pre-sales-history")
    public ResponseEntity<List<InspectorHistoryPreSaleDTO>> getPreSalesByInspector(@PathVariable Long inspectorId) {
        List<InspectorHistoryPreSaleDTO> list = preSaleService.findPreSalesByInspector(inspectorId);
        return ResponseEntity.ok(list);
    }
}
