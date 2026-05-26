# ImagesClonerClient

Cliente Java para servidores **Clonezilla HTTP** (`clonezilla_server_scripts/prod-ubuntu`). En cada ejecución (systemd timer):

1. Lista carpetas en `{clonezilla-root}/{images-dir}/`.
2. `POST /images_cloner/client/` con la lista de nombres locales (JSON array).
3. Si la respuesta indica `activarImagen` (imagen `PENDIENTE` en servidor), actualiza `RESTORE_AUTO_*` en `.env`, ejecuta `docker compose up -d --force-recreate` y confirma con `PUT`.
4. Si `menuActivo` es false, vacía `RESTORE_AUTO_*` y recrea el stack (modo menú whiptail).

La orquestación del tick vive en `scheduled_tasks.SendImagesClonezillaDisponiblesTask` (`CommandLineRunner`: un proceso, un tick, salida vía `SpringApplication.exit`). La lógica de `.env` + Docker está en `scheduled_tasks.RestoreAutoConfigurer` (sustituye `scripts/set-restore-auto.sh`).

Depende de [Reaktor_BaseClient](https://github.com/IESJandula/Reaktor_BaseServer/) (mismo parent `Dependencies` que [PrintersClient](https://github.com/IESJandula/Reaktor_PrintersClient)).

## Contrato REST (ImagesClonerServer)

Base URL: `images-cloner.server-url` (puerto **8094** en dev).

| Método | Ruta | Entrada | Respuesta |
|--------|------|---------|-----------|
| POST | `/images_cloner/client/` | `Content-Type: application/json` — cuerpo `List<String>` | `200` + `ConfiguracionClonadorDto` (`menuActivo`, `activarImagen`, `nombreImagen`, `accion`) |
| PUT | `/images_cloner/client/` | Cabecera `nombreImagen` | `200` sin cuerpo |

Todas las peticiones llevan `Authorization: Bearer <token>` vía `AuthorizationService`. Rol JWT: **`CLIENTE_IMAGES_CLONER`**.

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
  http_connection_timeout: 30000
```

`poll-interval-seconds` lo usa el instalador de `prod-ubuntu` para el **systemd timer**, no el JAR.

## Build y pruebas

```bash
mvn clean package
mvn test
```

Artefacto: `target/ImagesClonerClient-*.jar` (renombrar a `images-cloner-client.jar` en el servidor).

## Despliegue en Ubuntu

Tras `sudo bash install.sh` en prod-ubuntu:

- JAR: `/opt/clonezilla-http/images-cloner-client/images-cloner-client.jar`
- Config: `/opt/clonezilla-http/images-cloner-client/application.yaml`
- Timer: `clonezilla-images-cloner.timer`
- Log: `/var/log/clonezilla-http/images-cloner-client.log`

Prueba manual de un tick:

```bash
sudo /opt/clonezilla-http/scripts/run-images-cloner-client.sh
```

Repositorio: `C:\Users\Arduino\git\Reaktor_ImagesClonerClient`
