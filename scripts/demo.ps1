$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080/api/notifications'
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$utf8 = [Text.Encoding]::UTF8

Write-Host '1. Enviando notificación multicanal válida...'
$ok = @{
    external_id = "ORDER-$suffix"
    subject = 'Tu pedido fue despachado'
    content = 'El pedido ya está en camino.'
    recipient = @{ email='cliente@example.com'; phone='+595981123456'; device_token='device-token-abc123' }
    channels = @('EMAIL', 'SMS', 'PUSH')
} | ConvertTo-Json -Depth 4
$response = Invoke-RestMethod -Method Post -Uri $baseUrl -ContentType 'application/json; charset=utf-8' `
    -Body ($utf8.GetBytes($ok))
$response | ConvertTo-Json

Start-Sleep -Seconds 3
Write-Host "`n2. Consultando resultado y entregas..."
Invoke-RestMethod -Method Get -Uri "${baseUrl}?id=$($response.id)" | ConvertTo-Json -Depth 5

Write-Host "`n3. Enviando un mensaje que falla para demostrar reintentos y DLQ..."
$errorCase = @{
    external_id = "ERROR-$suffix"
    subject = 'Prueba de reintentos'
    content = 'Este caso simula el rechazo del proveedor.'
    recipient = @{ email='fail@example.com' }
    channels = @('EMAIL')
} | ConvertTo-Json -Depth 4
$failed = Invoke-RestMethod -Method Post -Uri $baseUrl -ContentType 'application/json; charset=utf-8' `
    -Body ($utf8.GetBytes($errorCase))
Start-Sleep -Seconds 3
Invoke-RestMethod -Method Get -Uri "${baseUrl}?id=$($failed.id)" | ConvertTo-Json -Depth 5
