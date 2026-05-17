package br.com.data.salesprocessor.service;

import br.com.data.salesprocessor.exception.ReceiptGenerationException;
import br.com.data.salesprocessor.model.Sale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfReceiptService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Path outputDir;

    public PdfReceiptService(@Value("${salesprocessor.receipts.output-dir:target/comprovantes}") String outputDir) {
        this.outputDir = Path.of(outputDir);
    }

    public Path gerarComprovante(Sale venda) {
        try {
            Files.createDirectories(outputDir);

            Path comprovante = outputDir.resolve("comprovante-" + sanitizarNomeArquivo(venda.getId()) + ".pdf");
            Files.write(comprovante, criarPdf(venda));

            return comprovante;
        } catch (IOException e) {
            throw new ReceiptGenerationException(venda.getId(), e);
        }
    }

    private byte[] criarPdf(Sale venda) throws IOException {
        String conteudo = criarConteudoPagina(venda);
        byte[] conteudoBytes = conteudo.getBytes(StandardCharsets.ISO_8859_1);

        List<String> objetos = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + conteudoBytes.length + " >>\nstream\n" + conteudo + "endstream"
        );

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        escrever(pdf, "%PDF-1.4\n");

        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(pdf.size());
            escrever(pdf, (i + 1) + " 0 obj\n");
            escrever(pdf, objetos.get(i));
            escrever(pdf, "\nendobj\n");
        }

        int xrefOffset = pdf.size();
        escrever(pdf, "xref\n");
        escrever(pdf, "0 " + (objetos.size() + 1) + "\n");
        escrever(pdf, "0000000000 65535 f \n");

        for (int i = 1; i < offsets.size(); i++) {
            escrever(pdf, String.format("%010d 00000 n \n", offsets.get(i)));
        }

        escrever(pdf, "trailer\n");
        escrever(pdf, "<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\n");
        escrever(pdf, "startxref\n");
        escrever(pdf, String.valueOf(xrefOffset));
        escrever(pdf, "\n%%EOF\n");

        return pdf.toByteArray();
    }

    private String criarConteudoPagina(Sale venda) {
        return """
                BT
                /F1 18 Tf
                50 780 Td
                (%s) Tj
                /F1 12 Tf
                0 -36 Td
                (%s) Tj
                0 -22 Td
                (%s) Tj
                0 -22 Td
                (%s) Tj
                0 -22 Td
                (%s) Tj
                0 -22 Td
                (%s) Tj
                0 -22 Td
                (%s) Tj
                0 -22 Td
                (%s) Tj
                0 -36 Td
                (%s) Tj
                ET
                """.formatted(
                escaparTextoPdf("Comprovante de Venda"),
                escaparTextoPdf("ID da venda: " + venda.getId()),
                escaparTextoPdf("Cliente: " + venda.getNomeCliente()),
                escaparTextoPdf("Valor bruto: R$ " + formatarValor(venda.getValorBruto())),
                escaparTextoPdf("Imposto: R$ " + formatarValor(venda.getValorImposto())),
                escaparTextoPdf("Valor liquido: R$ " + formatarValor(venda.getValorLiquido())),
                escaparTextoPdf("Data de cadastro: " + venda.getDataCadastro().format(DATE_FORMATTER)),
                escaparTextoPdf("Status: PROCESSADA"),
                escaparTextoPdf("Comprovante gerado automaticamente pelo Sales Processor.")
        );
    }

    private String formatarValor(java.math.BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String escaparTextoPdf(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String sanitizarNomeArquivo(String valor) {
        return valor.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void escrever(ByteArrayOutputStream outputStream, String texto) throws IOException {
        outputStream.write(texto.getBytes(StandardCharsets.ISO_8859_1));
    }
}
