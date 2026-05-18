package tads.ufrn.apigestao.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tads.ufrn.apigestao.controller.mapper.SaleMapper;
import tads.ufrn.apigestao.domain.Inspector;
import tads.ufrn.apigestao.domain.Sale;
import tads.ufrn.apigestao.domain.dto.commissionHistory.CommissionHistoryDTO;
import tads.ufrn.apigestao.domain.dto.sale.*;
import tads.ufrn.apigestao.enums.SaleStatus;
import tads.ufrn.apigestao.service.CommissionHistoryService;
import tads.ufrn.apigestao.service.InspectorService;
import tads.ufrn.apigestao.service.SaleService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/sale")
public class SaleController {

    private SaleService service;
    private InspectorService inspectorService;

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @GetMapping("/all")
    public ResponseEntity<Page<SalesListDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate
    ) {
        return ResponseEntity.ok(
                service.findAll(page, size, clientName, cpf, status, saleDate)
        );
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @PostMapping("/store-and-approve")
    public ResponseEntity<SaleDTO> storeAndApprove(@RequestBody StoreAndApprovePreSaleDTO dto) {

        Inspector inspector = inspectorService.findEntityById(1L);

        Sale sale = service.storeAndApprovePreSale(
                dto.getPreSale(),
                inspector,
                dto.getPaymentMethod(),
                dto.getInstallments(),
                dto.getCashPaid(),
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(SaleMapper.mapper(sale));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @GetMapping("/sales/{id}")
    public ResponseEntity<SaleDetailDTO> findSaleDetail(@PathVariable Long id) {
        return ResponseEntity.ok(service.findSaleDetail(id));

    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @GetMapping("/sales/search")
    public ResponseEntity<Page<SaleSearchDTO>> searchSales(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String city,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.searchSales(name, id, cpf, city, pageable));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @GetMapping("/{id}")
    public ResponseEntity<SaleDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok().body(SaleMapper.mapper(service.findById(id)));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @PostMapping
    public ResponseEntity<UpsertSaleDTO> store(@RequestBody UpsertSaleDTO model){
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}").buildAndExpand(service.store(model).getId()).toUri();
        return ResponseEntity.created(uri).build();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @DeleteMapping("{id}/delete")
    public ResponseEntity<SaleDTO> deleteById(@PathVariable Long id){
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
    @GetMapping("/sales/by-city")
    public List<SalesByCityDTO> getSalesByCity() {
        return service.getSalesGroupedByCity();
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @PatchMapping("/sales/{saleId}/open-installments/due-dates")
    public ResponseEntity<Void> changeOpenInstallmentsDueDate(
            @PathVariable Long saleId,
            @RequestBody ChangeOpenInstallmentsDueDateDTO request
    ) {
        service.changeOpenInstallmentsDueDate(saleId, request.getFirstDueDate());
        return ResponseEntity.noContent().build();
    }

}
