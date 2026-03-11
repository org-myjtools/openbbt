package org.myjtools.openbbt.persistence.test.attachment;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractAttachmentRepositoryTest {

	protected abstract AttachmentRepository repository();

	@Test
	void storeAttachment_withContentType() {
		UUID executionID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();
		byte[] content = "hello attachment".getBytes();

		assertDoesNotThrow(() -> repository().storeAttachment(executionID, attachmentID, content, "text/plain"));
	}

	@Test
	void storeAttachment_withoutContentType() {
		UUID executionID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();
		byte[] content = new byte[]{1, 2, 3, 4};

		assertDoesNotThrow(() -> repository().storeAttachment(executionID, attachmentID, content, null));
	}

	@Test
	void storeAttachment_binaryContent() {
		UUID executionID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();
		byte[] content = new byte[1024];
		for (int i = 0; i < content.length; i++) {
			content[i] = (byte) (i % 256);
		}

		assertDoesNotThrow(() -> repository().storeAttachment(executionID, attachmentID, content, "application/octet-stream"));
	}

	@Test
	void storeAttachment_emptyContent() {
		UUID executionID = UUID.randomUUID();
		UUID attachmentID = UUID.randomUUID();

		assertDoesNotThrow(() -> repository().storeAttachment(executionID, attachmentID, new byte[0], "text/plain"));
	}

	@Test
	void storeAttachment_multipleAttachmentsForSameExecution() {
		UUID executionID = UUID.randomUUID();
		UUID attachment1 = UUID.randomUUID();
		UUID attachment2 = UUID.randomUUID();

		assertDoesNotThrow(() -> {
			repository().storeAttachment(executionID, attachment1, "first".getBytes(), "text/plain");
			repository().storeAttachment(executionID, attachment2, "second".getBytes(), "text/plain");
		});
	}

	@Test
	void storeAttachment_sameAttachmentDifferentExecutions() {
		UUID attachmentID = UUID.randomUUID();

		assertDoesNotThrow(() -> {
			repository().storeAttachment(UUID.randomUUID(), attachmentID, "first".getBytes(), "text/plain");
			repository().storeAttachment(UUID.randomUUID(), attachmentID, "second".getBytes(), "text/plain");
		});
	}

}
