# Verificación de los requisitos de entrega

Esta tabla relaciona cada requisito de la sección 2.1 con su evidencia concreta dentro del proyecto.

| Requisito | Estado | Evidencia |
|---|---|---|
| Código fuente completo | Cumplido localmente | Directorios `src/main/java` y `src/test/java`. Pendiente publicar esta carpeta en un repositorio de GitHub. |
| `build.gradle` y configuración Gradle | Cumplido | `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat` y `gradle/wrapper`. |
| Ejecución con Java o Docker | Cumplido | `Dockerfile`, `docker-compose.yml` e instrucciones del `README.md`. |
| Configuración de Apache Camel | Cumplido | `NotificationApplication.java` y `NotificationRoutes.java`, con seis rutas nombradas. |
| Configuración de ActiveMQ Artemis | Cumplido | Servicio `artemis` y credenciales en `docker-compose.yml`; colas JMS declaradas en las rutas Camel. |
| Preparación de PostgreSQL | Cumplido | `sql/init.sql`, volumen persistente y servicio `postgres`. |
| README de instalación, configuración y ejecución | Cumplido | `README.md`, probado desde un entorno basado en contenedores. |
| Diagrama de arquitectura | Cumplido | Diagrama Mermaid en `docs/arquitectura.md`. |
| Descripción del flujo principal | Cumplido | Sección «Secuencia principal» de `docs/arquitectura.md`. |
| Identificación y justificación de patrones EIP | Cumplido | Tabla «Patrones EIP y justificación» de `docs/arquitectura.md`. |
| Evidencias de funcionamiento | Cumplido | `evidencias/resultado-prueba-funcional.txt`: compilación, ocho pruebas, flujo exitoso, reintentos y DLQ. |
| Mensaje de entrada | Cumplido | `examples/notification-ok.json` y `examples/notification-error.json`. |
| Transformación de mensajes | Cumplido | `examples/transformation-email.json`, `transformation-sms.json` y `transformation-push.json`. |
| Evidencia de enrutamiento | Cumplido | `examples/routing-example.json` y colas descritas en `docs/arquitectura.md`. |
| Mensajes de salida | Cumplido | `examples/output-completed.json` y `examples/output-failed.json`. |
| Ejecución en entorno limpio | Cumplido | Verificada con `docker compose up --build -d`; no necesita Java, Gradle, Artemis ni PostgreSQL instalados localmente. |

## Único paso externo pendiente

La cátedra exige una URL de GitHub. El contenido del proyecto está listo, pero debe crearse un repositorio
en la cuenta del estudiante y publicarse allí. Después de publicarlo, conviene colocar la URL al inicio del
`README.md` y presentar esa misma URL junto con el archivo ZIP.
