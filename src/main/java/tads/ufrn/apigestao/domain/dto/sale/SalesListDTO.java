package tads.ufrn.apigestao.domain.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tads.ufrn.apigestao.domain.dto.installment.InstallmentListSalesDTO;
import tads.ufrn.apigestao.domain.dto.product.ProductSaleDTO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SalesListDTO {

    private Long id;
    private String saleDate;
    private String paymentType;
    private Integer nParcel;
    private String clientName;
    private List<ProductSaleDTO> products;
    private List<InstallmentListSalesDTO> installments;
}
