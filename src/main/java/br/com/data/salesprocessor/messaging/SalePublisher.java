package br.com.data.salesprocessor.messaging;

import br.com.data.salesprocessor.config.SaleQueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SalePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public SalePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarEventoVenda(String idVenda) {
        LOGGER.info(
                "Publishing sale processing event. saleId={}, exchange={}, routingKey={}",
                idVenda,
                SaleQueueConfig.EXCHANGE_VENDAS,
                SaleQueueConfig.ROUTING_KEY_VENDAS
        );

        rabbitTemplate.convertAndSend(
                SaleQueueConfig.EXCHANGE_VENDAS,
                SaleQueueConfig.ROUTING_KEY_VENDAS,
                idVenda
        );

        LOGGER.info("Sale processing event published. saleId={}", idVenda);
    }
}
