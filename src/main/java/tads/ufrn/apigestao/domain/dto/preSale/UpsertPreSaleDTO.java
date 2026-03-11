package tads.ufrn.apigestao.domain.dto.preSale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tads.ufrn.apigestao.domain.dto.client.UpsertClientDTO;
import tads.ufrn.apigestao.domain.dto.preSaleItem.UpsertPreSaleItemDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpsertPreSaleDTO {

    private Long id;
    private LocalDate preSaleDate;
    private Long sellerId;
    private UpsertClientDTO client;
    private List<UpsertPreSaleItemDTO> products;
    private Long chargingId;
    private String uuidPreSale;
}
