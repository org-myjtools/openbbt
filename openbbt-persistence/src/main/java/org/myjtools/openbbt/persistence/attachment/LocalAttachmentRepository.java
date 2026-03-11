package org.myjtools.openbbt.persistence.attachment;

import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class LocalAttachmentRepository implements AttachmentRepository {

	private final Path basePath;

	public LocalAttachmentRepository(Path basePath) {
		this.basePath = basePath;
	}

	@Override
	public void storeAttachment(UUID executionID, UUID attachmentID, byte[] bytes, String contentType) {
		try {
			Path dir = basePath.resolve(executionID.toString());
			Files.createDirectories(dir);
			Files.write(dir.resolve(attachmentID.toString()), bytes);
			if (contentType != null) {
				Files.writeString(dir.resolve(attachmentID + ".mime"), contentType);
			}
		} catch (IOException e) {
			throw new OpenBBTException(e, "Failed to store attachment {}", attachmentID);
		}
	}

}
