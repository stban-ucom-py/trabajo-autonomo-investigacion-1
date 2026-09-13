# Trabajo Autónomo de Investigación 1 (P1)

## Notificador de mensajes multicanal con Apache Camel y Artemis

**Alumno:** Esteban Gavilan  
**Materia:** Integración de Sistemas  
**Tema:** Implementación de patrones de integración empresarial en una arquitectura moderna basada en mensajería

## Problema elegido

Una empresa necesita enviar avisos generados por sus sistemas de negocio mediante correo electrónico,
SMS y notificaciones push. Cada proveedor utiliza un contrato diferente y puede fallar de forma
independiente. El sistema productor no debe esperar el envío ni conocer los detalles de cada proveedor.

La solución recibe un modelo canónico por HTTP, valida y persiste la solicitud, responde inmediatamente
con HTTP 202 y delega el procesamiento a colas durables de ActiveMQ Artemis. Apache Camel divide,
enruta y transforma los mensajes. PostgreSQL conserva el estado y permite consultar el resultado.

## Stack utilizado

- Java 21.
- Apache Camel 4.18.4.
- Apache ActiveMQ Artemis 2.40.0 mediante JMS.
- Gradle 9.6.0.
- PostgreSQL 17.
- Docker y Docker Compose para una ejecución reproducible.

## Requisitos

La forma recomendada de ejecutar la entrega solo requiere **Docker Desktop**. Java y Gradle se
descargan dentro de las imágenes, por lo que no hace falta instalarlos en la computadora.

## Inicio rápido

Desde esta carpeta:

```powershell
docker compose up --build -d
```

Espere hasta que los tres servicios aparezcan activos:

```powershell
docker compose ps
```

La API queda en `http://localhost:8080`. La consola de Artemis queda en
`http://localhost:8161` con usuario y contraseña `admin`.

PostgreSQL se publica en `localhost:5433` para evitar conflictos con instalaciones locales; dentro de
Docker conserva su puerto normal `5432`.

## Demostración

El siguiente comando ejecuta un caso exitoso y otro que demuestra los reintentos y la DLQ:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\demo.ps1
```

También puede crear una notificación manualmente:

```powershell
$respuesta = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/notifications' `
  -ContentType 'application/json; charset=utf-8' `
  -InFile '.\examples\notification-ok.json'

$respuesta | ConvertTo-Json
```

La respuesta esperada es HTTP 202:

```json
{
  "id": "946cf550-f7b2-4d41-b5c8-bbf0a5391bf5",
  "external_id": "ORDER-2026-0001",
  "status": "QUEUED",
  "message": "Notificación aceptada para procesamiento"
}
```

Luego consulte el resultado usando el `id` recibido:

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/notifications?id=$($respuesta.id)" |
  ConvertTo-Json -Depth 6
```

## Contrato de entrada

| Campo | Obligatorio | Descripción |
|---|---:|---|
| `external_id` | Sí | Identificador idempotente aportado por el sistema origen. |
| `subject` | Sí | Título de la notificación. |
| `content` | Sí | Contenido del mensaje. |
| `recipient` | Sí | Destinos disponibles del destinatario. |
| `channels` | Sí | Uno o más valores: `EMAIL`, `SMS`, `PUSH`. |

El destino correspondiente a cada canal solicitado también es obligatorio: `email`, `phone` o
`device_token`. La restricción única de `external_id` impide procesar dos veces la misma solicitud.

## Rutas de Camel

| Ruta | Responsabilidad |
|---|---|
| `http-notification-intake` | Recibir, validar, persistir y publicar la solicitud. |
| `http-notification-status` | Consultar estado y entregas por UUID. |
| `split-notification-by-channel` | Dividir y enrutar un mensaje por cada canal. |
| `email-provider-adapter` | Traducir y enviar el contrato de correo. |
| `sms-provider-adapter` | Traducir, limitar a 160 caracteres y enviar el SMS. |
| `push-provider-adapter` | Traducir y enviar el contrato push. |

## Manejo de errores

- Contrato inválido: HTTP 400 y estado `REJECTED`.
- `external_id` duplicado: HTTP 400 sin publicar otro mensaje.
- ID de consulta inválido: HTTP 400.
- Notificación inexistente: HTTP 404.
- Proveedor no disponible: tres intentos totales, entrega `FAILED`, solicitud `PARTIAL_FAILURE` y
  publicación en `notifications.dlq`.
- Error interno no previsto: HTTP 500 sin revelar detalles técnicos.

Para simular el fallo de un proveedor se utiliza un destino que contenga la palabra `fail`, como el
ejemplo de `examples/notification-error.json`.

## Persistencia

El archivo [sql/init.sql](sql/init.sql) crea:

- `notifications`: solicitud, estado global e identificador externo único.
- `deliveries`: una entrega por canal, destino, intentos, ID del proveedor y error.

Los datos se conservan en el volumen `postgres-data`. Para reiniciar la demostración desde cero:

```powershell
docker compose down -v
```

Este comando elimina el volumen y sus datos; para detener sin borrar información use solamente
`docker compose down`.

## Pruebas

La construcción ejecuta automáticamente ocho pruebas con JUnit 5:

```powershell
docker compose build app
```

Si dispone localmente de Java 21, el wrapper incluido descarga la versión correcta de Gradle:

```powershell
.\gradlew.bat clean test
```

Las pruebas cubren validación correcta, campos obligatorios, lista y duplicación de canales, Splitter,
correlación, transformación de SMS, seguridad básica del HTML y fallo del proveedor. La evidencia de la
prueba funcional real se encuentra en `evidencias/resultado-prueba-funcional.txt`.

## Diseño y patrones EIP

El diagrama completo, el flujo, los canales y la justificación de cada patrón están en
[docs/arquitectura.md](docs/arquitectura.md). Se aplican Message Channel, Point-to-Point Channel,
Canonical Data Model, Validator, Splitter, Content-Based Router, Message Translator, Correlation
Identifier, Idempotent Receiver, Message Store y Dead Letter Channel.

Referencia conceptual: Gregor Hohpe y Bobby Woolf, *Enterprise Integration Patterns* (2003), y el
[catálogo público de EIP](https://www.enterpriseintegrationpatterns.com/).

## Estructura de la entrega

La correspondencia punto por punto con la sección 2.1 de la consigna está documentada en
[docs/checklist-entrega.md](docs/checklist-entrega.md).

```text
.
├── build.gradle                 Configuración Java 21, Camel y pruebas
├── docker-compose.yml           Aplicación, Artemis y PostgreSQL
├── Dockerfile                   Construcción reproducible con Gradle 9.6
├── gradlew / gradlew.bat        Wrapper oficial de Gradle 9.6
├── docs/arquitectura.md         Diagrama, flujo y patrones EIP
├── evidencias/                  Resultados verificados
├── examples/                    Mensajes de entrada
├── scripts/demo.ps1             Demostración repetible
├── sql/init.sql                 Esquema PostgreSQL
└── src/                         Código fuente y pruebas
```
