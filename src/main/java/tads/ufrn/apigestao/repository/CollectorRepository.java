package tads.ufrn.apigestao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tads.ufrn.apigestao.domain.Collector;
import tads.ufrn.apigestao.enums.SaleStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CollectorRepository extends JpaRepository<Collector, Long> {

    Optional<Collector> findByUserId(Long userId);

    @Query("SELECT c.user.name, COUNT(cl), SUM(cl.total) " +
            "FROM Collector c LEFT JOIN c.sales cl " +
            "WHERE (:startDate IS NULL OR cl.saleDate >= :startDate) " +
            "AND (:endDate IS NULL OR cl.saleDate <= :endDate) " +
            "GROUP BY c.id, c.user.name")
    List<Object[]> findCollectorsWithChargeCountAndTotal(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("""
    SELECT DISTINCT c
    FROM Collector c
    JOIN FETCH c.sales s
    WHERE (:collectorId IS NULL OR c.id = :collectorId)
      AND (:status IS NULL OR s.status = :status)

      AND (
          :fullPaid IS NULL

          OR (

              :fullPaid = true AND NOT EXISTS (
                  SELECT 1
                  FROM Installment i
                  WHERE i.sale = s
                    AND i.paid = false
              )

          )

          OR (

              :fullPaid = false AND EXISTS (
                  SELECT 1
                  FROM Installment i
                  WHERE i.sale = s
                    AND i.paid = false
              )

          )
      )
""")
    List<Collector> findCollectorsWithSales(
            @Param("status") SaleStatus status,
            @Param("collectorId") Long collectorId,
            @Param("fullPaid") Boolean fullPaid
    );
}
