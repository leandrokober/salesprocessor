package br.com.data.salesprocessor.service;

import br.com.data.salesprocessor.model.Sale;

public record SaleProcessingResult(Sale sale, boolean created) {
}
