package tads.ufrn.apigestao.domain.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleSearchDTO {

    private Long saleId;
    private LocalDate saleDate;
    private String clientName;
    private String cpf;
    private String city;
}
