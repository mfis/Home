package homecontroller.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
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
import com.backblaze.b2.client.exceptions.B2Exception;
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

	public synchronized void backup(Path pathToBackup) {

		File fileToBackup = pathToBackup.toFile();
		B2StorageClient client = null;

		try {
			String environmentIdentifier = StringUtils.defaultString(InetAddress.getLocalHost().getHostName(),
					"host") + "." + StringUtils.defaultString(System.getProperty("user.name"), "user");

			// Getting credentials
			String applicationkeyid = env.getProperty("backup.backblaze.applicationkeyid");
			String applicationkey = env.getProperty("backup.backblaze.applicationkey");
			String bucketid = env.getProperty("backup.backblaze.bucketid");

			HttpClientFactory httpClientFactory = HttpClientFactoryImpl.builder().build();
			client = B2StorageHttpClientBuilder.builder(applicationkeyid, applicationkey, "HomeController")
					.setHttpClientFactory(httpClientFactory).build();

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
				FileUtils.deleteQuietly(fileToBackup);
			}
		} catch (B2Exception | IOException ex) {
			LOG.error("Backup error:" + fileToBackup.getName(), ex);
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}

	private ExecutorService getExecutor() {
		if (executor == null) {
			executor = Executors.newFixedThreadPool(2,
					B2ExecutorUtils.createThreadFactory("HomeBackblazeBackup" + "-%d"));
		}
		return executor;
	}

}
