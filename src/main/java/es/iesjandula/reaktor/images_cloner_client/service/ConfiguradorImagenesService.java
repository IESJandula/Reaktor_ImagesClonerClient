package es.iesjandula.reaktor.images_cloner_client.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import es.iesjandula.reaktor.images_cloner_client.utils.Constants;
import es.iesjandula.reaktor.images_cloner_client.utils.ImagesClonerClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sustituye {@code scripts/set-restore-auto.sh}: actualiza RESTORE_AUTO_* en .env y recrea el stack Docker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfiguradorImagenesService
{
	@Value("${reaktor.clonezilla.root}")
	private String clonezillaRoot ;

	@Value("${reaktor.clonezilla.images.dir}")
	private String imagesDir ;

	/**
	 * Activa el modo menú.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	public void activarModoMenu() throws ImagesClonerClientException
	{
		// Logueamos el mensaje
		log.info("Activando el modo menú");

		// Activamos la imagen
		this.activarImagen(null, null);
	}

	/**
	 * Activa una imagen.
	 * @param nombreImagen - Nombre de la imagen.
	 * @param accion - Acción a realizar.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	public void activarImagen(String nombreImagen, String accion) throws ImagesClonerClientException
	{
		// Creamos el path del clonezilla root
		Path clonezillaRootPath = Path.of(this.clonezillaRoot).normalize();

		// Validamos el path de la imagen
		this.validarPathImagen(nombreImagen, clonezillaRootPath);

        // Obtenemos el path del archivo de variable de entorno
		Path variableEntornoFilePath = this.obtenerPathVariableEntorno(clonezillaRootPath);

		// Actualizamos las variables de entorno
		this.actualizarVariableEntorno(variableEntornoFilePath, Constants.MODO_AUTOMATICO_IMAGEN_ACTIVA_NOMBRE, nombreImagen);
		this.actualizarVariableEntorno(variableEntornoFilePath, Constants.MODO_AUTOMATICO_IMAGEN_ACTIVA_ACCION, accion);

		// Recreamos el contenedor Docker
		this.recrearContenedorDocker(clonezillaRootPath);
	}

	/**
	 * Obtiene el path de la imagen.
	 * @param nombreImagen - Nombre de la imagen.
	 * @param clonezillaRootPath - Path del root de clonezilla.
	 * @return Path de la imagen.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private void validarPathImagen(String nombreImagen, Path clonezillaRootPath) throws ImagesClonerClientException
	{
		// Creamos el path de la imagen
		Path imagenPath = clonezillaRootPath.resolve(this.imagesDir).resolve(nombreImagen).normalize();

		// Validamos si la imagen existe
		if (!Files.isDirectory(imagenPath))
		{
			// Creamos un mensaje de error
			String mensajeError = "No existe la carpeta " + this.imagesDir + "/" + nombreImagen + "/ (esperada en " + imagenPath + ")";

			// Logueamos el mensaje de error
			log.error(mensajeError);

			// Devolvemos la excepción de entrada/salida
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE, mensajeError);
		}
	}

	/**
	 * Obtiene el path del archivo de variable de entorno.
	 * @param clonezillaRootPath - Path del root de clonezilla.
	 * @return Path del archivo de variable de entorno.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private Path obtenerPathVariableEntorno(Path clonezillaRootPath) throws ImagesClonerClientException
	{
		// Validamos si existe el archivo .env
		Path variableEntornoFilePath = clonezillaRootPath.resolve(".env");
		if (!Files.isRegularFile(variableEntornoFilePath))
		{
			// Creamos un mensaje de error
			String mensajeError = "No existe el archivo de variable de entorno";

			// Logueamos el mensaje de error
			log.error(mensajeError);

			// Devolvemos la excepción de entrada/salida
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE, mensajeError);
		}

		// Devolvemos el path del archivo de variable de entorno
		return variableEntornoFilePath;
	}

	/**
	 * Actualiza una variable de entorno.
	 * @param variableEntornoFilePath - Path del archivo de variable de entorno.
	 * @param key - Clave de la variable.
	 * @param value - Valor de la variable.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private void actualizarVariableEntorno(Path variableEntornoFilePath, String key, String value) throws ImagesClonerClientException
	{
		try
		{

			// Creamos el patrón de la clave
			Pattern expresionRegular = Pattern.compile("^[\\s]*#?[\\s]*" + Pattern.quote(key) + "=");
	
			// Leemos las líneas del archivo
			List<String> lineasVariableEntorno = Files.readAllLines(variableEntornoFilePath, StandardCharsets.UTF_8);
	
			// Creamos la lista de salida
			List<String> lineasVariableEntornoOutput = new ArrayList<String>();
	
			// Variable para controlar si se ha reemplazado la clave
			boolean replaced = false;
	
			// Recorremos las líneas del archivo
			for (String variableEntornoLinea : lineasVariableEntorno)
			{
				// Si ya existe la clave ...
				if (expresionRegular.matcher(variableEntornoLinea).find())
				{
					// Reemplazamos el valor, añadiéndolo al fichero de variable de entorno de salida nuevo
					lineasVariableEntornoOutput.add(key + "=" + value);
	
					// Activamos el flag de reemplazado
					replaced = true;
				}
				else
				{
					// Si la clave valor no coincide, la añadimos al fichero de variable de entorno de salida nuevo
					lineasVariableEntornoOutput.add(variableEntornoLinea);
				}
			}
	
			// Si no se ha reemplazado la clave, la añadimos al fichero de variable de entorno de salida nuevo
			// ya que es una nueva clave que no existe en el fichero de variable de entorno de salida nuevo
			if (!replaced)
			{
				// Añadimos la clave y el valor a la lista de salida
				lineasVariableEntornoOutput.add(key + "=" + value);
			}
	
			// Escribimos las líneas en el archivo
			Files.write(variableEntornoFilePath, lineasVariableEntornoOutput, StandardCharsets.UTF_8);
		}
		catch (IOException exception)
		{
			// Creamos un mensaje de error
			String mensajeError = "IOException en actualizarVariableEntorno";

			// Logueamos el mensaje de error
			log.error(mensajeError, exception);

			// Devolvemos la excepción de entrada/salida
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE, mensajeError, exception);
		}
	}

	/**
	 * Recrea el stack.
	 * @param clonezillaRootPath - Path del root de clonezilla.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private void recrearContenedorDocker(Path clonezillaRootPath) throws ImagesClonerClientException
	{
		// Obtemos el path del archivo de docker compose
		Path composeFilePath = this.obtenerPathComposeFile(clonezillaRootPath);

		// Logueamos el mensaje
		log.info("Recreando contenedor ({}...)", Constants.COMANDO_DOCKER_COMPOSE_UP_D_FORCE_RECREATE);

		// Creamos el proceso
		ProcessBuilder processBuilder = new ProcessBuilder(Constants.COMANDO_DOCKER_COMPOSE_UP_D_FORCE_RECREATE);

		// Establecemos el directorio de trabajo
		processBuilder.directory(composeFilePath.toFile());

		try
		{
			// Iniciamos el proceso
			Process process = processBuilder.start();
	
			// Esperamos a que el proceso termine
			this.esperarProceso(process);
	
			// Validamos si el proceso terminó correctamente
			this.validarProceso(process);
		}
		catch (IOException exception)
		{
			// Creamos un mensaje de error
			String mensajeError = "IOException en recrearContenedorDocker";

			// Logueamos el mensaje de error
			log.error(mensajeError, exception);

			// Devolvemos la excepción de entrada/salida
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE, mensajeError, exception);
		}
	}

	/**
	 * Obtiene el path del archivo de docker compose.
	 * @param clonezillaRootPath - Path del root de clonezilla.
	 * @return Path del archivo de docker compose.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private Path obtenerPathComposeFile(Path clonezillaRootPath) throws ImagesClonerClientException
	{
		Path composeFile = clonezillaRootPath.resolve("docker-compose.yml");
		if (!Files.isRegularFile(composeFile))
		{
			// Creamos un mensaje de error
			String mensajeError = "No se encuentra " + composeFile;

			// Logueamos el mensaje de error
			log.error(mensajeError);

			// Devolvemos la excepción de entrada/salida
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE, mensajeError);
		}

		// Devolvemos el path del archivo de docker compose
		return composeFile;
	}

	/**
	 * Espera a que el proceso termine.
	 * @param process - Proceso.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private void esperarProceso(Process process) throws ImagesClonerClientException
	{
		try
		{
			boolean finished = process.waitFor(Constants.COMPOSE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
			if (!finished)
			{
				// Creamos un mensaje de error
				String mensajeError = "Comando superó el tiempo límite de " + Constants.COMPOSE_TIMEOUT_MINUTES + " minutos";
	
				// Logueamos el mensaje de error
				log.error(mensajeError);
	
				// Devolvemos la excepción de entrada/salida
				throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE, mensajeError);
			}
		}
		catch (InterruptedException exception)
		{
			// Creamos un mensaje de error
			String mensajeError = "InterruptedException en esperarProceso";

			// Logueamos el mensaje de error
			log.error(mensajeError, exception);

			// Devolvemos la excepción de entrada/salida
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE, mensajeError, exception);
		}
		finally
		{
			// Validamos si el proceso existe
			if (process != null)
			{
				// Destruimos el proceso
				process.destroyForcibly();
			}
		}
	}

	/**
	 * Valida si el proceso terminó correctamente.
	 * @param process - Proceso.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private void validarProceso(Process process) throws ImagesClonerClientException
	{
		// Obtenemos el código de salida
		int exitCode = process.exitValue();

		// Si el código de salida no es 0, devolvemos la excepción de entrada/salida
		if (exitCode != 0)
		{
			// Creamos un mensaje de error
			String mensajeError = "docker compose falló con código " + exitCode;

			// Logueamos el mensaje de error
			log.error(mensajeError);

			// Devolvemos la excepción de entrada/salida
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ACTIVAR_IMAGEN_CODE, mensajeError);
		}

		// Logueamos el mensaje
		log.info("Configurador de imágenes - contenedor recreado correctamente");
	}
}
