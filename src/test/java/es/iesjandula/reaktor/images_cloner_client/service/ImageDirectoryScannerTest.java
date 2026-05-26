package es.iesjandula.reaktor.images_cloner_client.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.iesjandula.reaktor.images_cloner_client.config.ImagesClonerProperties;

class ImageDirectoryScannerTest
{
	@TempDir
	Path tempDir;

	@Test
	void scanImageNames_listsOnlyDirectories() throws Exception
	{
		Files.createDirectory(tempDir.resolve("demo-image"));
		Files.createDirectory(tempDir.resolve("otra"));
		Files.writeString(tempDir.resolve("readme.txt"), "x");

		ImagesClonerProperties properties = new ImagesClonerProperties();
		properties.setClonezillaRoot(tempDir.getParent().toString());
		properties.setImagesDir(tempDir.getFileName().toString());

		ImageDirectoryScanner scanner = new ImageDirectoryScanner(properties);

		assertThat(scanner.scanImageNames()).containsExactly("demo-image", "otra");
	}
}
