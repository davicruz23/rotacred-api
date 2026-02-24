package tads.ufrn.apigestao.domain.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tads.ufrn.apigestao.domain.dto.product.ProductItemDTO;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleDetailDTO {

    private Long saleId;
    private LocalDate saleDate;
    private String clientName;
    private List<ProductItemDTO> products;

    public SaleDetailDTO(Long id, String string, String name) {
    }
}
