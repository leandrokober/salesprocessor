package br.com.data.salesprocessor.exception;

public class SaleRecoveryException extends RuntimeException {

    public SaleRecoveryException(String saleId) {
        super("Erro ao recuperar venda existente. saleId=" + saleId);
    }
}
