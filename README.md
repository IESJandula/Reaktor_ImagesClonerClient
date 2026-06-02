# ImagesClonerClient

Cliente Java para servidores **Clonezilla HTTP**. Sincroniza las carpetas de imágenes locales con **ImagesClonerServer** y aplica la configuración de restauración automática en el host Clonezilla.

En cada ejecución programada (`GestorDeImagenesClonezillaDisponiblesTask`):

1. Lista carpetas en `{clonezilla-root}/{images-dir}/`.
2. `POST /images_cloner/client/` con la lista de nombres locales (JSON array).
3. Si la respuesta indica `activarImagen` (imagen `PENDIENTE` en servidor), actualiza `MODO_AUTOMATICO_IMAGEN_ACTIVA_*` en `.env`, ejecuta `docker compose up -d --force-recreate` y confirma con `PUT`.
4. Si `menuActivo` es true, vacía esas variables y recrea el stack (modo menú whiptail).

Depende de [Reaktor_BaseClient](https://github.com/IESJandula/Reaktor_BaseClient) (mismo parent `Dependencies` que [PrintersClient](https://github.com/IESJandula/Reaktor_PrintersClient)).

## Contrato REST (ImagesClonerServer)

Base URL: `reaktor.images_cloner_server_url` (API central en producción).

| Método | Ruta | Entrada | Respuesta |
|--------|------|---------|-----------|
| POST | `/images_cloner/client/` | `Content-Type: application/json` — cuerpo `List<String>` | `200` + `ConfiguracionClonadorDto` (`menuActivo`, `activarImagen`, `nombreImagen`, `accion`) |
| PUT | `/images_cloner/client/{nombreImagen}` | — | `200` sin cuerpo |

Todas las peticiones llevan `Authorization: Bearer <token>` vía `AuthorizationService`. Rol JWT: **`CLIENTE_IMAGES_CLONER`**.

## Ejecución programada (dentro del JAR)

El polling **no** lo marca systemd: lo hace Spring con `@Scheduled` en horario lectivo (L–V):

| Franja | Cron |
|--------|------|
| 07:45–07:59 | `0 45-59/5 7 * * MON-FRI` |
| 08:00–19:55 | `0 */5 8-19 * * MON-FRI` |
| 20:00–20:30 | `0 0-30/5 20 * * MON-FRI` |

Requisitos en código:

- `@EnableScheduling` en `ReaktorImagesClonerClientApplication`.
- `SpringApplication.run(...)` **sin** `System.exit` (mismo patrón que PrintersClient: proceso JVM permanente).

## Configuración

### Desarrollo (`application.yaml`)

```yaml
spring:
  main:
    web-application-type: none

reaktor:
  publicKeyFile: /opt/clonezilla-http/images-cloner-client/public_key.pem
  clientId: CLIENTE_IMAGES_CLONER
  images_cloner_server_url: http://localhost:8094/
  http_connection_timeout: 30000
  clonezilla:
    root: /opt/clonezilla-http
    images:
      dir: images
```

### Producción VPS (`application-VPS.yaml`, perfil `VPS`)

Perfil activado por `script.sh` del despliegue estándar Reaktor (`--spring.profiles.active=VPS`).

Secretos inyectados en build CI vía `application-VPS-filter.properties`: `PUBLIC_KEY_FILE`, `CLIENT_ID`.

Log en servidor: `/tmp/reaktor_imagesClonerClient.log`

## Build y pruebas

```bash
mvn clean package
mvn test
```

Artefacto: `target/ImagesClonerClient-*-jar-with-dependencies.jar`

## Despliegue en VPS (GitHub runner)

Patrón generado por [VPS_GeneratorScripts](https://github.com/IESJandula/VPS_GeneratorScripts) (igual que otros clientes Reaktor).

Ruta típica: `/home/github_runners/automatizaciones/Reaktor_ImagesClonerClient/`

### Unidades systemd

| Unidad | Función |
|--------|---------|
| `reaktor_imagesclonerclient.service` | Ejecuta `script.sh`: lanza el JAR si no hay proceso Java en ejecución |
| `reaktor_imagesclonerclient.timer` | Watchdog: dispara el servicio cada **5 s** (`OnUnitActiveSec=5s`) |

Contenido esperado (resumen):

```ini
# reaktor_imagesclonerclient.service
[Service]
User=root
WorkingDirectory=/home/github_runners/automatizaciones/Reaktor_ImagesClonerClient
ExecStart=/bin/bash .../script.sh
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

# reaktor_imagesclonerclient.timer
[Timer]
OnUnitActiveSec=5s
Unit=reaktor_imagesclonerclient.service
```

**Importante:** el timer de 5 s **no es el intervalo de polling** al servidor. Solo comprueba que el JVM siga vivo y lo relanza si murió. El intervalo real de sincronización son los cron de `@Scheduled` (cada 5 min en horario lectivo).

`script.sh` (plantilla VPS):

```bash
if ! pgrep -f "$JAR_NAME" > /dev/null; then
    nohup java -jar "$JAR_PATH/$JAR_NAME" --spring.profiles.active=VPS
fi
```

### Comprobaciones en el servidor

```bash
# Estado del watchdog (no confundir con el cron interno)
systemctl status reaktor_imagesclonerclient.service
systemctl status reaktor_imagesclonerclient.timer

# ¿Un solo JVM?
pgrep -af ImagesClonerClient

# Log de la aplicación
tail -f /tmp/reaktor_imagesClonerClient.log
```

### Si el servicio “reinicia” continuamente

1. **`System.exit` en el `main`** — incompatible con microservicio; el JVM debe quedarse bloqueado en `SpringApplication.run`.
2. **Fallo al arrancar** — revisar el log (`Exception` al inicio). Con `Restart=on-failure` y el timer de 5 s el ciclo parece un reinicio constante.
3. **`pgrep -f "$JAR_NAME"` no coincide** con el nombre real del JAR → el timer puede lanzar procesos duplicados hasta agotar memoria (`-Xmx256m`).
4. **`JarUpdateService`** (Reaktor_Base) — si el JAR se redeploya en caliente, hace `System.exit(0)` al detectar cambio de timestamp.
5. **Recreación Docker** — si el servidor devuelve `activarImagen`/`menuActivo` en cada tick, `docker compose up -d --force-recreate` se ejecuta en cada poll (reinicio del stack Clonezilla, no del unit systemd).

## Despliegue alternativo: Clonezilla prod-ubuntu

En `clonezilla_server_scripts/prod-ubuntu` existe otro modelo (**tick único** + timer cada `poll-interval-seconds`, p. ej. 120 s):

- JAR: `/opt/clonezilla-http/images-cloner-client/images-cloner-client.jar`
- Timer: `clonezilla-images-cloner.timer`
- Script: `/opt/clonezilla-http/scripts/run-images-cloner-client.sh`

Ese flujo está pensado para un proceso que arranca, ejecuta una vez y termina. **No** mezclar con el despliegue VPS de microservicio permanente.

## Prueba manual

En el VPS, tras `mvn package`:

```bash
java -jar target/ImagesClonerClient-*-jar-with-dependencies.jar --spring.profiles.active=VPS
```

Debe permanecer en ejecución y registrar `IMAGES_CLONER_CLIENT - INICIO` en los minutos definidos por el cron.
