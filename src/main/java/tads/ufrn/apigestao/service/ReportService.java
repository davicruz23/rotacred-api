package tads.ufrn.apigestao.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.domain.*;
import tads.ufrn.apigestao.exception.BusinessException;
import tads.ufrn.apigestao.repository.ChargingRepository;
import tads.ufrn.apigestao.repository.ClientRepository;
import tads.ufrn.apigestao.repository.ProductRepository;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ChargingRepository chargingRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;

    public byte[] generateChargingReport(Long id) {
        Charging charging = chargingRepository.findWithItems(id)
                .orElseThrow(() -> new BusinessException("Carregamento não encontrado"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        com.lowagie.text.Document document = new com.lowagie.text.Document();
        PdfWriter.getInstance(document, baos);

        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("RotaCred - Relatório de Carregamento\n\n", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(new Locale("pt", "BR"));




        document.add(new Paragraph("Descrição: " + charging.getDescription()));
        if (charging.getDate() != null) {
            String dataFormatada = charging.getDate().format(formatter);
            document.add(new Paragraph("Data: " + dataFormatada));
        }
        else {
            document.add(new Paragraph("Insdisponível"));
        }
        document.add(new Paragraph("Usuário: " + charging.getUser().getName()));
        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(new float[]{4f, 3f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);

        PdfPCell header1 = new PdfPCell(new Phrase("Produto", headerFont));
        PdfPCell header2 = new PdfPCell(new Phrase("Marca", headerFont));
        PdfPCell header3 = new PdfPCell(new Phrase("Qtd", headerFont));

        Stream.of(header1, header2).forEach(cell -> {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setBackgroundColor(new Color(230, 230, 230));
            cell.setPadding(8f);
            table.addCell(cell);
        });

        header3.setHorizontalAlignment(Element.ALIGN_CENTER);
        header3.setBackgroundColor(new Color(230, 230, 230));
        header3.setPadding(8f);
        table.addCell(header3);


        for (ChargingItem item : charging.getItems()) {
            PdfPCell c1 = new PdfPCell(new Phrase(item.getProduct().getName()));
            PdfPCell c2 = new PdfPCell(new Phrase(item.getProduct().getBrand()));
            PdfPCell c3 = new PdfPCell(new Phrase(item.getQuantity().toString()));

            c1.setPadding(6f);
            c2.setPadding(6f);
            c3.setPadding(6f);
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);

            table.addCell(c1);
            table.addCell(c2);
            table.addCell(c3);
        }

        document.add(table);

        document.close();

        return baos.toByteArray();
    }

    public byte[] generateProductReport() {

        List<Product> products = productRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        com.lowagie.text.Document document = new com.lowagie.text.Document();
        PdfWriter.getInstance(document, baos);

        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("RotaCred - Estoque Atual\n\n", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(new Locale("pt", "BR"));

        String dataFormatada = LocalDate.now().format(formatter);
        document.add(new Paragraph("Data: " + dataFormatada));
        document.add(new Paragraph("\n"));

        // Tabela
        PdfPTable table = new PdfPTable(new float[]{4f, 3f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);

        PdfPCell header1 = new PdfPCell(new Phrase("Produto", headerFont));
        PdfPCell header2 = new PdfPCell(new Phrase("Marca", headerFont));
        PdfPCell header3 = new PdfPCell(new Phrase("Qtd", headerFont));

        Stream.of(header1, header2).forEach(cell -> {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setBackgroundColor(new Color(230, 230, 230));
            cell.setPadding(8f);
            table.addCell(cell);
        });

        header3.setHorizontalAlignment(Element.ALIGN_CENTER);
        header3.setBackgroundColor(new Color(230, 230, 230));
        header3.setPadding(8f);
        table.addCell(header3);

        Font rowFont = new Font(Font.HELVETICA, 11);

        for (Product product : products) {
            PdfPCell c1 = new PdfPCell(new Phrase(product.getName(), rowFont));
            PdfPCell c2 = new PdfPCell(new Phrase(product.getBrand(), rowFont));
            PdfPCell c3 = new PdfPCell(new Phrase(String.valueOf(product.getAmount()), rowFont));

            c1.setPadding(6f);
            c2.setPadding(6f);
            c3.setPadding(6f);
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);

            table.addCell(c1);
            table.addCell(c2);
            table.addCell(c3);
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }


    public byte[] generateClientsByCityReport(String city) {

        List<Client> clients = clientRepository.findByAddress_CityIgnoreCase(city);

        if (clients.isEmpty()) {
            throw new BusinessException("Nenhum cliente encontrado na cidade: " + city);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        PdfWriter.getInstance(document, baos);
        document.open();

        // Cabeçalho do relatório
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("RotaCred - Clientes por Cidade\n\n", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Paragraph subtitle = new Paragraph("Cidade: " + city.toUpperCase() + "\n\n", subFont);
        document.add(subtitle);

        // Configuração da tabela
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{3, 2, 4, 2}); // tamanho das colunas

        // Cabeçalho da tabela
        addHeader(table, "Nome");
        addHeader(table, "Telefone");
        addHeader(table, "Endereço");
        addHeader(table, "CPF");

        // Dados da tabela
        for (Client client : clients) {
            table.addCell(client.getName());
            table.addCell(client.getPhone());

            Address address = client.getAddress();
            String fullAddress = address.getStreet() + ", " + address.getNumber() +
                    (address.getComplement() != null ? " - " + address.getComplement() : "");

            table.addCell(fullAddress);
            table.addCell(client.getCpf());
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private void addHeader(PdfPTable table, String title) {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(title, headerFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setPadding(6);
        table.addCell(cell);
    }
}
