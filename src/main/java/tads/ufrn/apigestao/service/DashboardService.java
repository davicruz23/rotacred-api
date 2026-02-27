package tads.ufrn.apigestao.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.domain.Collector;
import tads.ufrn.apigestao.domain.Installment;
import tads.ufrn.apigestao.domain.Sale;
import tads.ufrn.apigestao.domain.dto.collector.CollectorCommissionDTO;
import tads.ufrn.apigestao.domain.dto.collector.CollectorTopDTO;
import tads.ufrn.apigestao.domain.dto.dashboard.*;
import tads.ufrn.apigestao.enums.PreSaleStatus;
import tads.ufrn.apigestao.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final PreSaleRepository preSaleRepository;
    private final CollectorRepository collectorRepository;
    private final InstallmentRepository installmentRepository;
    private final PreSaleItemRepository preSaleItemRepository;
    private final InstallmentRepository installmentItemRepository;
    private final ClientRepository clientRepository;
    private final AddressRepository addressRepository;
    private final ChargingRepository chargingRepository;
    private final SaleService saleService;

    public DashboardSaleDTO getSalesDashboard(LocalDate startDate, LocalDate endDate) {

        if (startDate == null || endDate == null) {
            LocalDate hoje = LocalDate.now();
            startDate = hoje.withDayOfMonth(1);
            endDate = hoje.withDayOfMonth(hoje.lengthOfMonth());
        }

        List<Sale> sales = saleRepository.findSalesByDateRange(startDate, endDate);

        Long totalVendas = (long) sales.size();

        BigDecimal totalValor = sales.stream()
                .map(Sale::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate startPrev = startDate.minusMonths(1);
        LocalDate endPrev = startPrev.withDayOfMonth(startPrev.lengthOfMonth());

        List<Sale> prevSales = saleRepository.findSalesByDateRange(startPrev, endPrev);

        BigDecimal totalValorAnterior = prevSales.stream()
                .map(Sale::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentual = BigDecimal.ZERO;

        if (totalValorAnterior.compareTo(BigDecimal.ZERO) > 0) {
            percentual = totalValor
                    .subtract(totalValorAnterior)
                    .divide(totalValorAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new DashboardSaleDTO(
                totalVendas,
                totalValor.setScale(2, RoundingMode.HALF_UP),
                percentual
        );
    }


    public Map<String, Long> getSalesStatusCount() {
        Long pending = preSaleRepository.countByStatus(PreSaleStatus.fromValue(1));
        Long approved = preSaleRepository.countByStatus(PreSaleStatus.fromValue(2));
        Long rejected = preSaleRepository.countByStatus(PreSaleStatus.fromValue(3));

        Map<String, Long> result = new HashMap<>();
        result.put("PENDENTE", pending);
        result.put("APROVADA", approved);
        result.put("RECUSADA", rejected);
        return result;
    }

    public List<CollectorChargeSummaryDTO> getCollectorsChargeSummary(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = collectorRepository.findCollectorsWithChargeCountAndTotal(startDate, endDate);

        return results.stream().map(result -> {
            String collectorName = (String) result[0];
            Long chargeCount = result[1] != null ? ((Number) result[1]).longValue() : 0L;
            Double totalAmountDouble = (Double) result[2];
            BigDecimal totalAmount = totalAmountDouble != null ? BigDecimal.valueOf(totalAmountDouble) : BigDecimal.ZERO;

            return new CollectorChargeSummaryDTO(collectorName, chargeCount, totalAmount);
        }).collect(Collectors.toList());
    }

    public List<CollectorCommissionDTO> getAllCommissionsByPeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<Collector> collectors = collectorRepository.findAll();

        return collectors.stream().map(collector -> {

            List<Sale> sales = saleService.getSalesByCollector(collector.getId());

            BigDecimal totalCollected = sales.stream()
                    .flatMap(sale ->
                            installmentRepository.findBySaleId(sale.getId()).stream()
                    )
                    .filter(inst ->
                            inst.isPaid()
                                    && inst.isCommissionable()
                                    && inst.getPaymentDate() != null
                                    && (startDate == null
                                    || !inst.getPaymentDate()
                                    .isBefore(startDate.atStartOfDay()))
                                    && (endDate == null
                                    || !inst.getPaymentDate()
                                    .isAfter(endDate.atTime(23, 59, 59)))
                    )
                    .map(Installment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal commission = totalCollected
                    .multiply(BigDecimal.valueOf(0.01))
                    .setScale(2, RoundingMode.HALF_UP);

            return new CollectorCommissionDTO(
                    collector.getId(),
                    collector.getUser().getName(),
                    startDate,
                    endDate,
                    commission
            );

        }).collect(Collectors.toList());
    }


    public List<DashboardProductSalesDTO> getTotalProductsSold(LocalDate startDate, LocalDate endDate) {

        Pageable topFour = PageRequest.of(0, 4);

        return preSaleItemRepository
                .findTotalProductsSoldByDateRange(startDate, endDate, topFour);
    }

    public DashboardTotalCobradoDTO getTotalCobrado() {

        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now(zone);

        LocalDate inicioSemanaAtualDate = hoje.with(DayOfWeek.MONDAY);
        LocalDate fimSemanaAtualDate = hoje.with(DayOfWeek.SUNDAY);

        LocalDateTime inicioSemanaAtual = inicioSemanaAtualDate.atStartOfDay();
        LocalDateTime fimSemanaAtual = fimSemanaAtualDate.atTime(LocalTime.MAX);

        // Semana anterior (segunda até domingo completa)
        LocalDate inicioSemanaAnteriorDate = hoje.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate fimSemanaAnteriorDate = hoje.minusWeeks(1).with(DayOfWeek.SUNDAY);

        LocalDateTime inicioSemanaAnterior = inicioSemanaAnteriorDate.atStartOfDay();
        LocalDateTime fimSemanaAnterior = fimSemanaAnteriorDate.atTime(LocalTime.MAX);

        BigDecimal totalSemanaAtual = installmentItemRepository
                .findTotalCobradoPorPeriodo(inicioSemanaAtual, fimSemanaAtual);

        BigDecimal totalSemanaAnterior = installmentItemRepository
                .findTotalCobradoPorPeriodo(inicioSemanaAnterior, fimSemanaAnterior);

        totalSemanaAtual = Optional.ofNullable(totalSemanaAtual).orElse(BigDecimal.ZERO);
        totalSemanaAnterior = Optional.ofNullable(totalSemanaAnterior).orElse(BigDecimal.ZERO);

        BigDecimal percentual = getBigDecimal(totalSemanaAnterior, totalSemanaAtual);

        return new DashboardTotalCobradoDTO(totalSemanaAtual, percentual);
    }

    private static BigDecimal getBigDecimal(BigDecimal totalSemanaAnterior, BigDecimal totalSemanaAtual) {
        BigDecimal percentual;

        if (totalSemanaAnterior.compareTo(BigDecimal.ZERO) == 0) {

            if (totalSemanaAtual.compareTo(BigDecimal.ZERO) > 0) {
                percentual = BigDecimal.valueOf(100);
            } else {
                percentual = BigDecimal.ZERO;
            }

        } else {

            percentual = totalSemanaAtual
                    .subtract(totalSemanaAnterior)
                    .divide(totalSemanaAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return percentual;
    }

    public DashboardTotalClientsDTO getTotalClients() {
        Long total = clientRepository.countTotalClients();
        return new DashboardTotalClientsDTO(total);
    }

    public List<DashboardSalesPerMonthDTO> getSalesPerMonth(int meses) {
        List<Object[]> rawData = saleRepository.findSalesPerMonth(meses);
        return rawData.stream()
                .map(row -> new DashboardSalesPerMonthDTO(
                        row[0].toString(),
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    public Long getDistinctCityCount() {
        return addressRepository.countDistinctCities();
    }

    public Long getCountChargins() {
        return chargingRepository.countDistinctCities();
    }

    public List<CollectorTopDTO> getTopCollectorsStatus() {

        List<Object[]> rows = installmentRepository.findTopCollectorsStatus();
        List<CollectorTopDTO> result = new ArrayList<>();

        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now(zone);

        int diaAtual = hoje.getDayOfMonth();
        int totalDiasMes = hoje.lengthOfMonth();

        for (Object[] row : rows) {

            Long collectorId = ((Number) row[0]).longValue();
            String collectorName = (String) row[1];

            BigDecimal totalCollectedToday = row[2] != null
                    ? BigDecimal.valueOf(((Number) row[2]).doubleValue())
                    : BigDecimal.ZERO;

            BigDecimal totalCollectedMonth = row[3] != null
                    ? BigDecimal.valueOf(((Number) row[3]).doubleValue())
                    : BigDecimal.ZERO;

            BigDecimal totalToCollectThisMonth = row[4] != null
                    ? BigDecimal.valueOf(((Number) row[4]).doubleValue())
                    : BigDecimal.ZERO;

            // Meta diária média
            BigDecimal metaDiaria = BigDecimal.ZERO;

            if (totalDiasMes > 0) {
                metaDiaria = totalToCollectThisMonth
                        .divide(BigDecimal.valueOf(totalDiasMes), 4, RoundingMode.HALF_UP);
            }

            // Meta esperada até hoje
            BigDecimal metaEsperadaAteHoje = metaDiaria
                    .multiply(BigDecimal.valueOf(diaAtual))
                    .setScale(2, RoundingMode.HALF_UP);

            // Performance percentual
            BigDecimal performancePercent = BigDecimal.ZERO;

            if (metaEsperadaAteHoje.compareTo(BigDecimal.ZERO) > 0) {
                performancePercent = totalCollectedMonth
                        .divide(metaEsperadaAteHoje, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            result.add(new CollectorTopDTO(
                    collectorId,
                    collectorName,
                    totalCollectedToday.doubleValue(),
                    totalToCollectThisMonth.doubleValue(),
                    metaEsperadaAteHoje.doubleValue(),
                    performancePercent.doubleValue()
            ));
        }

        return result;
    }

}
