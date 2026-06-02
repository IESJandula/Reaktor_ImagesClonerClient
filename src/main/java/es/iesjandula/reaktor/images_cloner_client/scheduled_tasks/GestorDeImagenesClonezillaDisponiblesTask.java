package es.iesjandula.reaktor.images_cloner_client.scheduled_tasks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.iesjandula.reaktor.base.utils.BaseException;
import es.iesjandula.reaktor.base.utils.HttpClientUtils;
import es.iesjandula.reaktor.base_client.security.service.AuthorizationService;
import es.iesjandula.reaktor.base_client.utils.BaseClientException;
import es.iesjandula.reaktor.images_cloner_client.dto.ConfiguracionClonadorDto;
import es.iesjandula.reaktor.images_cloner_client.service.EscaneadorImagenesEnDirectoriosService;
import es.iesjandula.reaktor.images_cloner_client.service.ConfiguradorImagenesService;
import es.iesjandula.reaktor.images_cloner_client.utils.Constants;
import es.iesjandula.reaktor.images_cloner_client.utils.ImagesClonerClientException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Francisco Manuel Benítez Chico
 */
@Slf4j
@Component
public class GestorDeImagenesClonezillaDisponiblesTask
{
	/** Servicio de escaneo de directorios de imágenes */
    @Autowired
    private EscaneadorImagenesEnDirectoriosService escaneadorImagenesEnDirectoriosService ;

    /** Servicio de autorización */
    @Autowired
    private AuthorizationService authorizationService ;

	/** Servicio de configuración de imágenes */
	@Autowired
	private ConfiguradorImagenesService	configuradorImagenesService ;

	/** Tiempo de espera de la conexión HTTP */
	@Value("${reaktor.http_connection_timeout}")
	private int httpConnectionTimeout ;

	/** URL del servidor de impresoras */
	@Value("${reaktor.images_cloner_server_url}")
	private String imagesClonerServerUrl ;

	/**
	 * Metodo encargado de ejecutar el cada X tiempo una petición al servidor 
	 * para avisar de las imágenes que hay disponibles en Clonezilla 
	 * cada 5 minutos y que se ejecute al inicio del servidor
	 */
	@Scheduled(cron = "0 */5 * * * *")
	@Scheduled(initialDelay = 15000)
	public void enviarImagenesClonezillaDisponibles()
	{
		// Mostramos un mensaje de información
		log.info("IMAGES_CLONER_CLIENT - INICIO - Enviar imágenes Clonezilla disponibles");

		// Escaneamos los nombres de las imágenes locales
		List<String> nombresImagenesLocales = this.escaneadorImagenesEnDirectoriosService.escanearNombresImagenes();

		// Creamos el cliente HTTP
		CloseableHttpClient closeableHttpClient = HttpClientUtils.crearHttpClientConTimeout(this.httpConnectionTimeout) ;

		try
		{
			// Enviamos una petición al servidor con el nombre de las imágenes locales disponibles en Clonezilla
			// aprovechando la respuesta para obtener la configuración del clonador
			ConfiguracionClonadorDto configuracionClonadorDto = this.enviarImagenesClonezillaDisponiblesPeticionPost(closeableHttpClient, nombresImagenesLocales);

            // Aplicamos la configuración del clonador
			this.aplicarConfiguracionClonador(closeableHttpClient, configuracionClonadorDto);
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
		CloseableHttpResponse closeableHttpResponse = null ;

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
			closeableHttpResponse = closeableHttpClient.execute(httpPost);

			// Obtenemos y devolvemos la configuración del clonador
			return this.enviarImagenesClonezillaDisponiblesPeticionPostObtenerConfiguracionClonador(closeableHttpResponse) ;
		}
		catch (IOException ioException)
		{
			// Creamos un mensaje de error
			String mensajeError = "IOException en enviarImagenesClonezillaDisponiblesPeticionPost";

			// Logueamos la excepción
			log.error(mensajeError, ioException) ;

			// Devolvemos la excepción de servidor
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, mensajeError) ;
		}
		catch (BaseException baseException)
		{
			// Creamos un mensaje de error
			String mensajeError = "BaseException en enviarImagenesClonezillaDisponiblesPeticionPost";

			// Logueamos la excepción
			log.error(mensajeError, baseException) ;

			// Devolvemos la excepción de servidor
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, mensajeError) ;
		}
		catch (BaseClientException baseClientException)
		{
			// Creamos un mensaje de error
			String mensajeError = "BaseClientException en enviarImagenesClonezillaDisponiblesPeticionPost";

			// Logueamos la excepción
			log.error(mensajeError, baseClientException) ;

			// Devolvemos la excepción de servidor
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, mensajeError) ;
		}
		finally
		{
			try
			{
				if (closeableHttpResponse != null)
				{
					closeableHttpResponse.close() ;
				}
			}
			catch (IOException ioException)
			{
				// Creamos un mensaje de error
				String mensajeError = "IOException en closeableHttpResponse mientras se cerraba el flujo de datos";

				// Logueamos la excepción
				log.error(mensajeError, ioException) ;

				// Devolvemos la excepción de servidor
				throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, mensajeError) ;
			}
		}
	}

	/**
	 * Metodo encargado de enviar una petición POST al servidor para obtener la configuración del clonador
	 * @param closeableHttpResponse - Respuesta HTTP.
	 * @return Configuración del clonador.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private ConfiguracionClonadorDto enviarImagenesClonezillaDisponiblesPeticionPostObtenerConfiguracionClonador(CloseableHttpResponse closeableHttpResponse)
		throws ImagesClonerClientException
	{
		try
		{
			// Obtenemos el estado de la respuesta
			int status = closeableHttpResponse.getStatusLine().getStatusCode();

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
			String body = EntityUtils.toString(closeableHttpResponse.getEntity(), StandardCharsets.UTF_8);

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
			// Creamos un mensaje de error
			String mensajeError = "IOException en enviarImagenesClonezillaDisponiblesPeticionPostObtenerConfiguracionClonador";

			// Logueamos la excepción
			log.error(mensajeError, ioException) ;

			// Devolvemos la excepción de servidor
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, mensajeError) ;
		}
		finally
		{
			try
			{
				if (closeableHttpResponse != null)
				{
					closeableHttpResponse.close() ;
				}
			}
			catch (IOException ioException)
			{
				// Creamos un mensaje de error
				String mensajeError = "IOException en closeableHttpResponse mientras se cerraba el flujo de datos";

				// Logueamos la excepción
				log.error(mensajeError, ioException) ;

				// Devolvemos la excepción de servidor
				throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_ENVIAR_IMAGENES_DISPONIBLES_CODE, mensajeError) ;
			}
		}
	}

	/**
	 * Metodo encargado de aplicar la configuración del clonador
	 * @param closeableHttpClient - Cliente HTTP.
	 * @param configuracionClonador - Configuración del clonador.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private void aplicarConfiguracionClonador(CloseableHttpClient closeableHttpClient, ConfiguracionClonadorDto configuracionClonador) throws ImagesClonerClientException
	{
		// Validamos si se debe activar el modo menú
		if (configuracionClonador.isMenuActivo())
		{
			// Activamos el modo menú
			this.configuradorImagenesService.activarModoMenu();
		}
		else if (configuracionClonador.isActivarImagen())
		{
			// Obtenemos el nombre de la imagen
			String nombreImagen = configuracionClonador.getNombreImagen();

			// Obtenemos la acción
			String accion = configuracionClonador.getAccion();

			// Activamos la imagen
			this.configuradorImagenesService.activarImagen(nombreImagen, accion);

			// Confirmamos que la imagen está activada
			this.confirmarImagenActivada(closeableHttpClient, nombreImagen);
		}
	}

	/**
	 * Metodo encargado de confirmar que la imagen está activada
	 * @param nombreImagen - Nombre de la imagen.
	 * @throws ImagesClonerClientException - Excepción de cliente de clonador de imágenes.
	 */
	private void confirmarImagenActivada(CloseableHttpClient closeableHttpClient, String nombreImagen) throws ImagesClonerClientException
	{
		CloseableHttpResponse closeableHttpResponse = null ;

		try
		{
			// Creamos la petición HTTP
			HttpPut httpPut = new HttpPut(this.imagesClonerServerUrl + "/images_cloner/client/");

			// Añadimos el token a la petición
			httpPut.addHeader("Authorization", "Bearer " + this.authorizationService.obtenerTokenPersonalizado(this.httpConnectionTimeout)) ;

			// Añadimos el nombre de la imagen a la petición
			httpPut.addHeader("nombreImagen", nombreImagen);

			// Enviamos la petición
			closeableHttpResponse = closeableHttpClient.execute(httpPut);

			// Obtenemos el estado de la respuesta
			int status = closeableHttpResponse.getStatusLine().getStatusCode();

			// Validamos el estado de la respuesta
			if (status < HttpStatus.SC_OK || status >= HttpStatus.SC_MULTIPLE_CHOICES)
			{
				String mensaje = "PUT " + this.imagesClonerServerUrl + "/images_cloner/client/" + " respondió HTTP " + status;

				log.error(mensaje);
				throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_CONFIRMAR_IMAGEN_CODE, mensaje);
			}
		}
		catch (IOException ioException)
		{
			// Creamos un mensaje de error
			String mensajeError = "IOException en confirmarImagenActivada";

			// Logueamos la excepción
			log.error(mensajeError, ioException);

			// Devolvemos la excepción de cliente
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_CONFIRMAR_IMAGEN_CODE, mensajeError);
		}
		catch (BaseException baseException)
		{
			// Creamos un mensaje de error
			String mensajeError = "BaseException en confirmarImagenActivada";

			// Logueamos la excepción
			log.error(mensajeError, baseException);

			// Devolvemos la excepción de cliente
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_CONFIRMAR_IMAGEN_CODE, mensajeError);
		}
		catch (BaseClientException baseClientException)
		{
			// Creamos un mensaje de error
			String mensajeError = "BaseClientException en confirmarImagenActivada";

			// Logueamos la excepción
			log.error(mensajeError, baseClientException);

			// Devolvemos la excepción de cliente
			throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_CONFIRMAR_IMAGEN_CODE, mensajeError);
		}
		finally
		{
			try
			{
				if (closeableHttpResponse != null)
				{
					closeableHttpResponse.close() ;
				}
			}
			catch (IOException ioException)
			{
				// Creamos un mensaje de error
				String mensajeError = "IOException en closeableHttpResponse mientras se cerraba el flujo de datos";

				// Logueamos la excepción
				log.error(mensajeError, ioException) ;

				// Devolvemos la excepción de cliente
				throw new ImagesClonerClientException(Constants.ERR_ERROR_AL_CONFIRMAR_IMAGEN_CODE, mensajeError) ;
			}
		}
	}
}
