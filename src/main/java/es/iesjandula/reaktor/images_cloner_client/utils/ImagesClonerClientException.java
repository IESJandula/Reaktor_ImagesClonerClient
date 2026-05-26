package es.iesjandula.reaktor.images_cloner_client.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.Getter;

@Getter
public class ImagesClonerClientException extends Exception
{
    // Identificador de versión de la clase Serializable
	private static final long serialVersionUID = -621923489318165563L;
    
    // Código de error personalizado
	private int codigo;
	
    /**
     * Constructor con código y mensaje.
     * @param codigo Código del error.
     * @param mensaje Mensaje del error.
     */
	public ImagesClonerClientException(int codigo, String mensaje)
	{
		super(mensaje);

		this.codigo = codigo;
	}
	
    /**
     * Constructor con código, mensaje y causa original.
     * @param codigo Código del error.
     * @param mensaje Mensaje del error.
     * @param excepcion Causa original de la excepción.
     */
	public ImagesClonerClientException(int codigo, String mensaje, Throwable excepcion)
	{
		super(mensaje, excepcion);

		this.codigo = codigo;
	}
	
    /**
     * Devuelve un mapa con los detalles de la excepción:
     * código, mensaje y stacktrace de la causa si existe.
     */
	public Object getBodyExceptionMessage()
	{
		Map<String, Object> mapBodyException = new HashMap<String, Object>() ;
		mapBodyException.put("codigo", this.codigo);
		mapBodyException.put("mensaje", this.getMessage());
		
		if (this.getCause() != null)
		{
			String stackTrace = ExceptionUtils.getStackTrace(this.getCause());
			mapBodyException.put("excepcion", stackTrace);
		}
		
		return mapBodyException;
	}
}
