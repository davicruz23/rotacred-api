package tads.ufrn.apigestao.domain.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tads.ufrn.apigestao.domain.dto.installment.InstallmentDTO;
import tads.ufrn.apigestao.domain.dto.product.ProductSaleDTO;
import tads.ufrn.apigestao.domain.dto.returnSale.SaleReturnInfoDTO;
import tads.ufrn.apigestao.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaleDTO {

    private Long id;
    private Boolean fullPaid;
    private String numberSale;
    private String saleDate;
    private String paymentType;
    private Integer nParcel;
    private String clientName;
    private BigDecimal total;
    private Double longitude;
    private Double latitude;
    private List<ProductSaleDTO> products;
    private List<InstallmentDTO> installments;
    private List<SaleReturnInfoDTO> saleReturns;
}
