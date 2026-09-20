package tads.ufrn.apigestao.domain.dto.sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tads.ufrn.apigestao.domain.dto.preSale.UpsertPreSaleDTO;
import tads.ufrn.apigestao.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreAndApprovePreSaleDTO {

    @NotNull(message = "Os dados da pré-venda são obrigatórios")
    @Valid
    private UpsertPreSaleDTO preSale;

    private Long inspectorId;

    @NotNull(message = "O método de pagamento é obrigatório")
    private PaymentType paymentMethod;

    private int installments;

    private BigDecimal cashPaid;

    private Double latitude;

    private Double longitude;

    private LocalDate saleDate;
}