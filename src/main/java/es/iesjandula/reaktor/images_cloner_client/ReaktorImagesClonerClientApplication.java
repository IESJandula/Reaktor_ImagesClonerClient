package es.iesjandula.reaktor.images_cloner_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import es.iesjandula.reaktor.images_cloner_client.config.ImagesClonerProperties;

@SpringBootApplication
@ComponentScan(basePackages = { "es.iesjandula" })
@EnableConfigurationProperties(ImagesClonerProperties.class)
public class ReaktorImagesClonerClientApplication
{
	public static void main(String[] args)
	{
		System.exit(SpringApplication.exit(
				SpringApplication.run(ReaktorImagesClonerClientApplication.class, args)));
	}
}
