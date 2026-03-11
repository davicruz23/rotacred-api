package tads.ufrn.apigestao.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tads.ufrn.apigestao.controller.mapper.InspectorMapper;
import tads.ufrn.apigestao.controller.mapper.PreSaleMapper;
import tads.ufrn.apigestao.domain.*;
import tads.ufrn.apigestao.domain.dto.inspector.InspectorHistoryPreSaleDTO;
import tads.ufrn.apigestao.domain.dto.preSale.PreSaleDTO;
import tads.ufrn.apigestao.domain.dto.preSale.UpsertPreSaleDTO;
import tads.ufrn.apigestao.domain.dto.preSaleItem.UpsertPreSaleItemDTO;
import tads.ufrn.apigestao.domain.dto.seller.SellerCommissionDTO;
import tads.ufrn.apigestao.enums.PreSaleStatus;
import tads.ufrn.apigestao.exception.BusinessException;
import tads.ufrn.apigestao.exception.ResourceNotFoundException;
import tads.ufrn.apigestao.repository.CommissionHistoryRepository;
import tads.ufrn.apigestao.repository.PreSaleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PreSaleService {

    private final PreSaleRepository repository;
    private final ClientService clientService;
    private final SellerService sellerService;
    private final ChargingService changingService;
    private final InspectorService inspectorService;
    private final CommissionHistoryRepository commissionHistoryRepository;

    public List<PreSale> findAll(){
        return repository.findAll();
    }

    public PreSale findById(Long id) {
        Optional<PreSale> preSale = repository.findById(id);
        return preSale.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
    }

    @Transactional
    public PreSaleDTO store(UpsertPreSaleDTO dto) {
        System.out.println("Charging ID recebido: " + dto.getChargingId());

        Optional<PreSale> existing = repository.findByUuidPreSale(dto.getUuidPreSale());

        if(existing.isPresent()){
            return PreSaleMapper.mapper(existing.get());
        }

        Client client = clientService.store(dto.getClient());
        Seller seller = sellerService.findById(dto.getSellerId());

        if (seller == null) {
            throw new BusinessException("Vendedor não encontrado: " + dto.getSellerId());
        }

        Inspector inspector = inspectorService.findEntityById(1L);

        PreSale preSale = new PreSale();
        preSale.setPreSaleDate(dto.getPreSaleDate());
        preSale.setSeller(seller);
        preSale.setClient(client);
        preSale.setInspector(inspector);
        preSale.setStatus(PreSaleStatus.PENDENTE);
        preSale.setItems(new ArrayList<>());
        preSale.setUuidPreSale(dto.getUuidPreSale());

        Charging charging = changingService.findEntityById(dto.getChargingId());

        for (UpsertPreSaleItemDTO prodDTO : dto.getProducts()) {

            ChargingItem ci = charging.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(prodDTO.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Produto não encontrado no carregamento: " + prodDTO.getProductId()));

            if (ci.getQuantity() < prodDTO.getQuantity()) {
                throw new BusinessException("Quantidade insuficiente no carregamento para o produto: " + ci.getProduct().getName());
            }

            ci.setQuantity(ci.getQuantity() - prodDTO.getQuantity());

            PreSaleItem preSaleItem = new PreSaleItem();
            preSaleItem.setPreSale(preSale);
            preSaleItem.setProduct(ci.getProduct());
            preSaleItem.setChargingItem(ci);
            preSaleItem.setQuantity(prodDTO.getQuantity());

            preSale.getItems().add(preSaleItem);
        }

        BigDecimal totalPreSale = preSale.getItems().stream()
                .map(item ->
                        item.getProduct().getValue()
                                .multiply(BigDecimal.valueOf(item.getQuantity()))
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        preSale.setTotalPreSale(totalPreSale);

        PreSale saved = repository.save(preSale);

        return PreSaleMapper.mapper(saved);
    }

    public void deleteById(Long id){
        PreSale client = repository.findById(id)
                .orElseThrow(()-> new BusinessException("Produto não encontrado!"));
        repository.save(client);

    }

    public PreSale approvePreSale(Long preSaleId, Inspector inspector) {
        PreSale preSale = findById(preSaleId);
        preSale.setStatus(PreSaleStatus.APROVADA);
        preSale.setInspector(inspector);
        return repository.save(preSale);
    }

    @Transactional
    public PreSale rejectPreSale(Long preSaleId) {
        PreSale preSale = findById(preSaleId);

        preSale.setStatus(PreSaleStatus.RECUSADA);
        for (PreSaleItem item : preSale.getItems()) {
            int quantity = item.getQuantity();

            ChargingItem chargingItem = item.getChargingItem();
            if (chargingItem != null) {
                chargingItem.setQuantity(chargingItem.getQuantity() + quantity);
            }
        }

        repository.save(preSale);

        return preSale;
    }

    public List<PreSale> listAllPreSales(Long inspectorId, PreSaleStatus status) {
        return repository.findByInspectorIdAndStatus(inspectorId, status);
    }

    public List<PreSale> getPreSalesBySeller(Long sellerId) {
        return repository.findBySellerId(sellerId);
    }

    @Transactional
    public SellerCommissionDTO getCommissionByPeriod(
            Long sellerId,
            LocalDate startDate,
            LocalDate endDate,
            boolean saveHistory
    ) {

        LocalDateTime now = LocalDateTime.now();

        if (endDate.isBefore(startDate)) {
            throw new BusinessException("A data final não pode ser anterior à data inicial.");
        }

        Seller seller = sellerService.findById(sellerId);

        List<PreSale> preSales = getPreSalesBySeller(sellerId).stream()
                .filter(preSale -> preSale.getStatus() == PreSaleStatus.APROVADA)
                .filter(preSale -> preSale.getPreSaleDate() != null
                        && !preSale.getPreSaleDate().isBefore(startDate)
                        && !preSale.getPreSaleDate().isAfter(endDate))
                .toList();

        BigDecimal totalCommission = preSales.stream()
                .map(preSale -> {

                    BigDecimal total = preSale.getTotalPreSale() != null
                            ? preSale.getTotalPreSale()
                            : BigDecimal.ZERO;

                    BigDecimal rate = total.compareTo(BigDecimal.valueOf(1000)) <= 0
                            ? new BigDecimal("0.09")
                            : new BigDecimal("0.045");

                    return total.multiply(rate);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (saveHistory) {
            CommissionHistory history = new CommissionHistory();
            history.setSeller(seller);
            history.setGeneratedAt(now);
            history.setStartDate(startDate);
            history.setEndDate(endDate);
            history.setAmount(totalCommission);
            commissionHistoryRepository.save(history);
        }

        return new SellerCommissionDTO(
                seller.getId(),
                seller.getUser().getName(),
                startDate,
                endDate,
                totalCommission
        );
    }


    @Transactional
    public List<InspectorHistoryPreSaleDTO> findPreSalesByInspector(Long inspectorId) {
        List<PreSale> preSales = repository.findAllByInspectorId(inspectorId);
        return preSales.stream()
                .map(InspectorMapper::mapperHistory)
                .toList();
    }

}
