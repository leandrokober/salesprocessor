package br.com.data.salesprocessor.controller;

import br.com.data.salesprocessor.dto.SaleRequestDTO;
import br.com.data.salesprocessor.exception.InvalidApiKeyException;
import br.com.data.salesprocessor.model.Sale;
import br.com.data.salesprocessor.service.SaleProcessingResult;
import br.com.data.salesprocessor.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendas")
@Tag(name = "Vendas", description = "Endpoints para processamento de vendas")
public class SaleController {

    private static final String API_KEY_MOCKADA = "sales-token-secreto-123";

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @Operation(
            summary = "Cria e processa uma venda",
            description = "Recebe os dados de uma venda, aplica as regras de processamento e publica o evento para processamento assincrono."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Venda criada e enviada para processamento",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Sale.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "Venda ja existente retornada por idempotencia",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Sale.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados da venda invalidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "API Key invalida ou ausente",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class)
                    )
            )
    })
    public ResponseEntity<?> criarVenda(
            @Parameter(
                    description = "Chave de autenticacao da API",
                    example = "sales-token-secreto-123"
            )
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da venda que sera processada",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SaleRequestDTO.class)
                    )
            )
            @Valid @RequestBody SaleRequestDTO dto) {

        if (apiKey == null || !apiKey.equals(API_KEY_MOCKADA)) {
            throw new InvalidApiKeyException();
        }

        SaleProcessingResult resultado = saleService.processarVenda(dto);
        HttpStatus status = resultado.created() ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status).body(resultado.sale());
    }
}
