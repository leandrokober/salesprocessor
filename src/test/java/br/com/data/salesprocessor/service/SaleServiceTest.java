package br.com.data.salesprocessor.service;

import br.com.data.salesprocessor.dto.SaleRequestDTO;
import br.com.data.salesprocessor.exception.SaleRecoveryException;
import br.com.data.salesprocessor.messaging.SalePublisher;
import br.com.data.salesprocessor.model.Sale;
import br.com.data.salesprocessor.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SalePublisher salePublisher;

    @InjectMocks
    private SaleService saleService;

    @Test
    void processarVenda_quandoVendaNaoExiste_deveCalcularValoresSalvarEPublicarEvento() {
        SaleRequestDTO dto = SaleRequestDTO.builder()
                .id("venda-001")
                .clienteNome("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .build();

        when(saleRepository.existsById("venda-001")).thenReturn(false);
        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Sale.class));

        SaleProcessingResult resultado = saleService.processarVenda(dto);

        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(saleCaptor.capture());

        Sale vendaSalva = saleCaptor.getValue();
        assertThat(resultado.created()).isTrue();
        assertThat(resultado.sale()).isSameAs(vendaSalva);
        assertThat(vendaSalva.getId()).isEqualTo("venda-001");
        assertThat(vendaSalva.getNomeCliente()).isEqualTo("Maria Silva");
        assertThat(vendaSalva.getValorBruto()).isEqualByComparingTo("100.00");
        assertThat(vendaSalva.getValorImposto()).isEqualByComparingTo("10.00");
        assertThat(vendaSalva.getValorLiquido()).isEqualByComparingTo("90.00");
        assertThat(vendaSalva.getDataCadastro()).isNotNull();
        assertThat(vendaSalva.getStatusProcessamento()).isEqualTo("PENDENTE");
        verify(saleRepository).existsById("venda-001");
        verify(saleRepository, never()).findById("venda-001");
        verify(salePublisher).publicarEventoVenda("venda-001");
    }

    @Test
    void processarVenda_quandoExisteTransacaoAtiva_devePublicarEventoSomenteAposCommit() {
        SaleRequestDTO dto = SaleRequestDTO.builder()
                .id("venda-001")
                .clienteNome("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .build();
        Sale vendaSalva = vendaExistente();

        when(saleRepository.existsById("venda-001")).thenReturn(false);
        when(saleRepository.save(any(Sale.class))).thenReturn(vendaSalva);

        TransactionSynchronizationManager.initSynchronization();
        try {
            SaleProcessingResult resultado = saleService.processarVenda(dto);

            assertThat(resultado.created()).isTrue();
            verify(salePublisher, never()).publicarEventoVenda("venda-001");

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(salePublisher).publicarEventoVenda("venda-001");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void processarVenda_quandoVendaJaExiste_deveRetornarVendaExistenteSemPublicarNovoEvento() {
        SaleRequestDTO dto = SaleRequestDTO.builder()
                .id("venda-001")
                .clienteNome("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .build();
        Sale vendaExistente = vendaExistente();

        when(saleRepository.existsById("venda-001")).thenReturn(true);
        when(saleRepository.findById("venda-001")).thenReturn(Optional.of(vendaExistente));

        SaleProcessingResult resultado = saleService.processarVenda(dto);

        assertThat(resultado.created()).isFalse();
        assertThat(resultado.sale()).isSameAs(vendaExistente);
        assertThat(resultado.sale().getValorImposto()).isEqualByComparingTo("10.00");
        assertThat(resultado.sale().getValorLiquido()).isEqualByComparingTo("90.00");
        verify(saleRepository).existsById("venda-001");
        verify(saleRepository).findById("venda-001");
        verify(saleRepository, never()).save(any(Sale.class));
        verify(salePublisher, never()).publicarEventoVenda("venda-001");
    }

    @Test
    void processarVenda_quandoMesmoIdForProcessadoDuasVezes_deveSalvarApenasNaPrimeira() {
        SaleRequestDTO dto = SaleRequestDTO.builder()
                .id("venda-001")
                .clienteNome("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .build();
        Sale vendaExistente = vendaExistente();

        when(saleRepository.existsById("venda-001")).thenReturn(false, true);
        when(saleRepository.save(any(Sale.class))).thenReturn(vendaExistente);
        when(saleRepository.findById("venda-001")).thenReturn(Optional.of(vendaExistente));

        SaleProcessingResult primeiraChamada = saleService.processarVenda(dto);
        SaleProcessingResult segundaChamada = saleService.processarVenda(dto);

        assertThat(primeiraChamada.created()).isTrue();
        assertThat(primeiraChamada.sale()).isSameAs(vendaExistente);
        assertThat(segundaChamada.created()).isFalse();
        assertThat(segundaChamada.sale()).isSameAs(vendaExistente);
        verify(saleRepository, times(2)).existsById("venda-001");
        verify(saleRepository, times(1)).save(any(Sale.class));
        verify(saleRepository, times(1)).findById("venda-001");
        verify(salePublisher, times(1)).publicarEventoVenda("venda-001");
    }

    @Test
    void processarVenda_quandoVendaExistenteNaoForEncontrada_deveLancarExcecao() {
        SaleRequestDTO dto = SaleRequestDTO.builder()
                .id("venda-001")
                .clienteNome("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .build();

        when(saleRepository.existsById("venda-001")).thenReturn(true);
        when(saleRepository.findById("venda-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.processarVenda(dto))
                .isInstanceOf(SaleRecoveryException.class)
                .hasMessage("Erro ao recuperar venda existente. saleId=venda-001");

        verify(saleRepository, never()).save(any(Sale.class));
        verify(salePublisher, never()).publicarEventoVenda("venda-001");
    }

    private Sale vendaExistente() {
        return Sale.builder()
                .id("venda-001")
                .nomeCliente("Maria Silva")
                .valorBruto(new BigDecimal("100.00"))
                .valorImposto(new BigDecimal("10.00"))
                .valorLiquido(new BigDecimal("90.00"))
                .dataCadastro(LocalDateTime.now())
                .statusProcessamento("PENDENTE")
                .build();
    }
}
