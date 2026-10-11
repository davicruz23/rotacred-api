package tads.ufrn.apigestao.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tads.ufrn.apigestao.domain.dto.city.CityNameDTO;
import tads.ufrn.apigestao.service.AddressService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/city", "/api/city"})
public class CityController {

    private final AddressService addressService;

    @GetMapping("/name/all")
    public ResponseEntity<List<CityNameDTO>> findAllNames() {
        return ResponseEntity.ok(addressService.getDistinctCityNames());
    }
}
