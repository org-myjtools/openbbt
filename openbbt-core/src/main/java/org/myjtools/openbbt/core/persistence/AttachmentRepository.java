package org.myjtools.openbbt.core.persistence;

import java.util.UUID;

public interface AttachmentRepository extends Repository {

	void storeAttachment(UUID executionID, UUID attachmentID, byte[] bytes, String contentType);

}
