package tads.ufrn.apigestao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tads.ufrn.apigestao.domain.CommissionHistory;

public interface CommissionHistoryRepository extends JpaRepository<CommissionHistory, Long> {

    Page<CommissionHistory> findByCollectorId(Long collectorId, Pageable pageable);

    Page<CommissionHistory> findBySellerId(Long sellerId, Pageable pageable);

    Page<CommissionHistory> findByCollectorIsNotNull(Pageable pageable);

    Page<CommissionHistory> findBySellerIsNotNull(Pageable pageable);
}
