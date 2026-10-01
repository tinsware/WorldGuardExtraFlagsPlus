package dev.tins.worldguardextraflagsplus;

import de.exlll.configlib.ConfigurationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defensive file handling for WGEFP YAML files (config-wgefp.yml, messages-wgefp.yml):
 * oversize quarantine, corrupt backup retention, and atomic writes.
 */
public final class WgefpYamlFileGuard
{
	public static final int MAX_FILE_BYTES = 307_200;
	public static final int MAX_CORRUPT_BACKUPS = 3;

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private WgefpYamlFileGuard()
	{
	}

	public static String corruptBackupPrefix(Path yamlFile)
	{
		return yamlFile.getFileName().toString() + ".corrupt.";
	}

	public static String fileLabel(Path yamlFile)
	{
		return yamlFile.getFileName().toString();
	}

	/**
	 * If the YAML file exists and exceeds {@link #MAX_FILE_BYTES}, renames it to a corrupt backup
	 * and returns {@code true}. Otherwise returns {@code false}.
	 */
	public static boolean checkAndQuarantineOversize(Path yamlFile, Logger logger) throws IOException
	{
		if (!Files.exists(yamlFile))
		{
			return false;
		}

		long size = Files.size(yamlFile);
		if (size <= MAX_FILE_BYTES)
		{
			return false;
		}

		Path quarantinePath = quarantineFile(yamlFile, logger);
		String label = fileLabel(yamlFile);

		if (logger != null)
		{
			logger.warning(label + " exceeds " + (MAX_FILE_BYTES / 1024)
					+ " KB (" + size + " bytes). Moved to " + quarantinePath.getFileName()
					+ " and regenerating defaults.");
		}

		return true;
	}

	/**
	 * Renames an existing YAML file to a corrupt backup (regardless of size).
	 */
	public static void quarantineCorrupt(Path yamlFile, Logger logger) throws IOException
	{
		if (!Files.exists(yamlFile))
		{
			return;
		}

		Path quarantinePath = quarantineFile(yamlFile, logger);
		String label = fileLabel(yamlFile);

		if (logger != null)
		{
			logger.warning("Quarantined corrupt " + label + " to " + quarantinePath.getFileName()
					+ ". Regenerating defaults.");
		}
	}

	private static Path quarantineFile(Path yamlFile, Logger logger) throws IOException
	{
		Path parent = yamlFile.getParent();
		String corruptPrefix = corruptBackupPrefix(yamlFile);
		String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
		Path quarantinePath = parent.resolve(corruptPrefix + timestamp);
		moveWithFallback(yamlFile, quarantinePath);
		pruneCorruptBackups(parent, corruptPrefix, logger);
		return quarantinePath;
	}

	private static void moveWithFallback(Path source, Path target) throws IOException
	{
		try
		{
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (AtomicMoveNotSupportedException e)
		{
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Keeps at most {@link #MAX_CORRUPT_BACKUPS} corrupt backup files for the given prefix; deletes oldest extras.
	 */
	public static void pruneCorruptBackups(Path worldGuardDir, String corruptPrefix, Logger logger)
	{
		if (worldGuardDir == null || !Files.isDirectory(worldGuardDir))
		{
			return;
		}

		try
		{
			List<Path> corruptFiles = new ArrayList<>();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldGuardDir, corruptPrefix + "*"))
			{
				for (Path path : stream)
				{
					if (Files.isRegularFile(path))
					{
						corruptFiles.add(path);
					}
				}
			}

			if (corruptFiles.size() <= MAX_CORRUPT_BACKUPS)
			{
				return;
			}

			corruptFiles.sort(Comparator.comparingLong(WgefpYamlFileGuard::lastModifiedMillis));

			int toDelete = corruptFiles.size() - MAX_CORRUPT_BACKUPS;
			for (int i = 0; i < toDelete; i++)
			{
				Path path = corruptFiles.get(i);
				Files.deleteIfExists(path);
				if (logger != null)
				{
					logger.fine("Deleted old corrupt YAML backup: " + path.getFileName());
				}
			}
		}
		catch (IOException e)
		{
			if (logger != null)
			{
				logger.log(Level.WARNING, "Could not prune corrupt YAML backups: " + e.getMessage(), e);
			}
		}
	}

	private static long lastModifiedMillis(Path path)
	{
		try
		{
			return Files.getLastModifiedTime(path).toMillis();
		}
		catch (IOException e)
		{
			return 0L;
		}
	}

	public static void writeAtomically(Path target, byte[] content) throws IOException
	{
		Path parent = target.getParent();
		if (parent != null)
		{
			Files.createDirectories(parent);
		}

		String tempPrefix = target.getFileName().toString() + "-";
		Path tempFile = Files.createTempFile(parent, tempPrefix, ".tmp");
		try
		{
			Files.write(tempFile, content, StandardOpenOption.TRUNCATE_EXISTING);
			try
			{
				Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException e)
			{
				Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			Files.deleteIfExists(tempFile);
		}
	}

	public static void writeAtomically(Path target, String content) throws IOException
	{
		writeAtomically(target, content.getBytes(StandardCharsets.UTF_8));
	}

	public static boolean isDuplicateKeyError(Throwable cause)
	{
		if (cause == null)
		{
			return false;
		}

		if (cause.getClass().getSimpleName().equals("DuplicateKeyException"))
		{
			return true;
		}

		String message = cause.getMessage();
		return message != null && message.contains("duplicate key");
	}

	public static boolean isRecoverableYamlError(ConfigurationException e)
	{
		if (isDuplicateKeyError(e.getCause()))
		{
			return false;
		}

		return containsRecoverableYamlHint(e.getMessage()) || containsRecoverableYamlHint(getRootCauseMessage(e));
	}

	public static boolean isRecoverableGenericError(Exception e)
	{
		return containsRecoverableYamlHint(e.getMessage()) || containsRecoverableYamlHint(getRootCauseMessage(e));
	}

	private static boolean containsRecoverableYamlHint(String message)
	{
		if (message == null)
		{
			return false;
		}

		String lower = message.toLowerCase();
		return lower.contains("exceeds the limit")
				|| lower.contains("while parsing")
				|| lower.contains("scanner")
				|| lower.contains("yaml")
				|| lower.contains("mapping values")
				|| lower.contains("unexpected character");
	}

	public static String getRootCauseMessage(Throwable throwable)
	{
		Throwable cause = throwable;
		while (cause.getCause() != null && cause.getCause() != cause)
		{
			cause = cause.getCause();
		}
		return cause.getMessage();
	}

	public static String buildYamlErrorMessage(ConfigurationException e, String fileLabel)
	{
		Throwable cause = e.getCause();
		String errorMsg = "Invalid YAML in " + fileLabel;

		if (isDuplicateKeyError(cause))
		{
			String message = cause.getMessage();
			if (message != null && message.contains("duplicate key"))
			{
				int keyStart = message.indexOf("duplicate key");
				if (keyStart != -1)
				{
					String keyPart = message.substring(keyStart);
					errorMsg = "Duplicate key found in " + fileLabel + ": " + keyPart.split("\n")[0].replace("found duplicate key", "").trim();
				}
				else
				{
					errorMsg = "Duplicate key found in " + fileLabel + ". Check the file for duplicate entries.";
				}
			}
			else
			{
				errorMsg = "Duplicate key found in " + fileLabel + ". Check the file for duplicate entries.";
			}
		}
		else if (cause != null)
		{
			String causeMsg = cause.getMessage();
			if (causeMsg != null && causeMsg.contains("duplicate key"))
			{
				errorMsg = "Duplicate key found in " + fileLabel + ". Check the file for duplicate entries.";
			}
			else
			{
				errorMsg = causeMsg != null ? causeMsg : cause.getClass().getSimpleName();
				if (errorMsg.length() > 200)
				{
					errorMsg = errorMsg.substring(0, 200) + "...";
				}
			}
		}

		return errorMsg;
	}

	public static void logYamlLoadFailure(Logger logger, Path yamlFile, String fileLabel, String errorMsg, boolean recoveryAttempted)
	{
		logger.severe("CRITICAL: " + errorMsg);
		logger.severe("File location: " + yamlFile.toAbsolutePath());
		if (recoveryAttempted)
		{
			logger.severe("Auto-recovery was attempted but loading still failed.");
		}
		logger.severe("Rename or delete " + fileLabel + ", then restart the server.");
		logger.severe("Disabling plugin until the file can be loaded.");
	}
}
