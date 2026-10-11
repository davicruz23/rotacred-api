package tads.ufrn.apigestao.domain.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/** DTOs usados pelos relatórios JSON. Todos os endpoints incluem o título do relatório. */
public final class ReportResponse {

    private ReportResponse() {
    }

    public record Page<T>(String title, List<T> content, int page, int size,
                          long totalElements, int totalPages) {
    }

    public record Summary<T>(String title, T data) {
    }

    public record Inventory(Long productId, String productName, String brand,
                            Integer currentStock, BigDecimal unitValue, String status) {
    }

    public record ProductRanking(Long productId, String productName, String brand,
                                 Long quantitySold, BigDecimal salesValue) {
    }

    public record Loading(Long loadingId, LocalDate date, String description,
                          Long responsibleId, String responsibleName,
                          Long productCount, Long quantityLoaded) {
    }

    public record SalesSummary(Long salesCount, BigDecimal revenue, BigDecimal averageTicket) {
    }

    public record Sale(Long saleId, LocalDate saleDate, Long clientId, String clientName,
                       String city, Long sellerId, String sellerName, Long collectorId,
                       String collectorName, BigDecimal total, String paymentMethod,
                       String status) {
    }

    public record Ranking(Long id, String name, Long quantity, Long salesCount,
                          BigDecimal value, BigDecimal paidValue, BigDecimal pendingValue,
                          Long overdueCount) {
    }

    public record Collection(Long installmentId, Long saleId, Long clientId,
                             String clientName, String city, Long collectorId,
                             String collectorName, LocalDate dueDate, BigDecimal amount,
                             Boolean paid, BigDecimal paidAmount, LocalDateTime paymentDate,
                             String paymentType, Long overdueDays) {
    }

    public record CollectionSummary(BigDecimal receivedValue, BigDecimal pendingValue,
                                    BigDecimal overdueValue, Long receivedCount,
                                    Long pendingCount, Long overdueCount) {
    }

    public record City(String city, Long salesCount, BigDecimal revenue,
                       Long clientCount, BigDecimal overdueValue) {
    }

    public record Return(Long returnId, Long saleId, OffsetDateTime returnDate,
                         Long productId, String productName, Integer quantity,
                         String status, String description, Long clientId,
                         String clientName, String city, Long sellerId,
                         String sellerName, Long collectorId, String collectorName) {
    }

    public record ReturnSummary(Long returnCount, Long quantityReturned) {
    }

    public record ClientRanking(Long clientId, String clientName, String city,
                                LocalDate firstSaleDate, LocalDate lastSaleDate,
                                Long salesCount, BigDecimal purchasedValue,
                                BigDecimal paidValue, Long overdueCount) {
    }
}
