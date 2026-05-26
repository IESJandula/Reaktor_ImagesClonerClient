package es.iesjandula.reaktor.images_cloner_client.utils;

/**
 * Clase de constantes para el cliente de Clonezilla.
 */
public final class Constants
{
	/** Constante - Error - Error al enviar las imágenes disponibles al servidor */
	public static final int ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE    = 9401 ;

	/** Constante - Error - Error al activar imagen */
	public static final int ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE = 9402 ;

	/** Constante - Error - Error al confirmar imagen activada */
	public static final int ERR_ERROR_AL_CONFIRMAR_IMAGEN_CODE = 9403 ;

	/************************************************/
	/************** Acción - Poweroff ***************/
	/************************************************/

	/** Constante - Acción - Poweroff */
	public static final String ACCION_POWEROFF = "poweroff" ;
	/** Constante - Acción - Reboot */
	public static final String ACCION_REBOOT   = "reboot" ;

	/************************************************/
	/************** Estados de la imagen ************/
	/************************************************/

	/** Constante - Estado - Pendiente */
	public static final String ESTADO_PENDIENTE   = "PENDIENTE" ;
	/** Constante - Estado - Activada */
	public static final String ESTADO_ACTIVADA    = "ACTIVADA" ;
	/** Constante - Estado - Desactivada */
	public static final String ESTADO_DESACTIVADA = "DESACTIVADA" ;


	/************************************************/
	/************** Comandos de Docker **************/
	/************************************************/

	/** Constante - Comando - docker compose up -d --force-recreate */
	public static final String COMANDO_DOCKER_COMPOSE_UP_D_FORCE_RECREATE = "docker compose up -d --force-recreate" ;

	/** Constante - Timeout - Timeout de la ejecución de docker compose */
	public static final int COMPOSE_TIMEOUT_MINUTES = 10 ;

	/************************************************/
	/************** Variables de entorno *************/
	/************************************************/

	/** Constante - Variable de entorno - MODO_AUTOMATICO_IMAGEN_ACTIVA_NOMBRE */
	public static final String MODO_AUTOMATICO_IMAGEN_ACTIVA_NOMBRE = "MODO_AUTOMATICO_IMAGEN_ACTIVA_NOMBRE" ;
	
	/** Constante - Variable de entorno - MODO_AUTOMATICO_IMAGEN_ACTIVA_ACCION */
	public static final String MODO_AUTOMATICO_IMAGEN_ACTIVA_ACCION = "MODO_AUTOMATICO_IMAGEN_ACTIVA_ACCION" ;
}