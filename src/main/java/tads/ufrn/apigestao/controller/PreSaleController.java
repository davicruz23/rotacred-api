package tads.ufrn.apigestao.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tads.ufrn.apigestao.controller.mapper.PreSaleMapper;
import tads.ufrn.apigestao.domain.PreSale;
import tads.ufrn.apigestao.domain.dto.client.ClientDTO;
import tads.ufrn.apigestao.domain.dto.client.UpsertClientDTO;
import tads.ufrn.apigestao.domain.dto.preSale.PreSaleDTO;
import tads.ufrn.apigestao.domain.dto.preSale.UpsertPreSaleDTO;
import tads.ufrn.apigestao.service.PreSaleService;

import java.net.URI;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/preSale")
public class PreSaleController {

    private PreSaleService service;

    @PreAuthorize("hasAnyRole('SUPERADMIN','VENDEDOR','FISCAL')")
    @GetMapping("/all")
    public ResponseEntity<List<PreSaleDTO>> findAll(){
        return ResponseEntity.ok().body(service.findAll().stream().map(PreSaleMapper::mapper).toList());
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN','VENDEDOR','FISCAL')")
    @GetMapping("/{id}")
    public ResponseEntity<PreSaleDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok().body(PreSaleMapper.mapper(service.findById(id)));
    }

    @PostMapping
    public ResponseEntity<?> store(@RequestBody UpsertPreSaleDTO model) {

        System.out.println("recebi: "+model );

        try {

            PreSaleDTO dto = service.store(model);

            URI uri = ServletUriComponentsBuilder
                    .fromCurrentRequest().path("/{id}")
                    .buildAndExpand(dto.getId())
                    .toUri();

            return ResponseEntity.created(uri).body(dto);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @DeleteMapping("{id}/delete")
    public ResponseEntity<PreSaleDTO> deleteById(@PathVariable Long id){
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
