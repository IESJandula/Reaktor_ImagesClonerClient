package es.iesjandula.reaktor.images_cloner_client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImagenClonezilla
{
	private String nombreImagen;
	private String estado;
	private String accion;
}
