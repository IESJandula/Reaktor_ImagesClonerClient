package es.iesjandula.reaktor.images_cloner_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * @author Francisco Manuel Benítez Chico
 */
@SpringBootApplication
@ComponentScan(basePackages = { "es.iesjandula" })
@EnableScheduling
public class ReaktorImagesClonerClientApplication
{
	public static void main(String[] args)
	{
		SpringApplication.run(ReaktorImagesClonerClientApplication.class, args);
	}
}
