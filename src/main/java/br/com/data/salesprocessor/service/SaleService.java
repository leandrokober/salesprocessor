package br.com.data.salesprocessor.service;

import br.com.data.salesprocessor.dto.SaleRequestDTO;
import br.com.data.salesprocessor.exception.SaleRecoveryException;
import br.com.data.salesprocessor.messaging.SalePublisher;
import br.com.data.salesprocessor.model.Sale;
import br.com.data.salesprocessor.repository.SaleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SalePublisher salePublisher;

    public SaleService(SaleRepository saleRepository, SalePublisher salePublisher) {
        this.saleRepository = saleRepository;
        this.salePublisher = salePublisher;
    }

    @Transactional
    public SaleProcessingResult processarVenda(SaleRequestDTO dto) {
        if (saleRepository.existsById(dto.getId())) {
            Sale vendaExistente = saleRepository.findById(dto.getId())
                    .orElseThrow(() -> new SaleRecoveryException(dto.getId()));
            return new SaleProcessingResult(vendaExistente, false);
        }

        BigDecimal taxaImposto = new BigDecimal("0.10");
        BigDecimal valorImposto = dto.getValorBruto().multiply(taxaImposto);
        BigDecimal valorLiquido = dto.getValorBruto().subtract(valorImposto);

        Sale novaVenda = Sale.builder()
                .id(dto.getId())
                .nomeCliente(dto.getClienteNome())
                .valorBruto(dto.getValorBruto())
                .valorImposto(valorImposto)
                .valorLiquido(valorLiquido)
                .dataCadastro(LocalDateTime.now())
                .statusProcessamento("PENDENTE")
                .build();

        Sale vendaSalva = saleRepository.save(novaVenda);
        publicarEventoAposCommit(vendaSalva.getId());

        return new SaleProcessingResult(vendaSalva, true);
    }

    private void publicarEventoAposCommit(String idVenda) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            salePublisher.publicarEventoVenda(idVenda);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                salePublisher.publicarEventoVenda(idVenda);
            }
        });
    }
}
