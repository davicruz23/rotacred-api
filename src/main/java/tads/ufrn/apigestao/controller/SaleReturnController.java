package tads.ufrn.apigestao.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tads.ufrn.apigestao.domain.dto.returnSale.ReturnSaleRequest;
import tads.ufrn.apigestao.domain.dto.returnSale.SaleReturnDTO;
import tads.ufrn.apigestao.domain.dto.returnSale.SaleReturnData;
import tads.ufrn.apigestao.domain.dto.returnSale.UpdateSaleStatusRequest;
import tads.ufrn.apigestao.service.SaleReturnService;

import java.util.List;

@RestController
@RequestMapping("/api/sale-return")
@RequiredArgsConstructor
public class SaleReturnController {

    private final SaleReturnService service;

//    @PostMapping("/{saleId}")
//    @ResponseStatus(HttpStatus.CREATED)
//    public List<SaleReturnDTO> returnSale(
//            @PathVariable Long saleId,
//            @Valid @RequestBody ReturnSaleRequest request
//    ) {
//        return service.returnSale(saleId, request);
//    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @PostMapping("/sales/{saleId}/returns")
    public ResponseEntity<List<SaleReturnDTO>> returnSale(@PathVariable Long saleId, @Valid @RequestBody ReturnSaleRequest request) {
        List<SaleReturnDTO> response = service.returnSale(saleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @GetMapping("/sale-returns")
    public ResponseEntity<List<SaleReturnData>> getReturns(@RequestParam(required = false) Integer status) {
        return ResponseEntity.ok(service.findReturns(status));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @GetMapping("/status/end")
    public ResponseEntity<List<SaleReturnData>> getReturnsGuarantee(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String cpf
    ) {
        return ResponseEntity.ok(service.findReturnGuarantee(id, name, cpf));
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @PatchMapping("/{saleReturnId}/update/status")
    public ResponseEntity<Void> updateAfterDefect(@PathVariable Long saleReturnId, @RequestBody UpdateSaleStatusRequest request) {
        service.updateAfterDefect(saleReturnId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}
