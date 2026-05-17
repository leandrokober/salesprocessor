package br.com.data.salesprocessor.exception;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("Acesso negado: X-API-KEY invalida ou ausente");
    }
}
