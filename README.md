# ImagesClonerClient

Cliente Java para servidores **Clonezilla HTTP** (`clonezilla_server_scripts/prod-ubuntu`). En cada ejecución (cron o systemd timer):

1. Lista carpetas en `{clonezilla-root}/{images-dir}/`.
2. `POST /images_cloner/client/` con la lista de nombres locales (JSON array).
3. En la respuesta, procesa imágenes con estado `PENDIENTE` y `accion` informada.
4. Ejecuta `scripts/set-restore-auto.sh <imagen> <accion>` en el host.
5. `PUT /images_cloner/client/` con cabecera `nombreImagen` para marcar la imagen como ACTIVADA en el servidor.

La orquestación del tick vive en `scheduled_tasks.PollTickRunner` (mismo patrón que `printers_client.scheduled_tasks`: HTTP Apache, JWT Bearer vía `AuthorizationService`, logging `POLL_TICK - …`).

Depende de [Reaktor_BaseClient](https://github.com/IESJandula/Reaktor_BaseServer/) (mismo parent `Dependencies` que [PrintersClient](https://github.com/IESJandula/Reaktor_PrintersClient)).

## Migración desde InternalComponents

| Antes | Ahora |
|-------|-------|
| `Reaktor_InternalComponentsClient` | `Reaktor_ImagesClonerClient` |
| Prefijo config `internal-components` | `images-cloner` |
| Rol JWT `CLIENTE_INTERNAL_COMPONENTS` | `CLIENTE_IMAGES_CLONER` |
| Rutas `/opt/.../internal-components-client/` | `/opt/.../images-cloner-client/` |
| Timer `clonezilla-internal-components.timer` | `clonezilla-images-cloner.timer` |

## Contrato REST (ImagesClonerServer)

Base URL configurable: `images-cloner.server-url` (puerto **8094** en dev).

| Método | Ruta | Entrada | Respuesta |
|--------|------|---------|-----------|
| POST | `/images_cloner/client/` | `Content-Type: application/json` — cuerpo `List<String>` | `200` + `List<ImagenClonezilla>` (`nombreImagen`, `estado`, `accion`) |
| PUT | `/images_cloner/client/` | Cabecera `nombreImagen` | `200` sin cuerpo |

Todas las peticiones llevan `Authorization: Bearer <token>` vía `AuthorizationService` de BaseClient. Rol JWT: **`CLIENTE_IMAGES_CLONER`**.

## Arranque del tick

Spring Boot sin servidor web (`web-application-type: none`). `PollTickRunner` implementa `CommandLineRunner`: un proceso, un tick, código de salida vía `SpringApplication.exit` (igual que el despliegue con timer en prod-ubuntu; distinto de PrintersClient, que usa `@Scheduled` en proceso largo).

## Configuración (`application.yaml`)

```yaml
images-cloner:
  server-url: http://localhost:8094
  poll-interval-seconds: 120
  clonezilla-root: /opt/clonezilla-http
  images-dir: images
  http-connection-timeout-ms: 30000
  client-id: CLONEZILLA_HOST

reaktor:
  publicKeyFile: /opt/clonezilla-http/images-cloner-client/public_key.pem
  clientId: CLIENTE_IMAGES_CLONER
```

`poll-interval-seconds` lo usa el instalador de `prod-ubuntu` para el **systemd timer**, no el JAR (cada invocación hace un solo tick y termina).

## Build

```bash
mvn clean package
```

Artefacto: `target/ImagesClonerClient-*.jar` (renombrar a `images-cloner-client.jar` en el servidor).

Repositorio: `C:\Users\Arduino\git\Reaktor_ImagesClonerClient`

## Despliegue en Ubuntu

Tras `sudo bash install.sh` en prod-ubuntu:

- JAR: `/opt/clonezilla-http/images-cloner-client/images-cloner-client.jar`
- Config: `/opt/clonezilla-http/images-cloner-client/application.yaml`
- Timer: `clonezilla-images-cloner.timer`
- Log: `/var/log/clonezilla-http/images-cloner-client.log`

Prueba manual:

```bash
sudo /opt/clonezilla-http/scripts/run-images-cloner-client.sh
```
