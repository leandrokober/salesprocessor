package br.com.data.salesprocessor.exception;

public class ReceiptGenerationException extends RuntimeException {

    public ReceiptGenerationException(String saleId, Throwable cause) {
        super("Erro ao gerar comprovante PDF da venda " + saleId, cause);
    }
}
