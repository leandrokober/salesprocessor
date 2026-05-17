package br.com.data.salesprocessor.controller;

import br.com.data.salesprocessor.dto.SaleRequestDTO;
import br.com.data.salesprocessor.exception.InvalidApiKeyException;
import br.com.data.salesprocessor.model.Sale;
import br.com.data.salesprocessor.service.SaleProcessingResult;
import br.com.data.salesprocessor.service.SaleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleControllerTest {

    private static final String API_KEY_VALIDA = "sales-token-secreto-123";

    @Mock
    private SaleService saleService;

    @InjectMocks
    private SaleController saleController;

    @Test
    void criarVenda_quandoApiKeyValidaEVendaNova_deveProcessarVendaERetornarCreated() {
        SaleRequestDTO dto = dto();
        Sale vendaProcessada = venda("PENDENTE");

        when(saleService.processarVenda(dto)).thenReturn(new SaleProcessingResult(vendaProcessada, true));

        ResponseEntity<?> response = saleController.criarVenda(API_KEY_VALIDA, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(vendaProcessada);
        verify(saleService).processarVenda(dto);
    }

    @Test
    void criarVenda_quandoVendaJaExiste_deveRetornarOkComDadosExistentes() {
        SaleRequestDTO dto = dto();
        Sale vendaExistente = venda("PROCESSADA");

        when(saleService.processarVenda(dto)).thenReturn(new SaleProcessingResult(vendaExistente, false));

        ResponseEntity<?> response = saleController.criarVenda(API_KEY_VALIDA, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vendaExistente);
        verify(saleService).processarVenda(dto);
    }

    @Test
    void criarVenda_quandoApiKeyAusente_deveRetornarUnauthorized() {
        SaleRequestDTO dto = dto();

        assertThatThrownBy(() -> saleController.criarVenda(null, dto))
                .isInstanceOf(InvalidApiKeyException.class)
                .hasMessage("Acesso negado: X-API-KEY invalida ou ausente");

        verify(saleService, never()).processarVenda(dto);
    }

    @Test
    void criarVenda_quandoApiKeyInvalida_deveRetornarUnauthorized() {
        SaleRequestDTO dto = dto();

        assertThatThrownBy(() -> saleController.criarVenda("token-invalido", dto))
                .isInstanceOf(InvalidApiKeyException.class)
                .hasMessage("Acesso negado: X-API-KEY invalida ou ausente");

        verify(saleService, never()).processarVenda(dto);
    }

    private SaleRequestDTO dto() {
        return SaleRequestDTO.builder()
                .id("venda-001")
                .clienteNome("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .build();
    }

    private Sale venda(String status) {
        return Sale.builder()
                .id("venda-001")
                .nomeCliente("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .valorImposto(new BigDecimal("10.00"))
                .valorLiquido(new BigDecimal("90.00"))
                .dataCadastro(LocalDateTime.now())
                .statusProcessamento(status)
                .build();
    }
}
