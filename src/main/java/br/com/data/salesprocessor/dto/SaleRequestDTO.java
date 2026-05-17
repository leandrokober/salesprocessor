package br.com.data.salesprocessor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDTO {
    @NotBlank(message = "O ID da venda é obrigatório para garantir a idempotência.")
    private String id;

    @NotBlank(message = "O nome do cliente é obrigatório.")
    private String clienteNome;

    @NotNull(message = "O valor bruto é obrigatório.")
    @Positive(message = "O valor bruto deve ser maior que zero.")
    private BigDecimal valorBruto;
}
