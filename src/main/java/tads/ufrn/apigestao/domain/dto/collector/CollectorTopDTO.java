package tads.ufrn.apigestao.domain.dto.collector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectorTopDTO {

    private Long collectorId;
    private String collectorName;
    private Double totalCollectedToday;
    private Double totalToCollectThisMonth;
    private Double expectedUntilToday;
    private Double performancePercent;
}
