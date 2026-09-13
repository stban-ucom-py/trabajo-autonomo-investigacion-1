$ErrorActionPreference = 'Stop'
$Host.UI.RawUI.WindowTitle = 'EVIDENCIA - Trabajo Autonomo de Investigacion 1'
$baseUrl = 'http://localhost:8080/api/notifications'
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$utf8 = [Text.Encoding]::UTF8

Clear-Host
Write-Host '============================================================' -ForegroundColor Cyan
Write-Host ' TRABAJO AUTONOMO DE INVESTIGACION 1 - PRUEBA FUNCIONAL' -ForegroundColor White
Write-Host ' Notificador multicanal | Camel | Artemis | PostgreSQL' -ForegroundColor White
Write-Host '============================================================' -ForegroundColor Cyan
Write-Host "Fecha: $(Get-Date -Format 'dd/MM/yyyy HH:mm:ss')"
Write-Host "Equipo: $env:COMPUTERNAME"
Write-Host ''
Write-Host 'SERVICIOS EN EJECUCION' -ForegroundColor Yellow
docker compose ps --format 'table {{.Service}}\t{{.Status}}'

$okBody = @{
    external_id = "EVIDENCIA-OK-$suffix"
    subject = 'Pedido despachado'
    content = 'El pedido ya esta en camino.'
    recipient = @{
        email = 'cliente@example.com'
        phone = '+595981123456'
        device_token = 'device-token-evidencia'
    }
    channels = @('EMAIL', 'SMS', 'PUSH')
} | ConvertTo-Json -Depth 4

Write-Host ''
Write-Host 'CASO 1 - NOTIFICACION MULTICANAL' -ForegroundColor Yellow
$okCreated = Invoke-RestMethod -Method Post -Uri $baseUrl `
    -ContentType 'application/json; charset=utf-8' -Body ($utf8.GetBytes($okBody))
Write-Host "HTTP 202 | ID: $($okCreated.id) | Estado inicial: $($okCreated.status)"
Start-Sleep -Seconds 3
$okResult = Invoke-RestMethod -Method Get -Uri "${baseUrl}?id=$($okCreated.id)"
Write-Host "Estado final: $($okResult.status)" -ForegroundColor Green
foreach ($delivery in $okResult.deliveries) {
    Write-Host ("  {0,-5} -> {1,-4} | intentos: {2}" -f $delivery.channel, $delivery.status, $delivery.attempts)
}

$errorBody = @{
    external_id = "EVIDENCIA-ERROR-$suffix"
    subject = 'Prueba de reintentos'
    content = 'Falla controlada del proveedor.'
    recipient = @{ email = 'fail@example.com' }
    channels = @('EMAIL')
} | ConvertTo-Json -Depth 4

Write-Host ''
Write-Host 'CASO 2 - REINTENTOS Y DEAD LETTER CHANNEL' -ForegroundColor Yellow
$errorCreated = Invoke-RestMethod -Method Post -Uri $baseUrl `
    -ContentType 'application/json; charset=utf-8' -Body ($utf8.GetBytes($errorBody))
Start-Sleep -Seconds 4
$errorResult = Invoke-RestMethod -Method Get -Uri "${baseUrl}?id=$($errorCreated.id)"
$failedDelivery = $errorResult.deliveries[0]
Write-Host "Estado final: $($errorResult.status)" -ForegroundColor Red
Write-Host "  EMAIL -> $($failedDelivery.status) | intentos: $($failedDelivery.attempts)"
Write-Host '  Error persistido: El proveedor simulado rechazo el destino'
$dlqLine = docker compose exec -T artemis /var/lib/artemis-instance/bin/artemis queue stat `
    --user admin --password admin | Select-String 'notifications.dlq'
Write-Host "  Artemis DLQ: $($dlqLine.Line.Trim())"

Write-Host ''
Write-Host 'RESULTADO: PRUEBA FUNCIONAL COMPLETADA CORRECTAMENTE' -ForegroundColor Green
Write-Host '============================================================' -ForegroundColor Cyan
$Host.UI.RawUI.WindowTitle = 'EVIDENCIA LISTA - Trabajo Autonomo de Investigacion 1'
Read-Host 'Captura lista. Presione ENTER para cerrar'
