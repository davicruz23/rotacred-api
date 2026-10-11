package tads.ufrn.apigestao.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tads.ufrn.apigestao.domain.dto.report.ReportResponse;
import tads.ufrn.apigestao.enums.PaymentType;
import tads.ufrn.apigestao.enums.SaleStatus;
import tads.ufrn.apigestao.repository.ReportRepository;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryService {

    private static final String PREFIX = "RotaCred - ";
    public static final int PDF_MAX_ROWS = 100_000;
    private final ReportRepository repository;

    public ReportResponse.Page<ReportResponse.Inventory> inventory(String name, String status,
                                                                    String sort, int page, int size) {
        var result = repository.inventory(clean(name), stockStatus(status), sortValue(sort, "stockDesc"), pageRequest(page, size));
        return page(result, "Estoque Atual", row -> new ReportResponse.Inventory(
                longValue(row[0]), string(row[1]), string(row[2]), integer(row[3]), decimal(row[4]), string(row[5])));
    }

    public ReportResponse.Page<ReportResponse.ProductRanking> productRanking(LocalDate startDate, LocalDate endDate,
                                                                               String city, Long sellerId, Long productId,
                                                                               String sort, int page, int size) {
        var result = repository.productRanking(startDate, endDate, clean(city), sellerId, productId,
                sortValue(sort, "quantityDesc"), pageRequest(page, size));
        return page(result, "Produtos Mais Vendidos", row -> new ReportResponse.ProductRanking(
                longValue(row[0]), string(row[1]), string(row[2]), longValue(row[3]), decimal(row[4])));
    }

    public ReportResponse.Page<ReportResponse.Loading> loadings(LocalDate startDate, LocalDate endDate,
                                                                 Long responsibleId, String sort, int page, int size) {
        var result = repository.loadings(startDate, endDate, responsibleId, sortValue(sort, "dateDesc"), pageRequest(page, size));
        return page(result, "Carregamentos e Movimentações de Estoque", row -> new ReportResponse.Loading(
                longValue(row[0]), localDate(row[1]), string(row[2]), longValue(row[3]), string(row[4]),
                longValue(row[5]), longValue(row[6])));
    }

    public ReportResponse.Page<ReportResponse.Sale> sales(LocalDate startDate, LocalDate endDate, String city,
                                                           Long clientId, Long sellerId, Long collectorId,
                                                           Long productId, String paymentMethod, String saleStatus,
                                                           String sort, int page, int size) {
        var result = repository.sales(startDate, endDate, clean(city), clientId, sellerId, collectorId, productId,
                enumName(paymentMethod), enumName(saleStatus), sortValue(sort, "dateDesc"), pageRequest(page, size));
        return page(result, "Vendas por Período", row -> new ReportResponse.Sale(
                longValue(row[0]), localDate(row[1]), longValue(row[2]), string(row[3]), string(row[4]),
                longValue(row[5]), string(row[6]), longValue(row[7]), string(row[8]), decimal(row[9]),
                string(row[10]), string(row[11])));
    }

    public ReportResponse.Summary<ReportResponse.SalesSummary> salesSummary(LocalDate startDate, LocalDate endDate,
                                                                              String city, Long clientId, Long sellerId,
                                                                              Long collectorId, Long productId,
                                                                              String paymentMethod, String saleStatus) {
        Object[] row = flatten(repository.salesSummary(startDate, endDate, clean(city), clientId, sellerId, collectorId,
                productId, enumValue(paymentMethod, PaymentType.class), enumValue(saleStatus, SaleStatus.class)));
        return new ReportResponse.Summary<>(title("Resumo de Vendas"), new ReportResponse.SalesSummary(
                longValueAt(row, 0), decimalAt(row, 1), decimalAt(row, 2)));
    }

    public ReportResponse.Page<ReportResponse.Ranking> salesRanking(String dimension, LocalDate startDate,
                                                                     LocalDate endDate, String city, String sort,
                                                                     int page, int size) {
        Pageable pageable = pageRequest(page, size);
        String normalized = clean(dimension);
        if (normalized == null) normalized = "product";
        var result = switch (normalized.toLowerCase(Locale.ROOT)) {
            case "seller", "vendedor" -> repository.sellerRanking(startDate, endDate, clean(city), sortValue(sort, "valueDesc"), pageable);
            case "payment", "pagamento" -> repository.paymentRanking(startDate, endDate, clean(city), sortValue(sort, "valueDesc"), pageable);
            case "client", "cliente" -> repository.clientRanking(startDate, endDate, clean(city), sortValue(sort, "valueDesc"), pageable);
            default -> repository.productRanking(startDate, endDate, clean(city), null, null,
                    sortValue(sort, "quantityDesc"), pageable);
        };
        if (normalized.equalsIgnoreCase("client") || normalized.equalsIgnoreCase("cliente")) {
            return page(result, "Ranking de Vendas por Cliente", row -> new ReportResponse.Ranking(
                    longValue(row[0]), string(row[1]), null, longValue(row[5]), decimal(row[6]), decimal(row[7]), null, longValue(row[8])));
        }
        if (normalized.equalsIgnoreCase("seller") || normalized.equalsIgnoreCase("vendedor")) {
            return page(result, "Ranking de Vendas por Vendedor", row -> new ReportResponse.Ranking(
                    longValue(row[0]), string(row[1]), null, longValue(row[2]), decimal(row[3]), null, null, null));
        }
        if (normalized.equalsIgnoreCase("payment") || normalized.equalsIgnoreCase("pagamento")) {
            return page(result, "Ranking de Vendas por Forma de Pagamento", null,
                    row -> new ReportResponse.Ranking(null, string(row[0]), null, longValue(row[1]), decimal(row[2]), null, null, null));
        }
        return page(result, "Ranking de Produtos Vendidos", row -> new ReportResponse.Ranking(
                longValue(row[0]), string(row[1]), longValue(row[3]), null, decimal(row[4]), null, null, null));
    }

    public ReportResponse.Page<ReportResponse.Collection> collections(LocalDate startDate, LocalDate endDate,
                                                                       String dateType, String city, Long clientId,
                                                                       Long collectorId, String paymentType, String status,
                                                                       String sort, int page, int size) {
        String effectiveDateType = "PAYMENT".equalsIgnoreCase(dateType) ? "PAYMENT" : "DUE";
        var result = repository.collections(startDate, endDate, effectiveDateType, clean(city), clientId, collectorId,
                enumName(paymentType), collectionStatus(status), sortValue(sort, "dueDesc"), pageRequest(page, size));
        return page(result, "Cobranças e Parcelas", row -> new ReportResponse.Collection(
                longValue(row[0]), longValue(row[1]), longValue(row[2]), string(row[3]), string(row[4]), longValue(row[5]),
                string(row[6]), localDate(row[7]), decimal(row[8]), booleanValue(row[9]), decimal(row[10]),
                localDateTime(row[11]), string(row[12]), longValue(row[13])));
    }

    public ReportResponse.Summary<ReportResponse.CollectionSummary> collectionsSummary(LocalDate startDate,
                                                                                         LocalDate endDate, String dateType,
                                                                                         String city, Long clientId,
                                                                                         Long collectorId, String paymentType) {
        String effectiveDateType = "PAYMENT".equalsIgnoreCase(dateType) ? "PAYMENT" : "DUE";
        Object[] row = flatten(repository.collectionsSummary(startDate, endDate, effectiveDateType, clean(city), clientId,
                collectorId, enumName(paymentType)));
        return new ReportResponse.Summary<>(title("Resumo de Cobranças"), new ReportResponse.CollectionSummary(
                decimalAt(row, 0), decimalAt(row, 1), decimalAt(row, 2), longValueAt(row, 3), longValueAt(row, 4), longValueAt(row, 5)));
    }

    public ReportResponse.Page<ReportResponse.ClientRanking> clients(LocalDate startDate, LocalDate endDate,
                                                                      String city, String sort, int page, int size) {
        var result = repository.clientRanking(startDate, endDate, clean(city), sortValue(sort, "valueDesc"), pageRequest(page, size));
        return page(result, "Ranking de Clientes", row -> new ReportResponse.ClientRanking(
                longValue(row[0]), string(row[1]), string(row[2]), localDate(row[3]), localDate(row[4]), longValue(row[5]),
                decimal(row[6]), decimal(row[7]), longValue(row[8])));
    }

    public ReportResponse.Page<ReportResponse.City> cities(LocalDate startDate, LocalDate endDate, String city,
                                                            String sort, int page, int size) {
        var result = repository.cityRanking(startDate, endDate, clean(city), sortValue(sort, "revenueDesc"), pageRequest(page, size));
        return page(result, "Vendas e Inadimplência por Cidade", row -> new ReportResponse.City(
                string(row[0]), longValue(row[1]), decimal(row[2]), longValue(row[3]), decimal(row[4])));
    }

    public ReportResponse.Page<ReportResponse.ProductRanking> cityProducts(String city, LocalDate startDate,
                                                                            LocalDate endDate, int page, int size) {
        var result = repository.cityProductRanking(city, startDate, endDate, pageRequest(page, size));
        return page(result, "Produtos Mais Vendidos por Cidade", row -> new ReportResponse.ProductRanking(
                longValue(row[0]), string(row[1]), null, longValue(row[2]), decimal(row[3])));
    }

    public ReportResponse.Page<ReportResponse.Return> returns(LocalDate startDate, LocalDate endDate, String city,
                                                               Long clientId, Long sellerId, Long collectorId,
                                                               Long productId, String status, int page, int size) {
        var result = repository.returns(startDate, endDate, clean(city), clientId, sellerId, collectorId, productId,
                enumName(status), pageRequest(page, size));
        return page(result, "Devoluções e Impacto no Estoque", row -> new ReportResponse.Return(
                longValue(row[0]), longValue(row[1]), offsetDateTime(row[2]), longValue(row[3]), string(row[4]),
                integer(row[5]), string(row[6]), string(row[7]), longValue(row[8]), string(row[9]), string(row[10]),
                longValue(row[11]), string(row[12]), longValue(row[13]), string(row[14])));
    }

    public ReportResponse.Summary<ReportResponse.ReturnSummary> returnsSummary(LocalDate startDate, LocalDate endDate,
                                                                                 String status) {
        OffsetDateTime start = startDate == null ? null : startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = endDate == null ? null : endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        Object[] row = flatten(repository.returnsSummary(start, end, enumValue(status, SaleStatus.class)));
        return new ReportResponse.Summary<>(title("Resumo de Devoluções"), new ReportResponse.ReturnSummary(longValueAt(row, 0), longValueAt(row, 1)));
    }

    public ReportResponse.Page<ReportResponse.ProductRanking> returnProducts(LocalDate startDate, LocalDate endDate,
                                                                                String status, int page, int size) {
        var result = repository.returnProductRanking(startDate, endDate, enumName(status), pageRequest(page, size));
        return page(result, "Produtos Devolvidos", row -> new ReportResponse.ProductRanking(
                longValue(row[0]), string(row[1]), null, longValue(row[2]), null));
    }

    public ReportResponse.Page<ReportResponse.Return> guarantees(LocalDate startDate, LocalDate endDate,
                                                                  int page, int size) {
        var result = repository.guaranteeRequests(startDate, endDate, pageRequest(page, size));
        return page(result, "Solicitações de Garantia", row -> new ReportResponse.Return(
                longValue(row[0]), longValue(row[1]), offsetDateTime(row[2]), longValue(row[3]), string(row[4]), integer(row[5]),
                string(row[6]), string(row[7]), longValue(row[8]), string(row[9]), string(row[10]), longValue(row[11]),
                string(row[12]), longValue(row[13]), string(row[14])));
    }

    public List<ReportResponse.Inventory> inventoryForPdf(String name, String status, String sort) {
        var result = repository.inventory(clean(name), stockStatus(status), sortValue(sort, "stockDesc"), pdfPageable());
        return requirePdfSize(result, "estoque").getContent().stream().map(row -> new ReportResponse.Inventory(
                longValue(row[0]), string(row[1]), string(row[2]), integer(row[3]), decimal(row[4]), string(row[5]))).toList();
    }

    public List<ReportResponse.ProductRanking> productRankingForPdf(LocalDate startDate, LocalDate endDate,
                                                                     String city, Long sellerId, Long productId, String sort) {
        var result = repository.productRanking(startDate, endDate, clean(city), sellerId, productId,
                sortValue(sort, "quantityDesc"), pdfPageable());
        return requirePdfSize(result, "ranking de produtos").getContent().stream().map(row -> new ReportResponse.ProductRanking(
                longValue(row[0]), string(row[1]), string(row[2]), longValue(row[3]), decimal(row[4]))).toList();
    }

    public List<ReportResponse.Loading> loadingsForPdf(LocalDate startDate, LocalDate endDate, Long responsibleId, String sort) {
        var result = repository.loadings(startDate, endDate, responsibleId, sortValue(sort, "dateDesc"), pdfPageable());
        return requirePdfSize(result, "carregamentos").getContent().stream().map(row -> new ReportResponse.Loading(
                longValue(row[0]), localDate(row[1]), string(row[2]), longValue(row[3]), string(row[4]),
                longValue(row[5]), longValue(row[6]))).toList();
    }

    public List<ReportResponse.Sale> salesForPdf(LocalDate startDate, LocalDate endDate, String city, Long clientId,
                                                  Long sellerId, Long collectorId, Long productId, String paymentMethod,
                                                  String saleStatus, String sort) {
        var result = repository.sales(startDate, endDate, clean(city), clientId, sellerId, collectorId, productId,
                enumName(paymentMethod), enumName(saleStatus), sortValue(sort, "dateDesc"), pdfPageable());
        return requirePdfSize(result, "vendas").getContent().stream().map(row -> new ReportResponse.Sale(
                longValue(row[0]), localDate(row[1]), longValue(row[2]), string(row[3]), string(row[4]),
                longValue(row[5]), string(row[6]), longValue(row[7]), string(row[8]), decimal(row[9]),
                string(row[10]), string(row[11]))).toList();
    }

    public List<ReportResponse.Ranking> salesRankingForPdf(String dimension, LocalDate startDate, LocalDate endDate,
                                                            String city, String sort) {
        String normalized = clean(dimension);
        if (normalized == null) normalized = "product";
        var result = switch (normalized.toLowerCase(Locale.ROOT)) {
            case "seller", "vendedor" -> repository.sellerRanking(startDate, endDate, clean(city), sortValue(sort, "valueDesc"), pdfPageable());
            case "payment", "pagamento" -> repository.paymentRanking(startDate, endDate, clean(city), sortValue(sort, "valueDesc"), pdfPageable());
            case "client", "cliente" -> repository.clientRanking(startDate, endDate, clean(city), sortValue(sort, "valueDesc"), pdfPageable());
            default -> repository.productRanking(startDate, endDate, clean(city), null, null, sortValue(sort, "quantityDesc"), pdfPageable());
        };
        requirePdfSize(result, "ranking de vendas");
        if (normalized.equalsIgnoreCase("client") || normalized.equalsIgnoreCase("cliente")) {
            return result.getContent().stream().map(row -> new ReportResponse.Ranking(
                    longValue(row[0]), string(row[1]), null, longValue(row[5]), decimal(row[6]), decimal(row[7]), null, longValue(row[8]))).toList();
        }
        if (normalized.equalsIgnoreCase("seller") || normalized.equalsIgnoreCase("vendedor")) {
            return result.getContent().stream().map(row -> new ReportResponse.Ranking(
                    longValue(row[0]), string(row[1]), null, longValue(row[2]), decimal(row[3]), null, null, null)).toList();
        }
        if (normalized.equalsIgnoreCase("payment") || normalized.equalsIgnoreCase("pagamento")) {
            return result.getContent().stream().map(row -> new ReportResponse.Ranking(
                    null, string(row[0]), null, longValue(row[1]), decimal(row[2]), null, null, null)).toList();
        }
        return result.getContent().stream().map(row -> new ReportResponse.Ranking(
                longValue(row[0]), string(row[1]), longValue(row[3]), null, decimal(row[4]), null, null, null)).toList();
    }

    public List<ReportResponse.Collection> collectionsForPdf(LocalDate startDate, LocalDate endDate, String dateType,
                                                              String city, Long clientId, Long collectorId,
                                                              String paymentType, String status, String sort) {
        String effectiveDateType = "PAYMENT".equalsIgnoreCase(dateType) ? "PAYMENT" : "DUE";
        var result = repository.collections(startDate, endDate, effectiveDateType, clean(city), clientId, collectorId,
                enumName(paymentType), collectionStatus(status), sortValue(sort, "dueDesc"), pdfPageable());
        return requirePdfSize(result, "cobranças").getContent().stream().map(row -> new ReportResponse.Collection(
                longValue(row[0]), longValue(row[1]), longValue(row[2]), string(row[3]), string(row[4]), longValue(row[5]),
                string(row[6]), localDate(row[7]), decimal(row[8]), booleanValue(row[9]), decimal(row[10]),
                localDateTime(row[11]), string(row[12]), longValue(row[13]))).toList();
    }

    public List<ReportResponse.City> citiesForPdf(LocalDate startDate, LocalDate endDate, String city, String sort) {
        var result = repository.cityRanking(startDate, endDate, clean(city), sortValue(sort, "revenueDesc"), pdfPageable());
        return requirePdfSize(result, "cidades").getContent().stream().map(row -> new ReportResponse.City(
                string(row[0]), longValue(row[1]), decimal(row[2]), longValue(row[3]), decimal(row[4]))).toList();
    }

    public List<ReportResponse.ProductRanking> cityProductsForPdf(String city, LocalDate startDate, LocalDate endDate) {
        var result = repository.cityProductRanking(city, startDate, endDate, pdfPageable());
        return requirePdfSize(result, "produtos por cidade").getContent().stream().map(row -> new ReportResponse.ProductRanking(
                longValue(row[0]), string(row[1]), null, longValue(row[2]), decimal(row[3]))).toList();
    }

    public List<ReportResponse.Return> returnsForPdf(LocalDate startDate, LocalDate endDate, String city, Long clientId,
                                                      Long sellerId, Long collectorId, Long productId, String status) {
        var result = repository.returns(startDate, endDate, clean(city), clientId, sellerId, collectorId, productId,
                enumName(status), pdfPageable());
        return requirePdfSize(result, "devoluções").getContent().stream().map(row -> new ReportResponse.Return(
                longValue(row[0]), longValue(row[1]), offsetDateTime(row[2]), longValue(row[3]), string(row[4]),
                integer(row[5]), string(row[6]), string(row[7]), longValue(row[8]), string(row[9]), string(row[10]),
                longValue(row[11]), string(row[12]), longValue(row[13]), string(row[14]))).toList();
    }

    public List<ReportResponse.ProductRanking> returnProductsForPdf(LocalDate startDate, LocalDate endDate, String status) {
        var result = repository.returnProductRanking(startDate, endDate, enumName(status), pdfPageable());
        return requirePdfSize(result, "produtos devolvidos").getContent().stream().map(row -> new ReportResponse.ProductRanking(
                longValue(row[0]), string(row[1]), null, longValue(row[2]), null)).toList();
    }

    public List<ReportResponse.Return> guaranteesForPdf(LocalDate startDate, LocalDate endDate) {
        var result = repository.guaranteeRequests(startDate, endDate, pdfPageable());
        return requirePdfSize(result, "garantias").getContent().stream().map(row -> new ReportResponse.Return(
                longValue(row[0]), longValue(row[1]), offsetDateTime(row[2]), longValue(row[3]), string(row[4]), integer(row[5]),
                string(row[6]), string(row[7]), longValue(row[8]), string(row[9]), string(row[10]), longValue(row[11]),
                string(row[12]), longValue(row[13]), string(row[14]))).toList();
    }

    public List<ReportResponse.ClientRanking> clientsForPdf(LocalDate startDate, LocalDate endDate, String city, String sort) {
        var result = repository.clientRanking(startDate, endDate, clean(city), sortValue(sort, "valueDesc"), pdfPageable());
        return requirePdfSize(result, "clientes").getContent().stream().map(row -> new ReportResponse.ClientRanking(
                longValue(row[0]), string(row[1]), string(row[2]), localDate(row[3]), localDate(row[4]), longValue(row[5]),
                decimal(row[6]), decimal(row[7]), longValue(row[8]))).toList();
    }

    private static Pageable pdfPageable() {
        return PageRequest.of(0, PDF_MAX_ROWS + 1);
    }

    private static <T> org.springframework.data.domain.Page<T> requirePdfSize(
            org.springframework.data.domain.Page<T> result, String reportName) {
        if (result.getTotalElements() > PDF_MAX_ROWS) {
            throw new IllegalArgumentException("O relatório de " + reportName + " excede o limite seguro de " + PDF_MAX_ROWS + " registros.");
        }
        return result;
    }

    private <T> ReportResponse.Page<T> page(org.springframework.data.domain.Page<Object[]> page,
                                             String name, Function<Object[], T> mapper) {
        return page(page, name, null, mapper);
    }

    private <T> ReportResponse.Page<T> page(org.springframework.data.domain.Page<Object[]> page, String name,
                                             Object ignored, Function<Object[], T> mapper) {
        return new ReportResponse.Page<>(title(name), page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static Pageable pageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize);
    }

    private static String title(String name) { return PREFIX + name; }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String enumName(String value) { return clean(value) == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private static String stockStatus(String value) { return enumName(value); }
    private static String collectionStatus(String value) {
        String status = enumName(value);
        return status == null ? null : switch (status) {
            case "PAID", "PENDING", "OVERDUE", "UPCOMING" -> status;
            default -> throw new IllegalArgumentException("Status de cobrança inválido: " + value);
        };
    }
    private static String sortValue(String value, String fallback) { return clean(value) == null ? fallback : value.trim(); }

    private static <E extends Enum<E>> E enumValue(String value, Class<E> type) {
        String normalized = enumName(value);
        return normalized == null ? null : Enum.valueOf(type, normalized);
    }
    private static String string(Object value) { return value == null ? null : value.toString(); }
    private static Long longValue(Object value) { return value == null ? null : ((Number) value).longValue(); }
    private static Integer integer(Object value) { return value == null ? null : ((Number) value).intValue(); }
    private static BigDecimal decimal(Object value) { return value == null ? BigDecimal.ZERO : value instanceof BigDecimal b ? b : new BigDecimal(value.toString()); }
    private static Boolean booleanValue(Object value) { return value == null ? null : value instanceof Boolean b ? b : ((Number) value).intValue() != 0; }
    private static LocalDate localDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate d) return d;
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        if (value instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();
        return LocalDate.parse(value.toString());
    }
    private static LocalDateTime localDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime d) return d;
        if (value instanceof java.sql.Timestamp t) return t.toLocalDateTime();
        return LocalDateTime.parse(value.toString().replace(' ', 'T'));
    }
    private static OffsetDateTime offsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime d) return d;
        if (value instanceof java.sql.Timestamp t) return t.toInstant().atOffset(ZoneOffset.UTC);
        return OffsetDateTime.parse(value.toString());
    }

    private static Object[] flatten(Object[] value) {
        Object[] current = value;
        while (current != null && current.length == 1 && current[0] instanceof Object[] nested) {
            current = nested;
        }
        return current;
    }

    private static Object valueAt(Object[] row, int index) {
        if (row == null || row.length == 0) return null;
        Object value = index < row.length ? row[index] : null;
        while (value instanceof Object[] nested) {
            if (nested.length == 0) return null;
            value = nested.length > index ? nested[index] : nested[0];
        }
        return value;
    }

    private static Long longValueAt(Object[] row, int index) { return longValue(valueAt(row, index)); }
    private static BigDecimal decimalAt(Object[] row, int index) { return decimal(valueAt(row, index)); }
}
