package org.myjtools.openbbt.persistence.attachment;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import java.io.ByteArrayInputStream;
import java.util.UUID;

public class MinioAttachmentRepository implements AttachmentRepository {

	private static final String BUCKET = "openbbt-attachments";

	private final MinioClient minioClient;

	public MinioAttachmentRepository(String url, String accessKey, String secretKey) {
		this.minioClient = MinioClient.builder()
			.endpoint(url)
			.credentials(accessKey, secretKey)
			.build();
		ensureBucket();
	}

	@Override
	public void storeAttachment(UUID executionID, UUID attachmentID, byte[] bytes, String contentType) {
		try {
			minioClient.putObject(PutObjectArgs.builder()
				.bucket(BUCKET)
				.object(executionID + "/" + attachmentID)
				.stream(new ByteArrayInputStream(bytes), bytes.length, -1)
				.contentType(contentType != null ? contentType : "application/octet-stream")
				.build());
		} catch (Exception e) {
			throw new OpenBBTException(e, "Failed to store attachment {} in Minio", attachmentID);
		}
	}

	private void ensureBucket() {
		try {
			boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build());
			if (!exists) {
				minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
			}
		} catch (Exception e) {
			throw new OpenBBTException(e, "Failed to ensure Minio bucket '{}'", BUCKET);
		}
	}

}
