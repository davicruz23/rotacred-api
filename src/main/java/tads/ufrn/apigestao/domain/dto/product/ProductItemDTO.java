package tads.ufrn.apigestao.domain.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductItemDTO {

    private Long productId;
    private String productName;
    private Integer quantityBought;
}
