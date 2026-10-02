package dev.ledgerflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Validated
public class WalletController {
    private final WalletService service;
    public WalletController(WalletService service) { this.service = service; }
    @PostMapping("/wallets") @ResponseStatus(HttpStatus.CREATED)
    public Api.WalletResponse create(@Valid @RequestBody Api.CreateWalletRequest request) { return service.create(request); }
    @GetMapping("/wallets/{id}")
    public Api.WalletResponse get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping("/wallets/{id}/ledger")
    public Api.LedgerPage ledger(@PathVariable UUID id, @RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.ledger(id, page, size); }
    @PostMapping("/transfers")
    public Api.TransferResponse transfer(@Valid @RequestBody Api.TransferRequest request) { return service.transfer(request); }
    @GetMapping("/wallets/{id}/reconciliation")
    public Api.ReconciliationResponse reconcile(@PathVariable UUID id) {
        return service.reconcile(id);
    }
}
