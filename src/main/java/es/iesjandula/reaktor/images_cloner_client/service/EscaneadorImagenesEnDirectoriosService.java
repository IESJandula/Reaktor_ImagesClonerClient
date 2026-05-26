package es.iesjandula.reaktor.images_cloner_client.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscaneadorImagenesEnDirectoriosService
{
	/** Propiedades de la aplicación - Clonezilla Root */
	@Value("${images-cloner.clonezilla-root}")
	private String clonezillaRoot;

	/** Propiedades de la aplicación - Imágenes Dir */
	@Value("${images-cloner.images-dir}")
	private String imagesDir;

	/**
	 * Escanea los nombres de las imágenes en el directorio de imágenes.
	 * @return Lista de nombres de las imágenes.
	 */
	public List<String> escanearNombresImagenes()
	{
		// Creamos una variable para la lista de nombres de imágenes
		List<String> nombreImagenes = new ArrayList<String>();

		// Resolvemos la ruta de las imágenes
		Path imagesPath = Path.of(this.clonezillaRoot, this.imagesDir).normalize();

		// Validamos si la carpeta de imágenes existe
		if (!Files.isDirectory(imagesPath))
		{
			// Mostramos un mensaje de advertencia
			log.warn("Carpeta de imágenes inexistente: {}", imagesPath);
		}
		else
		{
			// Escaneamos los nombres de las imágenes
			nombreImagenes = this.escanearNombresImagenesInternal(imagesPath);

			// Mostramos un mensaje de información
			log.info("Imágenes encontradas: {}", nombreImagenes);
		}

		// Devolvemos la lista de nombres de imágenes
		return nombreImagenes;
	}

	/**
	 * Escanea los nombres de las imágenes en el directorio de imágenes.
	 * @param imagesPath - Ruta de la carpeta de imágenes.
	 * @return Lista de nombres de las imágenes.
	 */
	private List<String> escanearNombresImagenesInternal(Path imagesPath)
	{
		// Creamos la variable de retorno
		List<String> nombresImagenes = new ArrayList<String>();
		
		// Creamos una variable para el stream de las imágenes
		Stream<Path> streamPaths = null;

		try
		{
			// Listamos las imágenes de la carpeta
			streamPaths = Files.list(imagesPath);

			// Creamos un iterador para la lista de imágenes
			Iterator<Path> iterator = streamPaths.iterator();

			// Recorremos la lista de imágenes
			while (iterator.hasNext())
			{
				nombresImagenes.add(iterator.next().getFileName().toString());
			}
		}
		catch (IOException ioException)
		{
			log.error("No se pudo listar {}", imagesPath, ioException);
		}
		finally
		{
			// Cerramos el stream si no es null
			if (streamPaths != null)
			{
				streamPaths.close();
			}
		}

		// Devolvemos la lista de nombres de imágenes
		return nombresImagenes;
	}
}
