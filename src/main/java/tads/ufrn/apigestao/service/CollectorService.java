package tads.ufrn.apigestao.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.controller.mapper.CollectorMapper;
import tads.ufrn.apigestao.controller.mapper.SaleMapper;
import tads.ufrn.apigestao.domain.*;
import tads.ufrn.apigestao.domain.dto.collector.*;
import tads.ufrn.apigestao.domain.dto.commissionHistory.CollectorCommissionDTOO;
import tads.ufrn.apigestao.domain.dto.installment.InstallmentPaidDTO;
import tads.ufrn.apigestao.domain.dto.sale.AdminPaymentDTO;
import tads.ufrn.apigestao.domain.dto.sale.SaleCollectorDTO;
import tads.ufrn.apigestao.enums.CommissionReason;
import tads.ufrn.apigestao.enums.SaleStatus;
import tads.ufrn.apigestao.exception.BusinessException;
import tads.ufrn.apigestao.exception.ResourceNotFoundException;
import tads.ufrn.apigestao.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectorService {

    private final CollectorRepository repository;
    private final SaleRepository saleRepository;
    private final SaleService saleService;
    private final InstallmentRepository installmentRepository;
    private final CommissionHistoryRepository commissionHistoryRepository;
    private final ApprovalLocationRepository  approvalLocationRepository;
    private final CollectionAttemptRepository collectionAttemptRepository;

    private static final double RADIUS_METERS = 500;

    public List<Collector> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CollectorDTO> findAll(Integer status, Long collectorId, Boolean fullPaid) {

        SaleStatus statusEnum = null;

        if (status != null) {
            statusEnum = SaleStatus.fromValue(status);
        }

        List<Collector> collectors =
                repository.findCollectorsWithSales(statusEnum, collectorId, fullPaid);

        for (Collector collector : collectors) {

            for (Sale sale : collector.getSales()) {

                if (sale.getInstallmentsEntities() == null) continue;

                sale.setInstallmentsEntities(
                        sale.getInstallmentsEntities().stream()
                                .filter(inst ->
                                        !(inst.isPaid() &&
                                                BigDecimal.ZERO.compareTo(inst.getAmount()) == 0)
                                )
                                .toList()
                );

                for (Installment installment : sale.getInstallmentsEntities()) {

                    if (installment.isPaid()) {
                        try {
                            installment.setIsValid(
                                    isAttemptWithinApprovalLocation(installment.getId())
                            );
                        } catch (Exception e) {
                            installment.setIsValid(null);
                        }
                    } else {
                        installment.setIsValid(null);
                    }
                }
            }
        }

        return collectors.stream()
                .map(CollectorMapper::mapper)
                .toList();
    }

    public Collector findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public void createFromUser(User user) {
        Collector collector = new Collector();
        collector.setUser(user);
        repository.save(collector);
    }

    @Transactional
    public void registerAdminPayment(Long saleId, AdminPaymentDTO dto) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException("Venda não encontrada"));

        BigDecimal amountPaid = dto.getAmount();

        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Informe um valor de pagamento válido");
        }

        List<Installment> openInstallments = installmentRepository
                .findBySaleIdAndPaidFalseOrderByDueDateAscIdAsc(saleId);

        if (openInstallments.isEmpty()) {
            throw new BusinessException("Essa venda não possui parcelas em aberto");
        }

        BigDecimal totalOpenAmount = openInstallments.stream()
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (amountPaid.compareTo(totalOpenAmount) > 0) {
            throw new BusinessException("O pagamento não pode ser maior que o saldo devedor");
        }

        BigDecimal remainingPayment = amountPaid;

        for (Installment installment : openInstallments) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal installmentAmount = installment.getAmount();
            BigDecimal amountToPay = remainingPayment.min(installmentAmount);

            markAsPaidByAdmin(installment, amountToPay);

            remainingPayment = remainingPayment.subtract(amountToPay);
        }

        if (amountPaid.compareTo(totalOpenAmount) == 0) {
            sale.setStatus(SaleStatus.FINALIZADO);
            saleRepository.save(sale);
        }
    }

    private void markAsPaidByAdmin(
            Installment installment,
            BigDecimal amountPaid
    ) {
        BigDecimal installmentAmount = installment.getAmount();
        BigDecimal remaining = installmentAmount.subtract(amountPaid);

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            installment.setAmount(remaining);
            installment.setPaid(false);
            installment.setPaidAmount(BigDecimal.ZERO);
            installment.setPaymentDate(null);
            installment.setCommissionable(true);
        } else {
            installment.setPaid(true);
            installment.setPaymentDate(LocalDateTime.now());
            installment.setPaidAmount(amountPaid);
            installment.setCommissionable(false);
        }

        installmentRepository.save(installment);
    }

    @Transactional
    public CollectorSalesAssignedDTO assignSalesByCity(Long collectorId, String city) {
        Collector collector = repository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector não encontrado"));
        List<Sale> sales = saleRepository.findUnassignedSalesByCity(city);
        for (Sale sale : sales) {
            sale.setCollector(collector);
        }
        saleRepository.saveAll(sales);

        return new CollectorSalesAssignedDTO(
                collector.getId(),
                city,
                sales.size()
        );
    }

    public List<Sale> getSales(Long collectorId) {
        return saleService.getSalesByCollector(collectorId);
    }

    @Transactional
    public void markAsPaid(Long installmentId, BigDecimal amountPaid) {

        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new BusinessException("Parcela não encontrada"));

        if (installment.isPaid()) {
            throw new BusinessException("Parcela já está marcada como paga");
        }

        BigDecimal installmentAmount = installment.getAmount();
        BigDecimal remaining = installmentAmount.subtract(amountPaid);

        installment.setPaid(true);
        installment.setPaymentDate(LocalDateTime.now());
        installment.setPaidAmount(amountPaid);
        installmentRepository.save(installment);

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            handleRemainingBalance(installment, remaining);
        }

        Sale sale = installment.getSale();

        boolean hasOpenInstallment = installmentRepository
                .existsBySaleIdAndPaidFalse(sale.getId());

        if (!hasOpenInstallment) {
            sale.setStatus(SaleStatus.FINALIZADO);
            saleRepository.save(sale);
        }

        new InstallmentPaidDTO(
                installment.getId(),
                installment.getDueDate(),
                installment.getPaidAmount(),
                installment.isPaid(),
                installment.getPaymentDate()
        );
    }

    private void handleRemainingBalance(Installment current, BigDecimal remaining) {

        Optional<Installment> nextOpt =
                installmentRepository.findFirstBySaleAndDueDateAfterOrderByDueDateAsc(
                        current.getSale(),
                        current.getDueDate()
                );

        if (nextOpt.isPresent() && !nextOpt.get().isPaid()) {

            Installment next = nextOpt.get();

            next.setAmount(
                    next.getAmount().add(remaining)
            );
            next.setPaymentType(next.getPaymentType());
            next.setPaymentType(next.getPaymentType());

            installmentRepository.save(next);

        } else {
            Installment newInstallment = new Installment();
            newInstallment.setSale(current.getSale());
            newInstallment.setDueDate(current.getDueDate().plusMonths(1));
            newInstallment.setAmount(remaining);
            newInstallment.setPaid(false);
            newInstallment.setCommissionable(true);

            installmentRepository.save(newInstallment);
        }
    }

    @Transactional
    public CollectorCommissionDTOO getCommissionByPeriod(
            Long collectorId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal paymentPercentage,
            CommissionReason reason
    ) {

        if (startDate == null) {
            throw new BusinessException("A data inicial é obrigatória.");
        }

        if (endDate == null) {
            throw new BusinessException("A data final é obrigatória.");
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessException("A data final não pode ser anterior à data inicial.");
        }

        if (reason == null) {
            throw new BusinessException("O motivo da comissão é obrigatório.");
        }

        BigDecimal percentage = paymentPercentage != null
                ? paymentPercentage
                : BigDecimal.valueOf(100);

        if (percentage.compareTo(BigDecimal.ZERO) <= 0
                || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("O percentual deve ser maior que 0 e menor ou igual a 100.");
        }

        boolean isOnlyConsultation = reason == CommissionReason.CONSULTA
                || reason == CommissionReason.OUTRO;

        Collector collector = repository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Cobrador não encontrado"));

        List<Sale> sales = saleService.getSalesByCollector(collectorId);

        BigDecimal totalCollected = sales.stream()
                .flatMap(sale -> installmentRepository.findBySaleId(sale.getId()).stream())
                .filter(inst -> inst.isPaid()
                        && inst.isCommissionable()
                        && inst.getPaymentDate() != null
                        && !inst.getPaymentDate().isBefore(startDate.atStartOfDay())
                        && !inst.getPaymentDate().isAfter(endDate.atTime(23, 59, 59)))
                .map(Installment::getPaidAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = totalCollected
                .multiply(BigDecimal.valueOf(0.01))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal previousPaidAmount = commissionHistoryRepository
                .sumPaidAmountByCollectorAndPeriod(collectorId, startDate, endDate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentageValue = percentage
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal calculatedAmountToPay = totalCommission
                .multiply(percentageValue)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal amountToPay = calculatedAmountToPay;

        if (!isOnlyConsultation) {
            amountToPay = calculatedAmountToPay
                    .subtract(previousPaidAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            if (amountToPay.compareTo(BigDecimal.ZERO) < 0) {
                amountToPay = BigDecimal.ZERO;
            }

            CommissionHistory history = new CommissionHistory();
            history.setCollector(collector);
            history.setGeneratedAt(LocalDateTime.now());
            history.setStartDate(startDate);
            history.setEndDate(endDate);

            history.setAmount(amountToPay);
            history.setTotalCommission(totalCommission);
            history.setPaymentPercentage(percentage);
            history.setPreviousPaidAmount(previousPaidAmount);
            history.setReason(reason);

            commissionHistoryRepository.save(history);
        }

        return new CollectorCommissionDTOO(
                collector.getId(),
                collector.getUser().getName(),
                startDate,
                endDate,
                totalCommission,
                percentage,
                previousPaidAmount,
                amountToPay,
                reason
        );
    }

    @Transactional(readOnly = true)
    public Map<String, List<SaleCollectorDTO>> findSalesByCollectorId(Long collectorId) {

        if (!repository.existsById(collectorId)) {
            throw new BusinessException("Cobrador com ID " + collectorId + " não encontrado.");
        }

        List<Sale> sales = saleRepository.findByCollectorIdWithPendingInstallments(collectorId);

        List<SaleCollectorDTO> saleDTOs = sales.stream()
                .map(SaleMapper::saleCollector)
                .toList();

        return saleDTOs.stream()
                .collect(Collectors.groupingBy(
                        sale -> {
                            if (sale.getClient() != null && sale.getClient().getAddress() != null) {
                                return sale.getClient().getAddress().getCity();
                            } else {
                                return "Cidade não informada";
                            }
                        }
                ));
    }


    public CollectorIdUserDTO getCollectorByUserId(Long userId) {
        Collector collector = repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cobrador não encontrado para o usuário: " + userId));

        return new CollectorIdUserDTO(collector.getId());
    }

    public boolean isAttemptWithinApprovalLocation(Long installmentId) {

        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new BusinessException("Parcela não encontrada"));

        // Fonte da verdade
        if (!installment.isPaid()) {
            return false;
        }

        Sale sale = installment.getSale();

        ApprovalLocation approvalLocation =
                approvalLocationRepository.findBySaleId(sale.getId())
                        .orElseThrow(() -> new BusinessException("Local de aprovação não encontrado"));

        CollectionAttempt attempt =
                collectionAttemptRepository
                        .findTopByInstallmentIdOrderByAttemptAtDesc(installmentId)
                        .orElseThrow(() -> new BusinessException("Tentativa não encontrada"));

        return isAttemptWithinApprovalLocation(attempt);
    }

    public boolean isAttemptWithinApprovalLocation(CollectionAttempt attempt) {
        if (attempt == null
                || attempt.getInstallment() == null
                || !attempt.getInstallment().isPaid()
                || attempt.getLatitude() == null
                || attempt.getLongitude() == null) {
            return false;
        }

        Sale sale = attempt.getInstallment().getSale();

        ApprovalLocation approvalLocation =
                approvalLocationRepository.findBySaleId(sale.getId())
                        .orElseThrow(() -> new BusinessException("Local de aprovaÃ§Ã£o nÃ£o encontrado"));

        double distance = distanceInMeters(
                approvalLocation.getLatitude(),
                approvalLocation.getLongitude(),
                attempt.getLatitude(),
                attempt.getLongitude()
        );

        return distance <= RADIUS_METERS;
    }

    private double distanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // metros

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                        Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
