package es.iesjandula.reaktor.images_cloner_client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "images-cloner")
public class ImagesClonerProperties
{
	private String serverUrl;
	private int pollIntervalSeconds;
	private String clonezillaRoot;
	private String imagesDir;
	private int httpConnectionTimeoutMs;
	private String clientId;
}
