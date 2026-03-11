package tads.ufrn.apigestao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tads.ufrn.apigestao.domain.PreSale;
import tads.ufrn.apigestao.domain.Sale;
import tads.ufrn.apigestao.enums.PreSaleStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreSaleRepository extends JpaRepository<PreSale, Long> {

    //List<PreSale> findByInspectorIdAndStatus(Long inspectorId, PreSaleStatus status);

    @Query("""
    select distinct ps
    from PreSale ps
    left join fetch ps.items i
    left join fetch i.product
    where ps.inspector.id = :inspectorId
      and ps.status = :status
""")
    List<PreSale> findByInspectorIdAndStatus(
            @Param("inspectorId") Long inspectorId,
            @Param("status") PreSaleStatus status
    );


    List<PreSale> findBySellerId(Long sellerId);

    @Query("""
    SELECT ps
    FROM PreSale ps
    WHERE ps.inspector.id = :inspectorId
      AND ps.status IN (
          tads.ufrn.apigestao.enums.PreSaleStatus.APROVADA,
          tads.ufrn.apigestao.enums.PreSaleStatus.RECUSADA
      )
""")
    List<PreSale> findAllByInspectorId(@Param("inspectorId") Long inspectorId);

    Long countByStatus(PreSaleStatus status);

    Optional<PreSale> findByUuidPreSale(String uuid);

}
