"""End-to-end demo. Start the application first. Uses Python standard library only."""
import json, urllib.request, uuid
BASE = 'http://127.0.0.1:8080/api'
def call(path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(BASE + path, data=data, headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.load(response)
a = call('/wallets', {'owner': 'Sourav', 'initialBalance': 1000})
b = call('/wallets', {'owner': 'Friend', 'initialBalance': 200})
payload = {'requestId': str(uuid.uuid4()), 'fromWalletId': a['id'], 'toWalletId': b['id'], 'amount': 500}
first = call('/transfers', payload)
assert first == call('/transfers', payload), 'Duplicate transfer changed response'
assert call('/wallets/' + a['id'])['balance'] == 500
assert call('/wallets/' + b['id'])['balance'] == 700
print('PASS: transferred 500 INR, balances 500/700, duplicate request did not debit twice.')
print(json.dumps(call('/wallets/' + a['id'] + '/ledger'), indent=2))
