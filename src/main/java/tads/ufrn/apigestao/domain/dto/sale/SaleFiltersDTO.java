package tads.ufrn.apigestao.domain.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import tads.ufrn.apigestao.enums.SaleStatus;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleFiltersDTO {

    private String clientName;
    private String cpf;
    private SaleStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate saleDate;
}
