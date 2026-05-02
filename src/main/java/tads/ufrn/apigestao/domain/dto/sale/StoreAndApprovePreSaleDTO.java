package tads.ufrn.apigestao.domain.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tads.ufrn.apigestao.domain.dto.preSale.UpsertPreSaleDTO;
import tads.ufrn.apigestao.enums.PaymentType;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreAndApprovePreSaleDTO {

    private UpsertPreSaleDTO preSale;

    private Long inspectorId;

    private PaymentType paymentMethod;

    private int installments;

    private BigDecimal cashPaid;

    private Double latitude;

    private Double longitude;

}