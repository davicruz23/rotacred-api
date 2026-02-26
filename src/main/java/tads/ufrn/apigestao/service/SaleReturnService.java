package tads.ufrn.apigestao.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tads.ufrn.apigestao.controller.mapper.SaleReturnMapper;
import tads.ufrn.apigestao.domain.*;
import tads.ufrn.apigestao.domain.dto.returnSale.*;
import tads.ufrn.apigestao.enums.SaleStatus;
import tads.ufrn.apigestao.exception.BusinessException;
import tads.ufrn.apigestao.repository.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleReturnService {

    private final SaleService saleService;
    private final SaleReturnRepository saleReturnRepository;
    private final InstallmentRepository installmentRepository;
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    @Transactional
    public List<SaleReturnDTO> returnSale(Long saleId, ReturnSaleRequest request) {

        Sale sale = saleService.findById(saleId);
        SaleStatus newStatus = SaleStatus.fromValue(request.getStatus());

        validateReturnRequest(sale);

        OffsetDateTime now = OffsetDateTime.now();

        updateSaleStatusInternal(sale, newStatus);

        handleFinancialImpactByStatus(sale, saleId, request, newStatus);

        List<SaleReturn> saleReturns = createSaleReturns(sale, request, newStatus, now);

        saleReturnRepository.saveAll(saleReturns);

        return mapToDTO(saleReturns);
    }

    private void validateReturnRequest(Sale sale) {

        if (isSaleFullyReturned(sale)) {
            throw new BusinessException("Essa venda já foi totalmente devolvida.");
        }
    }

    private void updateSaleStatusInternal(Sale sale, SaleStatus newStatus) {
        sale.setStatus(newStatus);
        saleRepository.save(sale);
    }

    private void handleFinancialImpactByStatus(Sale sale, Long saleId, ReturnSaleRequest request, SaleStatus newStatus) {

        switch (newStatus) {

            case DESISTENCIA ->
                    handleDesistencia(sale, saleId, request);

            case REAVIDO ->
                    handleReavido(sale, saleId);

            case DEFEITO_PRODUTO -> {
                // Futuramente você coloca regra aqui
                handleDefeitoProduto(sale, request);
            }

            case ATIVO -> {
                // não altera parcelas
            }

            default ->
                    throw new BusinessException("Status inválido para devolução: " + newStatus);
        }
    }

    private void handleDesistencia(Sale sale, Long saleId, ReturnSaleRequest request) {

        List<Installment> futureInstallments =
                installmentRepository.findAllBySaleIdAndPaidFalseOrderByDueDateDesc(saleId);

        if (futureInstallments.isEmpty()) {
            return;
        }

        BigDecimal discount = calculateDiscount(sale, request);

        applyDiscountOnInstallments(futureInstallments, discount);

        installmentRepository.saveAll(futureInstallments);
    }

    private BigDecimal calculateDiscount(Sale sale, ReturnSaleRequest request) {

        Map<Long, PreSaleItem> saleItemsMap =
                sale.getPreSale().getItems().stream()
                        .collect(Collectors.toMap(
                                i -> i.getProduct().getId(),
                                Function.identity()
                        ));

        BigDecimal discount = BigDecimal.ZERO;

        for (ReturnSaleItemDTO dto : request.getItems()) {

            PreSaleItem item = saleItemsMap.get(dto.getProductId());

            if (item == null) {
                throw new BusinessException(
                        "Produto " + dto.getProductId() + " não pertence à venda."
                );
            }

            int alreadyReturned = saleReturnRepository
                    .sumReturnedQuantity(sale.getId(), dto.getProductId());

            int availableToReturn = item.getQuantity() - alreadyReturned;

            if (dto.getQuantityReturned() <= 0 ||
                    dto.getQuantityReturned() > availableToReturn) {

                throw new BusinessException(
                        "Quantidade inválida. Disponível para devolução: "
                                + availableToReturn
                );
            }

            BigDecimal unitPrice = item.getProduct().getValue();

            discount = discount.add(
                    unitPrice.multiply(BigDecimal.valueOf(dto.getQuantityReturned()))
            );
        }

        return discount;
    }

    private void applyDiscountOnInstallments(List<Installment> installments, BigDecimal discount) {

        for (Installment inst : installments) {

            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal instAmount = inst.getAmount();

            if (instAmount.compareTo(discount) <= 0) {
                inst.setAmount(BigDecimal.ZERO);
                inst.setPaidAmount(BigDecimal.ZERO);
                inst.setPaid(true);
                discount = discount.subtract(instAmount);
            } else {
                inst.setAmount(instAmount.subtract(discount));
                inst.setPaidAmount(BigDecimal.ZERO);
                discount = BigDecimal.ZERO;
            }
        }
    }

    private void handleReavido(Sale sale, Long saleId) {

        List<Installment> futureInstallments =
                installmentRepository.findAllBySaleIdAndPaidFalseOrderByDueDateAsc(saleId);

        for (Installment inst : futureInstallments) {
            inst.setAmount(BigDecimal.ZERO);
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setPaid(true);
        }

        installmentRepository.saveAll(futureInstallments);
    }

    private void handleDanificado(Sale sale, Long saleId, ReturnSaleRequest request) {

        handleDesistencia(sale, saleId, request);
    }

    private void handleDefeitoProduto(Sale sale, ReturnSaleRequest request) {

        // Aqui você poderá:
        // - gerar ordem de garantia
        // - gerar movimentação de estoque
        // - criar workflow técnico
        // - registrar histórico

    }

    private List<SaleReturn> createSaleReturns(Sale sale, ReturnSaleRequest request, SaleStatus status, OffsetDateTime now) {

        List<SaleReturn> saleReturns = new ArrayList<>();

        for (ReturnSaleItemDTO dto : request.getItems()) {

            SaleReturn.SaleReturnBuilder builder = SaleReturn.builder()
                    .sale(sale)
                    .returnDate(now)
                    .productId(dto.getProductId())
                    .quantityReturned(dto.getQuantityReturned())
                    .saleStatus(status)
                    .description(request.getDescription());


            saleReturns.add(builder.build());
        }

        return saleReturns;
    }

    private List<SaleReturnDTO> mapToDTO(List<SaleReturn> saleReturns) {

        return saleReturns.stream()
                .map(sr -> new SaleReturnDTO(
                        sr.getId(),
                        sr.getSale().getId(),
                        sr.getProductId(),
                        sr.getReturnDate(),
                        sr.getSaleStatus()
                ))
                .toList();
    }

    @Transactional
    public void updateSaleStatus(Long saleId, SaleStatus newStatus) {

        Sale sale = saleService.findById(saleId);

        validateStatusTransition(sale.getStatus(), newStatus);

        sale.setStatus(newStatus);
        saleRepository.save(sale);
    }

    private void validateStatusTransition(SaleStatus current, SaleStatus target) {

        if (current == SaleStatus.DEVOLVIDO_CLIENTE
                && target == SaleStatus.ATIVO) {

            throw new BusinessException(
                    "Não é permitido reativar uma venda devolvida."
            );
        }

        if (current == SaleStatus.REAVIDO
                && target == SaleStatus.DEFEITO_PRODUTO) {

            throw new BusinessException(
                    "Venda reavida não pode virar defeito."
            );
        }
    }

    @Transactional
    public void updateAfterDefect(Long saleReturnId, int status) {

        SaleReturn saleReturn = saleReturnRepository.findById(saleReturnId)
                .orElseThrow(() -> new BusinessException("SaleReturn não encontrado."));

        if (saleReturn.getSaleStatus() != SaleStatus.DEFEITO_PRODUTO) {
            throw new BusinessException("SaleReturn não está em análise de defeito.");
        }

        SaleStatus newStatus = SaleStatus.fromValue(status);

        saleReturn.setSaleStatus(newStatus);
        saleReturnRepository.save(saleReturn);

        Sale sale = saleReturn.getSale();
        sale.setStatus(newStatus);
        saleRepository.save(sale);

        if (newStatus == SaleStatus.DANIFICADO) {
            handleDesistencia(
                    sale,
                    sale.getId(),
                    buildRequestFromSaleReturn(saleReturn)
            );
        }
    }

    private ReturnSaleRequest buildRequestFromSaleReturn(SaleReturn saleReturn) {

        ReturnSaleItemDTO item = new ReturnSaleItemDTO();
        item.setProductId(saleReturn.getProductId());
        item.setQuantityReturned(saleReturn.getQuantityReturned());

        ReturnSaleRequest request = new ReturnSaleRequest();
        request.setItems(List.of(item));

        return request;
    }

    public List<SaleReturnData> findReturns(Integer statusValue) {

        SaleStatus status = statusValue != null
                ? SaleStatus.fromValue(statusValue)
                : null;

        List<SaleReturn> returns = saleReturnRepository.findAllWithSale(status);

        Set<Long> productIds = returns.stream()
                .map(SaleReturn::getProductId)
                .collect(Collectors.toSet());

        Map<Long, String> productNameMap =
                productRepository.findAllByIdIn(productIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Product::getId,
                                Product::getName
                        ));

        return returns.stream()
                .map(sr -> SaleReturnMapper.mapper(sr, productNameMap))
                .toList();
    }

    public List<SaleReturnData> findReturnGuarantee(Long id, String name, String cpf) {

        List<SaleReturn> returns =
                saleReturnRepository.findAllDefectiveProductReturns(id, name, cpf);

        Set<Long> productIds = returns.stream()
                .map(SaleReturn::getProductId)
                .collect(Collectors.toSet());

        Map<Long, String> productNameMap =
                productRepository.findAllByIdIn(productIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Product::getId,
                                Product::getName
                        ));

        return returns.stream()
                .map(sr -> SaleReturnMapper.mapper(sr, productNameMap))
                .toList();
    }

    private boolean isSaleFullyReturned(Sale sale) {

        for (PreSaleItem item : sale.getPreSale().getItems()) {

            int alreadyReturned = saleReturnRepository
                    .sumReturnedQuantity(sale.getId(), item.getProduct().getId());

            if (alreadyReturned < item.getQuantity()) {
                return false;
            }
        }

        return true;
    }

}

