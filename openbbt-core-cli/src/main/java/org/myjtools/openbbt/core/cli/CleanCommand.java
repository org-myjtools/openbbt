package org.myjtools.openbbt.core.cli;

import org.myjtools.openbbt.core.OpenBBTConfig;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
	name = "clean",
	description = "Delete plugins, logs, and data files of previous runs"
)
public class CleanCommand implements Runnable {

	@CommandLine.Option(names = {"all"}, description = "Clean all")
	boolean all;

	@CommandLine.Option(names = {"logs"}, description = "Clean logs")
	boolean logs;

	@CommandLine.Option(names = {"data"}, description = "Clean data")
	boolean data;

	@Override
	public void run() {
		try {
			if (all || logs) {
				System.out.println("Cleaning logs...");
				deleteFolder(OpenBBTConfig.LOGS_PATH);
			}
			if (all || data) {
				System.out.println("Cleaning data...");
				deleteFolder(OpenBBTConfig.DATA_PATH);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private void deleteFolder(Path path) throws IOException {
		if (path.toFile().exists()) {
			Files.walk(path)
				.sorted(java.util.Comparator.reverseOrder())
				.forEach(p -> {
					try {
						Files.delete(p);
					} catch (Exception e) {
						System.err.println("Failed to delete " + p + ": " + e.getMessage());
					}
				});
		}
	}

}
