package tads.ufrn.apigestao.service;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.controller.mapper.ChargingMapper;
import tads.ufrn.apigestao.domain.Charging;
import tads.ufrn.apigestao.domain.ChargingItem;
import tads.ufrn.apigestao.domain.Product;
import tads.ufrn.apigestao.domain.User;
import tads.ufrn.apigestao.domain.dto.charging.AddChargingItemDTO;
import tads.ufrn.apigestao.domain.dto.charging.ChargingDTO;
import tads.ufrn.apigestao.domain.dto.charging.UpdateChargingItemDTO;
import tads.ufrn.apigestao.domain.dto.charging.UpsertChargingDTO;
import tads.ufrn.apigestao.domain.dto.chargingItem.UpsertChargingItemDTO;
import tads.ufrn.apigestao.exception.BusinessException;
import tads.ufrn.apigestao.exception.ResourceNotFoundException;
import tads.ufrn.apigestao.repository.ChargingRepository;
import tads.ufrn.apigestao.repository.ProductRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChargingService {

    private final ChargingRepository repository;
    private final UserService userService;
    private final ProductService productService;
    private final ChargingItemService chargingItemService;
    private final ProductRepository productRepository;

    private static final Logger log = LoggerFactory.getLogger(ChargingService.class);

    @Transactional(readOnly = true)
    public Page<ChargingDTO> findAll(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        String searchTerm = (name == null || name.isBlank()) ? null : name.trim();

        Page<Long> idsPage = repository.findIdsByFilter(searchTerm, pageable);

        if (idsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Passa o searchTerm também para a segunda query
        List<Charging> chargings = repository.findAllByIdInWithItems(
                idsPage.getContent(),
                searchTerm
        );

        Map<Long, Charging> map = chargings.stream()
                .collect(Collectors.toMap(Charging::getId, Function.identity()));

        List<ChargingDTO> dtoList = idsPage.getContent().stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .map(ChargingMapper::mapper)
                .toList();

        return new PageImpl<>(dtoList, pageable, idsPage.getTotalElements());
    }


    @Transactional(readOnly = true)
    public List<ChargingDTO> findCurrent(String nameProduct, String brand) {

        String search = (nameProduct != null && !nameProduct.isEmpty())
                ? nameProduct
                : brand;

        return repository.findAllCurrentWithItems(search)
                .stream()
                .map(ChargingMapper::mapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChargingDTO findById(Long id) {
        Charging charging = repository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carregamento não encontrado!"));

        return ChargingMapper.mapper(charging);
    }

    @Transactional(readOnly = true)
    public Charging findEntityById(Long id) {
        return repository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carregamento não encontrado!"));
    }

    @Transactional
    public Charging store(UpsertChargingDTO chargingDTO) {

        User userCharging = userService.findUserById(1L);

        Charging charging = repository.findFirstBy()
                .orElseGet(Charging::new);

        for (UpsertChargingItemDTO itemDTO : chargingDTO.getItems()) {

            Product product = productService.findById(itemDTO.getProductId());
            int newQuantity = itemDTO.getQuantity();

            ChargingItem item = charging.getItems()
                    .stream()
                    .filter(i -> i.getProduct().getId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            if (item != null) {

                int diff = newQuantity - item.getQuantity();

                if (diff > 0 && product.getAmount() < diff) {
                    throw new RuntimeException(
                            "Estoque insuficiente para o produto: " + product.getId()
                    );
                }

                product.setAmount(product.getAmount() - diff);
                item.setQuantity(newQuantity);

            } else {

                if (product.getAmount() < newQuantity) {
                    throw new RuntimeException(
                            "Estoque insuficiente para o produto: " + product.getId()
                    );
                }

                product.setAmount(product.getAmount() - newQuantity);
                charging.addItem(product, newQuantity);
                charging.setDescription(chargingDTO.getDescription());
                charging.setDate(LocalDate.now());
                charging.setUser(userCharging);
                charging.setCreatedAt(OffsetDateTime.now());
                charging.setDescription("MERCADORIAS");
            }
        }

        Charging savedCharging = repository.save(charging);
        productService.saveAllFromChargingItems(savedCharging.getItems());

        return savedCharging;
    }

    @Transactional
    public ChargingDTO addProductsToCharging(List<AddChargingItemDTO> itemsToAdd) {

        Charging charging = repository.findFirstBy()
                .orElseThrow(() -> new BusinessException("Carregamento não encontrado"));

        for (AddChargingItemDTO dto : itemsToAdd) {

            if (dto.quantity() <= 0) {
                throw new BusinessException("Quantidade inválida para o produto " + dto.productId());
            }

            Product product = productService.findById(dto.productId());

            if (product.getAmount() < dto.quantity()) {
                throw new BusinessException(
                        "Estoque insuficiente para o produto: " + dto.productId()
                );
            }

            ChargingItem item = charging.getItems()
                    .stream()
                    .filter(i -> i.getProduct().getId().equals(dto.productId()))
                    .findFirst()
                    .orElse(null);

            product.setAmount(product.getAmount() - dto.quantity());
            productRepository.save(product);

            if (item != null) {
                item.setQuantity(item.getQuantity() + dto.quantity());
            } else {
                charging.addItem(product, dto.quantity());

            }
        }

        repository.save(charging);

        return ChargingMapper.mapper(charging);
    }

    @Transactional
    public void deleteById(Long id) {
        Charging charging = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carregamento não encontrado!"));

        for (ChargingItem item : charging.getItems()) {
            productService.returnStock(item.getProduct().getId(), item.getQuantity());
        }

        chargingItemService.markItemsAsDeletedByChargingId(id);

        charging.delete();
        repository.save(charging);
    }

}
