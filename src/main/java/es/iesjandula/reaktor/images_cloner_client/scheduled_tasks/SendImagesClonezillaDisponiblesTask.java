package es.iesjandula.reaktor.images_cloner_client.scheduled_tasks;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.iesjandula.reaktor.base.utils.BaseException;
import es.iesjandula.reaktor.base.utils.HttpClientUtils;
import es.iesjandula.reaktor.base_client.security.service.AuthorizationService;
import es.iesjandula.reaktor.base_client.utils.BaseClientException;
import es.iesjandula.reaktor.images_cloner_client.config.ImagesClonerProperties;
import es.iesjandula.reaktor.images_cloner_client.dto.ConfiguracionClonadorDto;
import es.iesjandula.reaktor.images_cloner_client.dto.ImagenClonezilla;
import es.iesjandula.reaktor.images_cloner_client.service.ImageDirectoryScanner;
import es.iesjandula.reaktor.images_cloner_client.service.RestoreAutoScriptRunner;
import es.iesjandula.reaktor.images_cloner_client.utils.Constants;
import es.iesjandula.reaktor.images_cloner_client.utils.ImagesClonerClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Francisco Manuel Benítez Chico
 */
@Slf4j
@Component
public class SendImagesClonezillaDisponiblesTask
{
	/** Servicio de escaneo de directorios de imágenes */
    @Autowired
    private ImageDirectoryScanner imageDirectoryScanner;

    /** Servicio de autorización */
    @Autowired
    private AuthorizationService authorizationService;

	/** Tiempo de espera de la conexión HTTP */
	@Value("${reaktor.http_connection_timeout}")
	private int httpConnectionTimeout ;

	/** URL del servidor de impresoras */
	@Value("${reaktor.images_cloner_server_url}")
	private String imagesClonerServerUrl ;

	/**
	 * Metodo encargado de ejecutar el cada X tiempo una petición al servidor 
	 * para avisar de las imágenes que hay disponibles en Clonezilla
	 */
	@Scheduled(cron = "0 45-59/5 7 * * MON-FRI")
	@Scheduled(cron = "0 */5 8-19 * * MON-FRI")
	@Scheduled(cron = "0 0-30/5 20 * * MON-FRI")
	public void enviarImagenesClonezillaDisponibles()
	{
		// Mostramos un mensaje de información
		log.info("IMAGES_CLONER_CLIENT - INICIO - Enviar imágenes Clonezilla disponibles");

		// Escaneamos los nombres de las imágenes locales
		List<String> nombresImagenesLocales = imageDirectoryScanner.escanearNombresImagenes();

		// Creamos el cliente HTTP
		CloseableHttpClient closeableHttpClient = HttpClientUtils.crearHttpClientConTimeout(this.httpConnectionTimeout) ;

		try
		{
			// Enviamos una petición al servidor con el nombre de las imágenes locales disponibles en Clonezilla
			// aprovechando la respuesta para obtener la configuración del clonador
			ConfiguracionClonadorDto configuracionClonadorDto = this.enviarImagenesClonezillaDisponiblesPeticionPost(closeableHttpClient, nombresImagenesLocales);

            
		}
		catch (ImagesClonerClientException imagesClonerClientException)
		{
			// Se ha logueado la excepción previamente, por lo que no se hace nada
		}
		finally
		{
			try
			{
				closeableHttpClient.close() ;
			}
			catch (IOException ioException)
			{
				log.error("IOException en httpClient mientras se cerraba el flujo de datos", ioException) ;
			}
		}
	}

	/**
	 * Metodo encargado de enviar una petición POST al servidor para avisar de las imágenes que hay disponibles en Clonezilla
	 * @param closeableHttpClient - Cliente HTTP.
	 * @param nombresImagenesLocales - Lista de nombres de las imágenes locales.
	 * @return Configuración del clonador.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private ConfiguracionClonadorDto enviarImagenesClonezillaDisponiblesPeticionPost(CloseableHttpClient closeableHttpClient, List<String> nombresImagenesLocales)
		throws ImagesClonerClientException
	{
		try
		{
			// Creamos la petición HTTP
			HttpPost httpPost = new HttpPost(this.imagesClonerServerUrl + "/images_cloner/client/");
	
			// Añadimos el token a la petición
			httpPost.addHeader("Authorization", "Bearer " + this.authorizationService.obtenerTokenPersonalizado(this.httpConnectionTimeout)) ;
	
			// Indicamos que viaja un JSON
			httpPost.setHeader("Content-Type", "application/json");

			// Creamos un objeto ObjectMapper
			ObjectMapper objectMapperRequest = new ObjectMapper();
	
			// Serialización de la entidad JSON asegurando UTF-8
			StringEntity entity = new StringEntity(objectMapperRequest.writeValueAsString(nombresImagenesLocales), StandardCharsets.UTF_8) ;
			httpPost.setEntity(entity) ;

			// Enviamos la petición
			CloseableHttpResponse response = closeableHttpClient.execute(httpPost);

			// Obtenemos y devolvemos la configuración del clonador
			return this.enviarImagenesClonezillaDisponiblesPeticionPostObtenerConfiguracionClonador(response) ;
		}
		catch (IOException ioException)
		{
			// Logueamos la excepción
			log.error("IOException en enviarImagenesClonezillaDisponiblesPeticionPost", ioException) ;

			// Devolvemos la excepción de servidor
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, "IOException en enviarImagenesClonezillaDisponiblesPeticionPost") ;
		}
		catch (BaseException baseException)
		{
			// Logueamos la excepción
			log.error("BaseException en enviarImagenesClonezillaDisponiblesPeticionPost", baseException) ;

			// Devolvemos la excepción de servidor
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, "BaseException en enviarImagenesClonezillaDisponiblesPeticionPost") ;
		}
		catch (BaseClientException baseClientException)
		{
			// Logueamos la excepción
			log.error("BaseClientException en enviarImagenesClonezillaDisponiblesPeticionPost", baseClientException) ;

			// Devolvemos la excepción de servidor
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, "BaseClientException en enviarImagenesClonezillaDisponiblesPeticionPost") ;
		}
	}

	/**
	 * Metodo encargado de enviar una petición POST al servidor para obtener la configuración del clonador
	 * @param response - Respuesta HTTP.
	 * @return Configuración del clonador.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private ConfiguracionClonadorDto enviarImagenesClonezillaDisponiblesPeticionPostObtenerConfiguracionClonador(CloseableHttpResponse response)
		throws ImagesClonerClientException
	{
		try
		{
			// Obtenemos el estado de la respuesta
			int status = response.getStatusLine().getStatusCode();

			// Validamos el estado de la respuesta
			if (status < HttpStatus.SC_OK || status >= HttpStatus.SC_MULTIPLE_CHOICES)
			{
				// Creamos un mensaje de error
				String mensajeError = "POST " + this.imagesClonerServerUrl + "/images_cloner/client/" + " respondió HTTP " + status ;

				// Logueamos la excepción
				log.error(mensajeError) ;

				// Devolvemos la excepción de servidor
				throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, mensajeError) ;
			}

			// Obtenemos el cuerpo de la respuesta
			String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

			// Si el body está vacío se lanza una excepción
			if (body == null || body.isBlank())
			{
				// Creamos un mensaje de error
				String mensajeError = "El cuerpo de la respuesta está vacío" ;

				// Logueamos la excepción
				log.error(mensajeError) ;

				// Devolvemos la excepción de servidor
				throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, mensajeError) ;
			}

			// Creamos un objeto ObjectMapper
			ObjectMapper objectMapperResponse = new ObjectMapper();

			// Deserializamos el cuerpo de la respuesta a una configuración del clonador
			return objectMapperResponse.readValue(body, ConfiguracionClonadorDto.class) ;
		}
		catch (IOException ioException)
		{
			// Logueamos la excepción
			log.error("IOException en enviarImagenesClonezillaDisponiblesPeticionPostObtenerConfiguracionClonador", ioException) ;

			// Devolvemos la excepción de servidor
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, "IOException en enviarImagenesClonezillaDisponiblesPeticionPostObtenerConfiguracionClonador") ;
		}
	}
}
