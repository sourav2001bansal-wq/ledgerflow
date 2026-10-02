$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8080/api'
function PostJson($path, $body) {
    Invoke-RestMethod -Method Post -Uri "$base/$path" -ContentType 'application/json' -Body ($body | ConvertTo-Json)
}
$a = PostJson 'wallets' @{ owner = 'Sourav'; initialBalance = 1000 }
$b = PostJson 'wallets' @{ owner = 'Friend'; initialBalance = 200 }
$request = @{ requestId = [guid]::NewGuid().ToString(); fromWalletId = $a.id; toWalletId = $b.id; amount = 500 }
$transfer = PostJson 'transfers' $request
$replay = PostJson 'transfers' $request
if ($transfer.id -ne $replay.id) { throw 'Replay returned a different transfer' }
$source = Invoke-RestMethod "$base/wallets/$($a.id)"
$target = Invoke-RestMethod "$base/wallets/$($b.id)"
if ($source.balance -ne 500 -or $target.balance -ne 700) { throw 'Unexpected wallet balances' }
Write-Host 'PASS: source 500 INR; destination 700 INR; replay did not debit again.'
$source
$target
Invoke-RestMethod "$base/wallets/$($a.id)/ledger" | ConvertTo-Json -Depth 6
