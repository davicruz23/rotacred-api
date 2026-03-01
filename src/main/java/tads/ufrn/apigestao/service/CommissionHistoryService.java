package tads.ufrn.apigestao.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.controller.mapper.CommissionHistoryMapper;
import tads.ufrn.apigestao.domain.CommissionHistory;
import tads.ufrn.apigestao.domain.dto.commissionHistory.CommissionHistoryDTO;
import tads.ufrn.apigestao.repository.CommissionHistoryRepository;

@Service
@RequiredArgsConstructor
public class CommissionHistoryService {

    private final CommissionHistoryRepository repository;

    public Page<CommissionHistoryDTO> getByCollector(Long collectorId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("generatedAt").descending());

        Page<CommissionHistory> result;

        if (collectorId != null) {
            result = repository.findByCollectorId(collectorId, pageable);
        } else {
            result = repository.findByCollectorIsNotNull(pageable);
        }

        return result.map(CommissionHistoryMapper::toDTO);
    }

    public Page<CommissionHistoryDTO> getBySeller(Long sellerId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("generatedAt").descending());

        Page<CommissionHistory> result;

        if (sellerId != null) {
            result = repository.findBySellerId(sellerId, pageable);
        } else {
            result = repository.findBySellerIsNotNull(pageable);
        }

        return result.map(CommissionHistoryMapper::toDTO);
    }
}
