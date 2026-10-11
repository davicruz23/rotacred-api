package tads.ufrn.apigestao.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import tads.ufrn.apigestao.domain.dto.report.ReportResponse.*;
import tads.ufrn.apigestao.service.ReportQueryService;
import tads.ufrn.apigestao.service.PdfReportService;
import tads.ufrn.apigestao.service.ReportService;

import java.time.LocalDate;

@RestController
@AllArgsConstructor
@RequestMapping({"/api/report", "/api/reports"})
public class ReportController {

    private final ReportService service;
    private final ReportQueryService queryService;
    private final PdfReportService pdfService;

    private static final String REPORT_ROLES = "hasAnyRole('SUPERADMIN','FUNCIONARIO','FISCAL','COBRADOR')";

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/stock")
    public ResponseEntity<Page<Inventory>> stock(@RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String sort,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.inventory(name, status, sort, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/stock/products/ranking")
    public ResponseEntity<Page<ProductRanking>> stockProductRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.productRanking(startDate, endDate, city, sellerId, productId, sort, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/stock/loadings")
    public ResponseEntity<Page<Loading>> loadings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long responsibleId, @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.loadings(startDate, endDate, responsibleId, sort, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/sales")
    public ResponseEntity<Page<Sale>> sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long sellerId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status, @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.sales(startDate, endDate, city, clientId, sellerId, collectorId, productId,
                paymentMethod, status, sort, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/sales/summary")
    public ResponseEntity<Summary<SalesSummary>> salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long sellerId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(queryService.salesSummary(startDate, endDate, city, clientId, sellerId, collectorId,
                productId, paymentMethod, status));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/sales/ranking")
    public ResponseEntity<Page<Ranking>> salesRanking(@RequestParam(defaultValue = "product") String dimension,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                       @RequestParam(required = false) String city,
                                                       @RequestParam(required = false) String sort,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.salesRanking(dimension, startDate, endDate, city, sort, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/collections")
    public ResponseEntity<Page<Collection>> collections(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DUE") String dateType, @RequestParam(required = false) String city,
            @RequestParam(required = false) Long clientId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) String paymentType, @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.collections(startDate, endDate, dateType, city, clientId, collectorId,
                paymentType, status, sort, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/collections/summary")
    public ResponseEntity<Summary<CollectionSummary>> collectionsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DUE") String dateType, @RequestParam(required = false) String city,
            @RequestParam(required = false) Long clientId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) String paymentType) {
        return ResponseEntity.ok(queryService.collectionsSummary(startDate, endDate, dateType, city, clientId, collectorId, paymentType));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/cities")
    public ResponseEntity<Page<City>> cities(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.cities(startDate, endDate, city, sort, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/cities/products")
    public ResponseEntity<Page<ProductRanking>> cityProducts(@RequestParam String city,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.cityProducts(city, startDate, endDate, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/returns")
    public ResponseEntity<Page<Return>> returns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long sellerId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.returns(startDate, endDate, city, clientId, sellerId, collectorId, productId, status, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/returns/summary")
    public ResponseEntity<Summary<ReturnSummary>> returnsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(queryService.returnsSummary(startDate, endDate, status));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/returns/products")
    public ResponseEntity<Page<ProductRanking>> returnProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.returnProducts(startDate, endDate, status, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/guarantees/requests")
    public ResponseEntity<Page<Return>> guarantees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.guarantees(startDate, endDate, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/clients/ranking")
    public ResponseEntity<Page<ClientRanking>> clients(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                       @RequestParam(required = false) String city, @RequestParam(required = false) String sort,
                                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.clients(startDate, endDate, city, sort, page, size));
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/stock/pdf")
    public ResponseEntity<byte[]> stockPdf(@RequestParam(required = false) String name,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String sort) {
        return pdf(pdfService.inventory(name, status, sort), "rotacred-estoque-atual.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/stock/products/ranking/pdf")
    public ResponseEntity<byte[]> stockProductRankingPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String sort) {
        return pdf(pdfService.productRanking(startDate, endDate, city, sellerId, productId, sort), "rotacred-produtos-mais-vendidos.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/stock/loadings/pdf")
    public ResponseEntity<byte[]> loadingsPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long responsibleId, @RequestParam(required = false) String sort) {
        return pdf(pdfService.loadings(startDate, endDate, responsibleId, sort), "rotacred-carregamentos.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/sales/pdf")
    public ResponseEntity<byte[]> salesPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long sellerId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status, @RequestParam(required = false) String sort) {
        return pdf(pdfService.sales(startDate, endDate, city, clientId, sellerId, collectorId, productId, paymentMethod, status, sort), "rotacred-vendas.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/sales/summary/pdf")
    public ResponseEntity<byte[]> salesSummaryPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long sellerId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status) {
        return pdf(pdfService.salesSummary(startDate, endDate, city, clientId, sellerId, collectorId, productId, paymentMethod, status), "rotacred-resumo-vendas.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/sales/ranking/pdf")
    public ResponseEntity<byte[]> salesRankingPdf(@RequestParam(defaultValue = "product") String dimension,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                   @RequestParam(required = false) String city,
                                                   @RequestParam(required = false) String sort) {
        return pdf(pdfService.salesRanking(dimension, startDate, endDate, city, sort), "rotacred-ranking-vendas.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/collections/pdf")
    public ResponseEntity<byte[]> collectionsPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DUE") String dateType, @RequestParam(required = false) String city,
            @RequestParam(required = false) Long clientId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) String paymentType, @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        return pdf(pdfService.collections(startDate, endDate, dateType, city, clientId, collectorId, paymentType, status, sort), "rotacred-cobrancas.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/collections/summary/pdf")
    public ResponseEntity<byte[]> collectionsSummaryPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DUE") String dateType, @RequestParam(required = false) String city,
            @RequestParam(required = false) Long clientId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) String paymentType) {
        return pdf(pdfService.collectionsSummary(startDate, endDate, dateType, city, clientId, collectorId, paymentType), "rotacred-resumo-cobrancas.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/cities/pdf")
    public ResponseEntity<byte[]> citiesPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) String sort) {
        return pdf(pdfService.cities(startDate, endDate, city, sort), "rotacred-cidades.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/cities/products/pdf")
    public ResponseEntity<byte[]> cityProductsPdf(@RequestParam String city,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return pdf(pdfService.cityProducts(city, startDate, endDate), "rotacred-produtos-por-cidade.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/returns/pdf")
    public ResponseEntity<byte[]> returnsPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long sellerId, @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String status) {
        return pdf(pdfService.returns(startDate, endDate, city, clientId, sellerId, collectorId, productId, status), "rotacred-devolucoes.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/returns/summary/pdf")
    public ResponseEntity<byte[]> returnsSummaryPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        return pdf(pdfService.returnsSummary(startDate, endDate, status), "rotacred-resumo-devolucoes.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/returns/products/pdf")
    public ResponseEntity<byte[]> returnProductsPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        return pdf(pdfService.returnProducts(startDate, endDate, status), "rotacred-produtos-devolvidos.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/guarantees/requests/pdf")
    public ResponseEntity<byte[]> guaranteesPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return pdf(pdfService.guarantees(startDate, endDate), "rotacred-garantias.pdf");
    }

    @PreAuthorize(REPORT_ROLES)
    @GetMapping("/clients/ranking/pdf")
    public ResponseEntity<byte[]> clientsPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city, @RequestParam(required = false) String sort) {
        return pdf(pdfService.clients(startDate, endDate, city, sort), "rotacred-ranking-clientes.pdf");
    }

    private ResponseEntity<byte[]> pdf(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'FUNCIONARIO')")
    @GetMapping("/{id}/chargings")
    public ResponseEntity<byte[]> gerarRelatorioCarregamento(@PathVariable Long id) {
        byte[] pdfBytes = service.generateChargingReport(id);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=carregamento_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'FUNCIONARIO')")
    @GetMapping("/products")
    public ResponseEntity<byte[]> gerarRelatorioProdutos() {
        byte[] pdfBytes = service.generateProductReport();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=produtos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'FUNCIONARIO')")
    @GetMapping("/clients-by-city")
    public ResponseEntity<byte[]> gerarRelatorioClientes(@RequestParam String city) {

        byte[] pdf = service.generateClientsByCityReport(city);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=clientes-" + city + ".pdf")
                .body(pdf);
    }

}
