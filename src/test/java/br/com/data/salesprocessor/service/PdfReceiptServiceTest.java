package br.com.data.salesprocessor.service;

import br.com.data.salesprocessor.model.Sale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReceiptServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void gerarComprovante_deveCriarArquivoPdfComDadosDaVenda() throws Exception {
        PdfReceiptService pdfReceiptService = new PdfReceiptService(tempDir.toString());
        Sale venda = Sale.builder()
                .id("VENDA-001")
                .nomeCliente("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .valorImposto(new BigDecimal("10.00"))
                .valorLiquido(new BigDecimal("90.00"))
                .dataCadastro(LocalDateTime.of(2026, 5, 17, 17, 30))
                .statusProcessamento("PENDENTE")
                .build();

        Path comprovante = pdfReceiptService.gerarComprovante(venda);

        assertThat(comprovante).exists();
        assertThat(comprovante.getFileName().toString()).isEqualTo("comprovante-VENDA-001.pdf");

        String conteudo = Files.readString(comprovante, StandardCharsets.ISO_8859_1);
        assertThat(conteudo).startsWith("%PDF-1.4");
        assertThat(conteudo).contains("Comprovante de Venda");
        assertThat(conteudo).contains("ID da venda: VENDA-001");
        assertThat(conteudo).contains("Cliente: Maria Silva");
    }
}
