package tads.ufrn.apigestao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tads.ufrn.apigestao.domain.SaleReturn;
import tads.ufrn.apigestao.enums.SaleStatus;

import java.util.List;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    boolean existsBySaleId(long saleId);

    @Query("""
       select sr
       from SaleReturn sr
       join fetch sr.sale
       where (:status is null or sr.saleStatus = :status)
    """)
    List<SaleReturn> findAllWithSale(SaleStatus status);

    @Query("""
    SELECT sr.productId, COALESCE(SUM(sr.quantityReturned), 0)
    FROM SaleReturn sr
    WHERE sr.sale.id = :saleId
    GROUP BY sr.productId
""")
    List<Object[]> sumReturnedBySale(Long saleId);

    @Query("""
    SELECT COALESCE(SUM(sr.quantityReturned), 0)
    FROM SaleReturn sr
    WHERE sr.sale.id = :saleId
      AND sr.productId = :productId
""")
    int sumReturnedQuantity(Long saleId, Long productId);
}
