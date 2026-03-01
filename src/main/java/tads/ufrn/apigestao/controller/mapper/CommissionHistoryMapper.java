package tads.ufrn.apigestao.controller.mapper;

import tads.ufrn.apigestao.domain.CommissionHistory;
import tads.ufrn.apigestao.domain.dto.commissionHistory.CommissionHistoryDTO;

import java.time.format.DateTimeFormatter;

public class CommissionHistoryMapper {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static CommissionHistoryDTO toDTO(CommissionHistory src) {

        String ownerName = null;
        String ownerType = null;

        if (src.getSeller() != null) {
            ownerName = src.getSeller().getUser().getName();
            ownerType = "SELLER";
        }

        if (src.getCollector() != null) {
            ownerName = src.getCollector().getUser().getName();
            ownerType = "COLLECTOR";
        }

        String interval = src.getStartDate().format(DATE_FORMAT)
                + " - "
                + src.getEndDate().format(DATE_FORMAT);


        return CommissionHistoryDTO.builder()
                .ownerName(ownerName)
                .ownerType(ownerType)
                .generatedAt(src.getGeneratedAt())
                .interval(interval)
                .totalCommission(src.getAmount())
                .build();
    }
}
