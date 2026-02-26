package tads.ufrn.apigestao.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tads.ufrn.apigestao.controller.mapper.SaleMapper;
import tads.ufrn.apigestao.domain.dto.sale.*;
import tads.ufrn.apigestao.service.SaleService;

import java.net.URI;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/sale")
public class SaleController {

    private SaleService service;

//    @PreAuthorize("hasAnyRole('SUPERADMIN','FISCAL')")
//    @GetMapping("/all")
//    public ResponseEntity<List<SaleDTO>> findAll(){
//        return ResponseEntity.ok().body(service.findAll().stream().map(SaleMapper::mapper).toList());
//    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @GetMapping("/sales/{id}")
    public ResponseEntity<SaleDetailDTO> findSaleDetail(@PathVariable Long id) {
        return ResponseEntity.ok(service.findSaleDetail(id));

    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','FUNCIONARIO')")
    @GetMapping("/sales/search")
    public ResponseEntity<List<SaleSearchDTO>> searchSales(@RequestParam(required = false) String name, @RequestParam(required = false) Long id, @RequestParam(required = false) String cpf, @RequestParam(required = false) String city) {
        return ResponseEntity.ok(service.searchSales(name, id, cpf, city));
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
}
