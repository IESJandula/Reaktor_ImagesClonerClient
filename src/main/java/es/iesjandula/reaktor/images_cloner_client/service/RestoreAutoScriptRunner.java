package es.iesjandula.reaktor.images_cloner_client.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import es.iesjandula.reaktor.images_cloner_client.config.ImagesClonerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestoreAutoScriptRunner
{
	private static final int SCRIPT_TIMEOUT_MINUTES = 15;

	private final ImagesClonerProperties properties;

	public ScriptResult run(String image, String action) throws IOException, InterruptedException
	{
		Path script = Path.of(properties.getClonezillaRoot(), "scripts", "set-restore-auto.sh").normalize();
		if (!Files.isExecutable(script) && !Files.isRegularFile(script))
		{
			throw new IOException("No se encuentra set-restore-auto.sh en " + script);
		}

		List<String> command = List.of(script.toString(), image, action);
		log.info("Ejecutando: {}", String.join(" ", command));

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(Path.of(properties.getClonezillaRoot()).toFile());
		processBuilder.redirectErrorStream(false);

		Process process = processBuilder.start();
		String stdout = readStream(process.getInputStream());
		String stderr = readStream(process.getErrorStream());

		boolean finished = process.waitFor(SCRIPT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
		if (!finished)
		{
			process.destroyForcibly();
			throw new IOException("set-restore-auto.sh superó el tiempo límite de " + SCRIPT_TIMEOUT_MINUTES + " min");
		}

		int exitCode = process.exitValue();
		if (exitCode != 0)
		{
			log.error("set-restore-auto.sh falló (exit={}). stderr:\n{}", exitCode, stderr);
			if (!stdout.isBlank())
			{
				log.error("stdout:\n{}", stdout);
			}
			return new ScriptResult(false, exitCode, stdout, stderr);
		}

		log.info("set-restore-auto.sh completado correctamente");
		if (!stderr.isBlank())
		{
			log.warn("stderr (informativo):\n{}", stderr);
		}
		return new ScriptResult(true, exitCode, stdout, stderr);
	}

	private static String readStream(java.io.InputStream inputStream) throws IOException
	{
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
		{
			return reader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	public record ScriptResult(boolean success, int exitCode, String stdout, String stderr)
	{
		public String combinedLog()
		{
			List<String> parts = new ArrayList<>();
			if (!stdout.isBlank())
			{
				parts.add("stdout: " + stdout);
			}
			if (!stderr.isBlank())
			{
				parts.add("stderr: " + stderr);
			}
			return String.join(" | ", parts);
		}
	}
}
