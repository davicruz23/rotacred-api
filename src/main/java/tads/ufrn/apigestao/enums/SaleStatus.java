package tads.ufrn.apigestao.enums;

import lombok.Getter;

@Getter
public enum SaleStatus {

        ATIVO(1, "ATIVO"),
        DEFEITO_PRODUTO(2, "PRODUTO COM DEFEITO"),
        DEVOLVIDO_CLIENTE(3, "DEVOLVIDO PARA O CLIENTE"),
        DESISTENCIA(4, "DESISTÊNCIA"),
        REAVIDO(5, "RECUPERADO"),
        DANIFICADO(6, "DANIFICADO"),
        FINALIZADO(7, "FINALIZADO");


        private final int value;
        private final String description;

    SaleStatus(int value, String description) {
            this.value = value;
            this.description = description;
    }

    public static SaleStatus fromValue(int value){
        for (SaleStatus type : SaleStatus.values()){
            if (type.getValue() == value){
                return type;
            }
        }
        throw new RuntimeException("Unknown value: "+ value);
    }
}
