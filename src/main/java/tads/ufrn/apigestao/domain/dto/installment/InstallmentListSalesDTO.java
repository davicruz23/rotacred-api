package tads.ufrn.apigestao.domain.dto.installment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InstallmentListSalesDTO {

    private Long id;
    private String dueDate;
    private BigDecimal amount;
}
