package tads.ufrn.apigestao.controller.mapper;

import tads.ufrn.apigestao.domain.PreSaleItem;
import tads.ufrn.apigestao.domain.dto.preSaleItem.PreSaleItemDTO;

public class PreSaleItemMapper {
    public static PreSaleItemDTO mapper(PreSaleItem src) {
        return PreSaleItemDTO.builder()
                .id(src.getId())
                .productId(src.getProduct().getId())
                .productName(src.getProduct().getName())
                .quantity(src.getQuantity())
                .unitPrice(src.getProduct().getValue())
                .build();
    }
}
