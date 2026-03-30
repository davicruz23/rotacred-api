package tads.ufrn.apigestao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tads.ufrn.apigestao.domain.Sale;
import tads.ufrn.apigestao.domain.dto.dashboard.DashboardSaleDTO;
import tads.ufrn.apigestao.domain.dto.sale.SaleSearchDTO;
import tads.ufrn.apigestao.domain.dto.sale.SalesByCityDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
    SELECT new tads.ufrn.apigestao.domain.dto.sale.SalesByCityDTO(c.address.city, COUNT(s))
    FROM Sale s
    JOIN s.preSale ps
    JOIN ps.client c
    GROUP BY c.address.city
""")
    List<SalesByCityDTO> countSaleByCity();

    @Query("""
    SELECT s
    FROM Sale s
    JOIN s.preSale ps
    JOIN ps.client c
    WHERE c.address.city = :city
    AND s.collector IS NULL
""")
    List<Sale> findUnassignedSalesByCity(String city);

    List<Sale> findByCollectorId(Long collectorId);

    @Query("""
    SELECT s
    FROM Sale s
    WHERE s.collector.id = :collectorId
      AND EXISTS (
          SELECT 1
          FROM Installment i
          WHERE i.sale.id = s.id
            AND i.paid = false
      )
""")
    List<Sale> findByCollectorIdWithPendingInstallments(@Param("collectorId") Long collectorId);

    @Query("SELECT s FROM Sale s " +
            "WHERE (:startDate IS NULL OR s.saleDate >= :startDate) " +
            "AND (:endDate IS NULL OR s.saleDate <= :endDate)")
    List<Sale> findSalesByDateRange(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT DATE_FORMAT(s.sale_date, '%Y-%m') AS mes,
                   COUNT(*) AS totalVendas
            FROM sale s
            WHERE s.sale_date >= DATE_SUB(CURDATE(), INTERVAL :meses MONTH)
            GROUP BY DATE_FORMAT(s.sale_date, '%Y-%m')
            ORDER BY mes
            """, nativeQuery = true)
    List<Object[]> findSalesPerMonth(int meses);

    @Query(value = """
        SELECT
            a.city AS city,
            COUNT(s.id) AS total_sales,
            GROUP_CONCAT(s.id) AS sale_ids
        FROM sale s
        JOIN pre_sale p  ON p.id = s.pre_sale_id
        JOIN client cl   ON cl.id = p.client_id
        JOIN address a   ON a.id = cl.address_id
        WHERE s.collector_id IS NULL
        GROUP BY a.city
        ORDER BY a.city
        """,
            nativeQuery = true)
    List<Object[]> findSalesGroupedByCity();

    @Query(value = """
        SELECT
            a.city AS city,
            s.id AS sale_id,
            s.pre_sale_id,
            cl.id AS client_id,
            cl.name as client_name,
            a.state,
            s.total
        FROM sale s
        JOIN pre_sale p  ON p.id = s.pre_sale_id
        JOIN client cl   ON cl.id = p.client_id
        JOIN address a   ON a.id = cl.address_id
        WHERE s.collector_id IS NULL
        ORDER BY a.city, s.id
        """,
            nativeQuery = true)
    List<Object[]> findSalesDetailedWithoutCollector();

    @Modifying
    @Query(value = """
    UPDATE sale 
    SET collector_id = :collectorId
    WHERE id IN (:saleIds)
""", nativeQuery = true)
    void assignCollector(Long collectorId, List<Long> saleIds);

    @Query("""
    SELECT new tads.ufrn.apigestao.domain.dto.sale.SaleSearchDTO(
        s.id,
        s.saleDate,
        c.name,
        c.cpf,
        a.city
    )
    FROM Sale s
    JOIN s.preSale ps
    JOIN ps.client c
    JOIN c.address a
    WHERE (:id IS NULL OR s.id = :id)
      AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
      AND (:cpf IS NULL OR c.cpf = :cpf)
      AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))

      AND EXISTS (
        SELECT 1
        FROM PreSaleItem psi
        WHERE psi.preSale = ps
          AND (
              psi.quantity >
              COALESCE(
                  (SELECT SUM(sr.quantityReturned)
                   FROM SaleReturn sr
                   WHERE sr.sale = s
                     AND sr.productId = psi.product.id),
                  0
              )
          )
      )

      AND (
          (SELECT COUNT(i) FROM Installment i WHERE i.sale = s) <= 2
          OR EXISTS (
              SELECT 1
              FROM Installment i
              WHERE i.sale = s
                AND i.paid = false
          )
      )
""")
    Page<SaleSearchDTO> searchSales(
            @Param("name") String name,
            @Param("id") Long id,
            @Param("cpf") String cpf,
            @Param("city") String city,
            Pageable pageable
    );
}
