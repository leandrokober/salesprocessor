package br.com.data.salesprocessor.messaging;

import br.com.data.salesprocessor.config.SaleQueueConfig;
import br.com.data.salesprocessor.model.Sale;
import br.com.data.salesprocessor.repository.SaleRepository;
import br.com.data.salesprocessor.service.PdfReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Component
public class SaleConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaleConsumer.class);

    private final SaleRepository saleRepository;
    private final PdfReceiptService pdfReceiptService;

    public SaleConsumer(SaleRepository saleRepository, PdfReceiptService pdfReceiptService) {
        this.saleRepository = saleRepository;
        this.pdfReceiptService = pdfReceiptService;
    }

    @RabbitListener(queues = SaleQueueConfig.QUEUE_VENDAS)
    public void receberEventoVenda(String idVenda) {
        LOGGER.info("Sale processing message received. saleId={}, queue={}", idVenda, SaleQueueConfig.QUEUE_VENDAS);

        // 1. BUSCA A VENDA NO BANCO
        Optional<Sale> saleOptional = saleRepository.findById(idVenda);

        if (saleOptional.isEmpty()) {
            LOGGER.warn("Sale not found. Processing cancelled. saleId={}", idVenda);
            return;
        }

        Sale venda = saleOptional.get();

        // 2. IDEMPOTENCIA NO CONSUMER: Verifica se o PDF ja foi gerado (Status PROCESSADA)
        if ("PROCESSADA".equals(venda.getStatusProcessamento())) {
            LOGGER.info("Duplicate sale processing message ignored. saleId={}, status={}", idVenda, venda.getStatusProcessamento());
            return;
            // O Spring AMQP faz o Acknowledge automatico, removendo a mensagem da fila.
        }

        try {
            // 3. GERACAO DO PDF (Regra de negocio pesada)
            LOGGER.info("Generating sale receipt PDF. saleId={}, customerName={}", venda.getId(), venda.getNomeCliente());
            Path comprovante = pdfReceiptService.gerarComprovante(venda);
            LOGGER.info("Sale receipt PDF generated. saleId={}, filePath={}", venda.getId(), comprovante.toAbsolutePath());

            // 4. ATUALIZACAO DE STATUS
            venda.setStatusProcessamento("PROCESSADA");
            saleRepository.save(venda);

            LOGGER.info("Sale processing completed. saleId={}, status={}", venda.getId(), venda.getStatusProcessamento());
        } catch (RuntimeException e) {
            LOGGER.error("Sale processing failed. saleId={}", idVenda, e);
            throw e;
        }
    }
}
