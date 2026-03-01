package tads.ufrn.apigestao.domain.dto.commissionHistory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommissionHistoryDTO {

    private String ownerName;
    private String ownerType;
    private String interval;
    private LocalDateTime generatedAt;
    private BigDecimal totalCommission;
}
