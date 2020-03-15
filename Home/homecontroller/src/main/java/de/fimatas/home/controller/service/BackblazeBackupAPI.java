package de.fimatas.home.controller.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.backblaze.b2.client.B2ListFilesIterable;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactory;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactoryImpl;
import com.backblaze.b2.util.B2ExecutorUtils;

@Component
public class BackblazeBackupAPI {

	@Autowired
	private Environment env;

	private ExecutorService executor;

	private static final Log LOG = LogFactory.getLog(BackblazeBackupAPI.class);

	public synchronized void backup(Stream<Path> pathes) {

		// Getting credentials
		String applicationkeyid = env.getProperty("backup.backblaze.applicationkeyid");
		String applicationkey = env.getProperty("backup.backblaze.applicationkey");
		String bucketid = env.getProperty("backup.backblaze.bucketid");

		HttpClientFactory httpClientFactory = HttpClientFactoryImpl.builder().build();
		B2StorageClient client = B2StorageHttpClientBuilder
				.builder(applicationkeyid, applicationkey, "HomeController")
				.setHttpClientFactory(httpClientFactory).build();

		// Comparing files
		B2ListFilesIterable b2ListFilesIterable;
		LinkedHashSet<String> fileNames = new LinkedHashSet<>();
		try {
			String environmentIdentifier = environmentIdentifier();
			b2ListFilesIterable = client.fileNames(bucketid);
			for (B2FileVersion x : b2ListFilesIterable) {
				fileNames.add(x.getFileName());
			}
			Stream<Path> pathesNotBackedUp = pathes.filter(
					path -> !fileNames.contains(environmentIdentifier + "/" + path.toFile().getName())); // NOSONAR

			// Backup
			pathesNotBackedUp
					.forEach(path -> backupSingleFile(path, client, bucketid, environmentIdentifier));
		} catch (Exception e) {
			LOG.error("Backup error:", e);
		} finally {
			if (client != null) {
				client.close();
			}
		}

	}

	private void backupSingleFile(Path path, B2StorageClient client, String bucketid,
			String environmentIdentifier) {

		File fileToBackup = path.toFile();

		try {
			B2ContentSource source = B2FileContentSource.build(fileToBackup);
			B2UploadFileRequest request = B2UploadFileRequest.builder(bucketid,
					environmentIdentifier + "/" + fileToBackup.getName(), B2ContentTypes.B2_AUTO, source)
					.build();

			final long contentLength = request.getContentSource().getContentLength();
			B2FileVersion file;
			if (client.getFilePolicy().shouldBeLargeFile(contentLength)) {
				file = client.uploadLargeFile(request, getExecutor());
			} else {
				file = client.uploadSmallFile(request);
			}

			B2ListFilesIterable unfinishedFiles = client.unfinishedLargeFiles(bucketid);

			if (file != null && StringUtils.isNotBlank(file.getFileId())) {
				while (unfinishedFiles.iterator().hasNext()) {
					if (unfinishedFiles.iterator().next().getFileId().equals(file.getFileId())) {
						throw new IOException("Upload is unfinished:" + fileToBackup.getName());
					}
				}
			}
		} catch (Exception ex) {
			LOG.error("Backup error single file:" + fileToBackup.getName(), ex);
		}
	}

	private String environmentIdentifier() throws UnknownHostException {
		return StringUtils.defaultString(InetAddress.getLocalHost().getHostName(), "host") + "."
				+ StringUtils.defaultString(System.getProperty("user.name"), "user");
	}

	private ExecutorService getExecutor() {
		if (executor == null) {
			executor = Executors.newFixedThreadPool(5,
					B2ExecutorUtils.createThreadFactory("HomeBackblazeBackup" + "-%d"));
		}
		return executor;
	}

}
