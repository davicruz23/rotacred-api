package tads.ufrn.apigestao.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.domain.dto.report.ReportResponse;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Geração centralizada dos PDFs dos relatórios. */
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR);
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(PT_BR);
    private static final Color BLUE = new Color(24, 72, 122);
    private static final Color LIGHT_BLUE = new Color(225, 235, 246);
    private static final Color BORDER = new Color(190, 198, 208);
    private static final BaseFont FOOTER_FONT = footerFont();

    private final ReportQueryService reports;

    public byte[] inventory(String name, String status, String sort) {
        List<ReportResponse.Inventory> data = reports.inventoryForPdf(name, status, sort);
        BigDecimal stockValue = data.stream().map(x -> money(x.unitValue()).multiply(BigDecimal.valueOf(nullSafe(x.currentStock()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return table("Estoque Atual", filters("Produto", name, "Status", status, "Ordenação", sort), false,
                List.of(col("Produto", 3), col("Marca", 2), col("Estoque", 1.2f), col("Valor unitário", 1.8f), col("Status", 1.8f)),
                data.stream().map(x -> List.of(text(x.productName()), text(x.brand()), text(x.currentStock()), brl(x.unitValue()), text(x.status()))).toList(),
                List.of("Produtos: " + data.size(), "Valor estimado em estoque: " + brl(stockValue)));
    }

    public byte[] productRanking(LocalDate start, LocalDate end, String city, Long sellerId, Long productId, String sort) {
        List<ReportResponse.ProductRanking> data = reports.productRankingForPdf(start, end, city, sellerId, productId, sort);
        return table("Produtos Mais Vendidos", filters("Início", start, "Fim", end, "Cidade", city, "Vendedor", sellerId, "Produto", productId, "Ordenação", sort), false,
                List.of(col("Produto", 3), col("Marca", 2), col("Quantidade", 1.5f), col("Faturamento", 2)),
                data.stream().map(x -> List.of(text(x.productName()), text(x.brand()), text(x.quantitySold()), brl(x.salesValue()))).toList(),
                List.of("Quantidade total vendida: " + data.stream().mapToLong(x -> nullSafe(x.quantitySold())).sum(),
                        "Faturamento total: " + brl(data.stream().map(x -> money(x.salesValue())).reduce(BigDecimal.ZERO, BigDecimal::add))));
    }

    public byte[] loadings(LocalDate start, LocalDate end, Long responsibleId, String sort) {
        List<ReportResponse.Loading> data = reports.loadingsForPdf(start, end, responsibleId, sort);
        return table("Carregamentos e Movimentações de Estoque", filters("Início", start, "Fim", end, "Responsável", responsibleId, "Ordenação", sort), false,
                List.of(col("Data", 1.5f), col("Descrição", 3), col("Responsável", 2.5f), col("Produtos", 1.2f), col("Quantidade", 1.5f)),
                data.stream().map(x -> List.of(date(x.date()), text(x.description()), text(x.responsibleName()), text(x.productCount()), text(x.quantityLoaded()))).toList(),
                List.of("Carregamentos: " + data.size(), "Quantidade total carregada: " + data.stream().mapToLong(x -> nullSafe(x.quantityLoaded())).sum()));
    }

    public byte[] sales(LocalDate start, LocalDate end, String city, Long clientId, Long sellerId, Long collectorId,
                        Long productId, String paymentMethod, String status, String sort) {
        List<ReportResponse.Sale> data = reports.salesForPdf(start, end, city, clientId, sellerId, collectorId, productId, paymentMethod, status, sort);
        BigDecimal total = data.stream().map(x -> money(x.total())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return table("Vendas por Período", filters("Início", start, "Fim", end, "Cidade", city, "Cliente", clientId, "Vendedor", sellerId,
                        "Cobrador", collectorId, "Produto", productId, "Forma de pagamento", paymentMethod, "Status", status, "Ordenação", sort), true,
                List.of(col("Data", 1.3f), col("Cliente", 2.5f), col("Cidade", 1.8f), col("Vendedor", 2.1f), col("Total", 1.5f), col("Pagamento", 1.6f), col("Status", 1.5f)),
                data.stream().map(x -> List.of(date(x.saleDate()), text(x.clientName()), text(x.city()), text(x.sellerName()), brl(x.total()), text(x.paymentMethod()), text(x.status()))).toList(),
                List.of("Vendas: " + data.size(), "Faturamento: " + brl(total), "Ticket médio: " + brl(average(total, data.size()))));
    }

    public byte[] salesSummary(LocalDate start, LocalDate end, String city, Long clientId, Long sellerId, Long collectorId,
                               Long productId, String paymentMethod, String status) {
        ReportResponse.SalesSummary data = reports.salesSummary(start, end, city, clientId, sellerId, collectorId, productId, paymentMethod, status).data();
        return indicators("Resumo de Vendas", filters("Início", start, "Fim", end, "Cidade", city, "Cliente", clientId, "Vendedor", sellerId,
                        "Cobrador", collectorId, "Produto", productId, "Forma de pagamento", paymentMethod, "Status", status),
                List.of(List.of("Quantidade de vendas", text(data.salesCount())), List.of("Faturamento", brl(data.revenue())), List.of("Ticket médio", brl(data.averageTicket()))));
    }

    public byte[] salesRanking(String dimension, LocalDate start, LocalDate end, String city, String sort) {
        List<ReportResponse.Ranking> data = reports.salesRankingForPdf(dimension, start, end, city, sort);
        boolean product = "product".equalsIgnoreCase(dimension) || dimension == null;
        return table("Ranking de Vendas - " + (dimension == null ? "Produto" : dimension), filters("Dimensão", dimension, "Início", start, "Fim", end, "Cidade", city, "Ordenação", sort), product,
                List.of(col("Identificação", 2.2f), col("Nome", 3), col(product ? "Quantidade" : "Vendas", 1.5f), col("Valor", 2)),
                data.stream().map(x -> List.of(text(x.id()), text(x.name()), text(product ? x.quantity() : x.salesCount()), brl(x.value()))).toList(),
                List.of("Itens no ranking: " + data.size()));
    }

    public byte[] collections(LocalDate start, LocalDate end, String dateType, String city, Long clientId, Long collectorId,
                               String paymentType, String status, String sort) {
        List<ReportResponse.Collection> data = reports.collectionsForPdf(start, end, dateType, city, clientId, collectorId, paymentType, status, sort);
        BigDecimal total = data.stream().map(x -> money(x.amount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return table("Cobranças e Parcelas", filters("Início", start, "Fim", end, "Tipo de data", dateType, "Cidade", city, "Cliente", clientId,
                        "Cobrador", collectorId, "Forma de pagamento", paymentType, "Status", status, "Ordenação", sort), true,
                List.of(col("Vencimento", 1.4f), col("Cliente", 2.4f), col("Cidade", 1.6f), col("Cobrador", 2.1f), col("Valor", 1.5f), col("Situação", 1.5f), col("Pagamento", 1.6f)),
                data.stream().map(x -> List.of(date(x.dueDate()), text(x.clientName()), text(x.city()), text(x.collectorName()), brl(x.amount()), text(collectionStatus(x)), dateTime(x.paymentDate()))).toList(),
                List.of("Parcelas: " + data.size(), "Valor total: " + brl(total)));
    }

    public byte[] collectionsSummary(LocalDate start, LocalDate end, String dateType, String city, Long clientId, Long collectorId, String paymentType) {
        ReportResponse.CollectionSummary data = reports.collectionsSummary(start, end, dateType, city, clientId, collectorId, paymentType).data();
        return indicators("Resumo de Cobranças", filters("Início", start, "Fim", end, "Tipo de data", dateType, "Cidade", city, "Cliente", clientId,
                        "Cobrador", collectorId, "Forma de pagamento", paymentType),
                List.of(List.of("Valor recebido", brl(data.receivedValue())), List.of("Valor pendente", brl(data.pendingValue())),
                        List.of("Valor atrasado", brl(data.overdueValue())), List.of("Parcelas recebidas", text(data.receivedCount())),
                        List.of("Parcelas pendentes", text(data.pendingCount())), List.of("Parcelas atrasadas", text(data.overdueCount()))));
    }

    public byte[] cities(LocalDate start, LocalDate end, String city, String sort) {
        List<ReportResponse.City> data = reports.citiesForPdf(start, end, city, sort);
        return table("Vendas e Inadimplência por Cidade", filters("Início", start, "Fim", end, "Cidade", city, "Ordenação", sort), false,
                List.of(col("Cidade", 2.5f), col("Vendas", 1.5f), col("Faturamento", 2), col("Clientes", 1.5f), col("Inadimplência", 2)),
                data.stream().map(x -> List.of(text(x.city()), text(x.salesCount()), brl(x.revenue()), text(x.clientCount()), brl(x.overdueValue()))).toList(),
                List.of("Cidades: " + data.size(), "Faturamento total: " + brl(data.stream().map(x -> money(x.revenue())).reduce(BigDecimal.ZERO, BigDecimal::add))));
    }

    public byte[] cityProducts(String city, LocalDate start, LocalDate end) {
        List<ReportResponse.ProductRanking> data = reports.cityProductsForPdf(city, start, end);
        return table("Produtos Mais Vendidos por Cidade", filters("Cidade", city, "Início", start, "Fim", end), false,
                List.of(col("Produto", 4), col("Quantidade", 2), col("Faturamento", 2.2f)),
                data.stream().map(x -> List.of(text(x.productName()), text(x.quantitySold()), brl(x.salesValue()))).toList(),
                List.of("Quantidade total: " + data.stream().mapToLong(x -> nullSafe(x.quantitySold())).sum()));
    }

    public byte[] returns(LocalDate start, LocalDate end, String city, Long clientId, Long sellerId, Long collectorId, Long productId, String status) {
        List<ReportResponse.Return> data = reports.returnsForPdf(start, end, city, clientId, sellerId, collectorId, productId, status);
        return table("Devoluções e Impacto no Estoque", filters("Início", start, "Fim", end, "Cidade", city, "Cliente", clientId,
                        "Vendedor", sellerId, "Cobrador", collectorId, "Produto", productId, "Status", status), true,
                List.of(col("Data", 1.5f), col("Produto", 2), col("Qtd.", .8f), col("Cliente", 2), col("Cidade", 1.5f), col("Status", 1.7f), col("Descrição", 3)),
                data.stream().map(x -> List.of(dateTime(x.returnDate()), text(x.productName()), text(x.quantity()), text(x.clientName()), text(x.city()), text(x.status()), text(x.description()))).toList(),
                List.of("Devoluções: " + data.size(), "Quantidade devolvida: " + data.stream().mapToLong(x -> nullSafe(x.quantity())).sum()));
    }

    public byte[] returnsSummary(LocalDate start, LocalDate end, String status) {
        ReportResponse.ReturnSummary data = reports.returnsSummary(start, end, status).data();
        return indicators("Resumo de Devoluções", filters("Início", start, "Fim", end, "Status", status),
                List.of(List.of("Quantidade de devoluções", text(data.returnCount())), List.of("Quantidade de produtos devolvidos", text(data.quantityReturned()))));
    }

    public byte[] returnProducts(LocalDate start, LocalDate end, String status) {
        List<ReportResponse.ProductRanking> data = reports.returnProductsForPdf(start, end, status);
        return table("Produtos Devolvidos", filters("Início", start, "Fim", end, "Status", status), false,
                List.of(col("Produto", 4), col("Quantidade devolvida", 2.5f)),
                data.stream().map(x -> List.of(text(x.productName()), text(x.quantitySold()))).toList(),
                List.of("Produtos: " + data.size(), "Quantidade total: " + data.stream().mapToLong(x -> nullSafe(x.quantitySold())).sum()));
    }

    public byte[] guarantees(LocalDate start, LocalDate end) {
        List<ReportResponse.Return> data = reports.guaranteesForPdf(start, end);
        return table("Solicitações de Garantia", filters("Início", start, "Fim", end), true,
                List.of(col("Data", 1.5f), col("Produto", 2), col("Qtd.", .8f), col("Cliente", 2), col("Cidade", 1.5f), col("Status", 1.8f), col("Descrição", 3)),
                data.stream().map(x -> List.of(dateTime(x.returnDate()), text(x.productName()), text(x.quantity()), text(x.clientName()), text(x.city()), text(x.status()), text(x.description()))).toList(),
                List.of("Solicitações: " + data.size()));
    }

    public byte[] clients(LocalDate start, LocalDate end, String city, String sort) {
        List<ReportResponse.ClientRanking> data = reports.clientsForPdf(start, end, city, sort);
        return table("Ranking de Clientes", filters("Início", start, "Fim", end, "Cidade", city, "Ordenação", sort), true,
                List.of(col("Cliente", 2.7f), col("Cidade", 1.7f), col("1ª venda", 1.3f), col("Última venda", 1.3f), col("Vendas", 1), col("Compras", 1.6f), col("Pagamentos", 1.6f), col("Atrasos", 1)),
                data.stream().map(x -> List.of(text(x.clientName()), text(x.city()), date(x.firstSaleDate()), date(x.lastSaleDate()), text(x.salesCount()), brl(x.purchasedValue()), brl(x.paidValue()), text(x.overdueCount()))).toList(),
                List.of("Clientes: " + data.size()));
    }

    private byte[] indicators(String name, String filterText, List<List<String>> rows) {
        return table(name, filterText, false, List.of(col("Indicador", 4), col("Valor", 3)), rows, List.of());
    }

    private byte[] table(String name, String filterText, boolean landscape, List<Column> columns,
                         List<List<String>> rows, List<String> totals) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Rectangle page = landscape ? PageSize.A4.rotate() : PageSize.A4;
            Document document = new Document(page, 28, 28, 42, 38);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new FooterEvent());
            document.addTitle("RotaCred - " + name);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 17, Font.BOLD, BLUE);
            Paragraph title = new Paragraph("RotaCred - " + name, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            Font metaFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
            document.add(new Paragraph("Emitido em: " + DATE_TIME.format(LocalDateTime.now(ZONE)), metaFont));
            document.add(new Paragraph("Filtros: " + filterText, metaFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            float[] widths = new float[columns.size()];
            for (int i = 0; i < columns.size(); i++) widths[i] = columns.get(i).width();
            table.setWidths(widths);
            table.setHeaderRows(1);
            Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
            for (Column column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column.header(), headerFont));
                cell.setBackgroundColor(BLUE);
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }
            Font bodyFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Nenhum registro encontrado para os filtros informados.", bodyFont));
                empty.setColspan(columns.size());
                empty.setPadding(8);
                table.addCell(empty);
            } else {
                for (List<String> row : rows) {
                    for (String value : row) {
                        PdfPCell cell = new PdfPCell(new Phrase(text(value), bodyFont));
                        cell.setPadding(4);
                        cell.setBorderColor(BORDER);
                        table.addCell(cell);
                    }
                }
            }
            document.add(table);
            if (!totals.isEmpty()) {
                document.add(Chunk.NEWLINE);
                Font totalFont = new Font(Font.HELVETICA, 9, Font.BOLD, BLUE);
                for (String total : totals) document.add(new Paragraph(total, totalFont));
            }
            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o PDF do relatório " + name, e);
        }
    }

    private static Column col(String header, float width) { return new Column(header, width); }

    private static String filters(Object... values) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            if (values[i + 1] != null && !values[i + 1].toString().isBlank()) result.add(values[i] + "=" + values[i + 1]);
        }
        return result.isEmpty() ? "Nenhum" : String.join(" | ", result);
    }

    private static String text(Object value) { return value == null ? "-" : value.toString(); }
    private static String date(LocalDate value) { return value == null ? "-" : DATE.format(value); }
    private static String dateTime(LocalDateTime value) { return value == null ? "-" : DATE_TIME.format(value); }
    private static String dateTime(OffsetDateTime value) { return value == null ? "-" : DATE_TIME.format(value.atZoneSameInstant(ZONE)); }
    private static BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static String brl(BigDecimal value) { return MONEY.format(money(value)); }
    private static long nullSafe(Long value) { return value == null ? 0 : value; }
    private static long nullSafe(Integer value) { return value == null ? 0 : value; }
    private static BigDecimal average(BigDecimal total, int count) { return count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP); }
    private static String collectionStatus(ReportResponse.Collection value) {
        if (Boolean.TRUE.equals(value.paid())) return "PAID";
        if (value.overdueDays() != null && value.overdueDays() > 0) return "OVERDUE";
        return "PENDING";
    }

    private record Column(String header, float width) { }

    private static final class FooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            canvas.saveState();
            canvas.setColorStroke(BORDER);
            canvas.moveTo(document.left(), 28);
            canvas.lineTo(document.right(), 28);
            canvas.stroke();
            canvas.beginText();
            canvas.setFontAndSize(FOOTER_FONT, 8);
            canvas.setColorFill(Color.GRAY);
            canvas.showTextAligned(Element.ALIGN_CENTER, "RotaCred - Página " + writer.getPageNumber(),
                    (document.left() + document.right()) / 2, 17, 0);
            canvas.endText();
            canvas.restoreState();
        }
    }

    private static BaseFont footerFont() {
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
