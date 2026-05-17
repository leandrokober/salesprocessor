package br.com.data.salesprocessor.messaging;

import br.com.data.salesprocessor.model.Sale;
import br.com.data.salesprocessor.repository.SaleRepository;
import br.com.data.salesprocessor.service.PdfReceiptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleConsumerTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private PdfReceiptService pdfReceiptService;

    @Test
    void receberEventoVenda_quandoVendaPendente_deveGerarPdfEAtualizarStatus() {
        Sale venda = venda("PENDENTE");
        SaleConsumer saleConsumer = new SaleConsumer(saleRepository, pdfReceiptService);

        when(saleRepository.findById("VENDA-001")).thenReturn(Optional.of(venda));
        when(pdfReceiptService.gerarComprovante(venda)).thenReturn(Path.of("target/comprovantes/comprovante-VENDA-001.pdf"));

        saleConsumer.receberEventoVenda("VENDA-001");

        verify(pdfReceiptService).gerarComprovante(venda);

        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(saleCaptor.capture());
        assertThat(saleCaptor.getValue().getStatusProcessamento()).isEqualTo("PROCESSADA");
    }

    @Test
    void receberEventoVenda_quandoVendaJaProcessada_deveIgnorarMensagemDuplicada() {
        Sale venda = venda("PROCESSADA");
        SaleConsumer saleConsumer = new SaleConsumer(saleRepository, pdfReceiptService);

        when(saleRepository.findById("VENDA-001")).thenReturn(Optional.of(venda));

        saleConsumer.receberEventoVenda("VENDA-001");

        verify(pdfReceiptService, never()).gerarComprovante(venda);
        verify(saleRepository, never()).save(venda);
    }

    private Sale venda(String status) {
        return Sale.builder()
                .id("VENDA-001")
                .nomeCliente("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .valorImposto(new BigDecimal("10.00"))
                .valorLiquido(new BigDecimal("90.00"))
                .dataCadastro(LocalDateTime.now())
                .statusProcessamento(status)
                .build();
    }
}
