package br.com.data.salesprocessor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_vendas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sale {

    @Id
    @Column (name = "id_venda")
    private String id;

    @Column (name = "cliente_nome", nullable = false)
    private String nomeCliente;

    @Column (name = "valor_bruto", nullable = false)
    private BigDecimal valorBruto;

    @Column(name = "valor_imposto", nullable = false)
    private BigDecimal valorImposto;

    @Column (name = "valor_liquido", nullable = false)
    private BigDecimal valorLiquido;

    @Column (name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @Column (name = "status_processamento", nullable = false)
    private String statusProcessamento;
}
