package tads.ufrn.apigestao.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.domain.dto.city.CityNameDTO;
import tads.ufrn.apigestao.repository.AddressRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository repository;

    public List<String> getDistinctCities() {
        return repository.findAllDistinctCities();
    }

    public List<CityNameDTO> getDistinctCityNames() {
        return repository.findAllDistinctCityNames()
                .stream()
                .map(CityNameDTO::new)
                .toList();
    }

}
