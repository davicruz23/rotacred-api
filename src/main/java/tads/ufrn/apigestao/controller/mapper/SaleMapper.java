package tads.ufrn.apigestao.controller.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tads.ufrn.apigestao.domain.CollectionAttempt;
import tads.ufrn.apigestao.domain.PreSaleItem;
import tads.ufrn.apigestao.domain.Sale;
import tads.ufrn.apigestao.domain.SaleReturn;
import tads.ufrn.apigestao.domain.dto.installment.InstallmentDTO;
import tads.ufrn.apigestao.domain.dto.installment.InstallmentStatusDTO;
import tads.ufrn.apigestao.domain.dto.product.ProductDTO;
import tads.ufrn.apigestao.domain.dto.product.ProductSaleDTO;
import tads.ufrn.apigestao.domain.dto.returnSale.SaleReturnInfoDTO;
import tads.ufrn.apigestao.domain.dto.sale.SaleCollectorDTO;
import tads.ufrn.apigestao.domain.dto.sale.SaleDTO;
import tads.ufrn.apigestao.domain.dto.sale.SaleLocationDTO;
import tads.ufrn.apigestao.domain.dto.sale.SaleReturnDTO;
import tads.ufrn.apigestao.enums.SaleStatus;
import tads.ufrn.apigestao.repository.CollectionAttemptRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SaleMapper {

    private static CollectionAttemptRepository attemptRepository;

    @Autowired
    public void setAttemptRepository(CollectionAttemptRepository repository) {
        SaleMapper.attemptRepository = repository;
    }

    public static SaleDTO mapper(Sale src) {

        List<SaleReturnInfoDTO> saleReturnInfos = null;

        if (src.getSaleReturns() != null && !src.getSaleReturns().isEmpty()) {

            saleReturnInfos = src.getSaleReturns().stream()
                    .map(sr -> {

                        // Busca o item original para calcular valor abatido
                        PreSaleItem item = src.getPreSale().getItems().stream()
                                .filter(i -> i.getProduct().getId().equals(sr.getProductId()))
                                .findFirst()
                                .orElse(null);

                        BigDecimal unitValue = item != null
                                ? item.getProduct().getValue()
                                : BigDecimal.ZERO;

                        BigDecimal valueAbatido =
                                unitValue.multiply(BigDecimal.valueOf(sr.getQuantityReturned()));

                        return SaleReturnInfoDTO.builder()
                                .saleReturnId(sr.getId())
                                .productId(sr.getProductId())
                                .productName(
                                        item != null ? item.getProduct().getName() : null
                                )
                                .quantityReturned(sr.getQuantityReturned())
                                .valueAbatido(valueAbatido)
                                .returnDate(
                                        sr.getReturnDate()
                                                .format(DateTimeFormatter.ofPattern(
                                                        "dd/MM/yyyy",
                                                        new Locale("pt", "BR")
                                                ))
                                )
                                .status(sr.getSaleStatus().getDescription())
                                .build();
                    })
                    .toList();
        }

        return SaleDTO.builder()
                .id(src.getId())
                .numberSale(src.getNumberSale())
                .saleDate(
                        src.getSaleDate()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "BR")))
                )
                .clientName(src.getPreSale().getClient().getName())
                .paymentType(src.getPaymentMethod().getDescription())
                .nParcel(src.getInstallments())
                .total(src.getTotal())

                // LOCAL DA APROVAÇÃO DA VENDA (NÃO MEXE)
                .latitude(src.getApprovalLocation().getLatitude())
                .longitude(src.getApprovalLocation().getLongitude())

                .products(
                        src.getPreSale().getItems().stream()
                                .map(item -> {

                                    BigDecimal valorTotal =
                                            item.getProduct()
                                                    .getValue()
                                                    .multiply(BigDecimal.valueOf(item.getQuantity()));

                                    return ProductMapper.mapperProductSale(
                                            item.getProduct(),
                                            item.getQuantity(),
                                            valorTotal
                                    );
                                })
                                .toList()
                )

                .installments(
                        src.getInstallmentsEntities().stream()
                                .map(inst -> {

                                    Double attemptLatitude = null;
                                    Double attemptLongitude = null;

                                    // Pega localização do pagamento apenas se estiver pago
                                    if (inst.isPaid() && attemptRepository != null) {
                                        Optional<CollectionAttempt> attemptOpt =
                                                attemptRepository.findTopByInstallmentIdOrderByAttemptAtDesc(inst.getId());

                                        if (attemptOpt.isPresent()) {
                                            CollectionAttempt attempt = attemptOpt.get();
                                            attemptLatitude = attempt.getLatitude();
                                            attemptLongitude = attempt.getLongitude();
                                        }
                                    }

                                    // Garante que amount nunca seja null
                                    BigDecimal amount;
                                    if (inst.isPaid()) {
                                        amount = inst.getPaidAmount() != null ? inst.getPaidAmount() : inst.getAmount();
                                    } else {
                                        amount = inst.getAmount();
                                    }

                                    return InstallmentDTO.builder()
                                            .id(inst.getId())
                                            .dueDate(
                                                    inst.getDueDate()
                                                            .format(DateTimeFormatter.ofPattern(
                                                                    "dd/MM/yyyy", new Locale("pt", "BR")))
                                            )
                                            .amount(amount)
                                            .paid(inst.isPaid())
                                            .isValid(inst.getIsValid())
                                            .attemptLatitude(attemptLatitude)
                                            .attemptLongitude(attemptLongitude)
                                            .build();
                                })
                                .toList()
                )
                .saleReturns(saleReturnInfos)
                .build();
    }


    public static SaleDTO toDTO(Sale sale) {
        if (sale == null) return null;

        return SaleDTO.builder()
                .numberSale(sale.getNumberSale())
                .saleDate(sale.getSaleDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "BR"))))
                .paymentType(sale.getPaymentMethod().name())
                .nParcel(sale.getInstallments())
                .clientName(sale.getPreSale().getClient().getName())
                .total(sale.getTotal())
                .build();
    }

    public static SaleReturnDTO toReturnDTO(Sale sale) {
        if (sale == null) return null;

        return SaleReturnDTO.builder()
                .saleId(sale.getId())
                .saleDate(sale.getSaleDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "BR"))))
                .clientName(sale.getPreSale().getClient().getName())
                .build();
    }

    public static SaleCollectorDTO saleCollector(Sale src) {

        return SaleCollectorDTO.builder()
                .id(src.getId())
                .saleDate(src.getSaleDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "BR"))))
                .client(src.getPreSale().getClient() != null
                        ? ClientMapper.clientSale(src.getPreSale().getClient())
                        : null)
                .products(src.getPreSale().getItems().stream()
                        .map(item -> ProductMapper.mapperProductSale(
                                item.getProduct(),
                                item.getQuantity(),
                                null
                        ))
                        .toList())
                .installments(
                        src.getInstallments() != 0 && src.getId() != null
                                ? src.getInstallmentsEntities().stream()
                                .map(InstallmentMapper::mapperStatus)
                                .toList()
                                : List.of()
                )
                .latitude(src.getApprovalLocation() != null ? src.getApprovalLocation().getLatitude() : null)
                .longitude(src.getApprovalLocation() != null ? src.getApprovalLocation().getLongitude() : null)
                .build();
    }


    public static SaleLocationDTO mapperSaleLocation(Sale src) {
        return SaleLocationDTO.builder()
                .saleId(src.getId())
                .idClient(src.getPreSale().getClient().getId())
                .clientName(src.getPreSale().getClient().getName())
                .build();
    }
}
