package tads.ufrn.apigestao.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tads.ufrn.apigestao.enums.PreSaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class PreSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate preSaleDate;

    private BigDecimal totalPreSale;

    @Column(unique = true)
    private String uuidPreSale;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "inspector_id", nullable = true)
    private Inspector inspector;

    @Enumerated(EnumType.STRING)
    private PreSaleStatus status;

    @OneToMany(mappedBy = "preSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreSaleItem> items = new ArrayList<>();
}
