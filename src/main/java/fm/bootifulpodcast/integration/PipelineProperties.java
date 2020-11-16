package fm.bootifulpodcast.integration;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "podcast.pipeline")
public class PipelineProperties {

	private final SiteGenerator siteGenerator = new SiteGenerator();

	private File root;

	private S3 s3 = new S3();

	private Processor processor = new Processor();

	private Notifications notifications = new Notifications();

	private Podbean podbean = new Podbean();

	@Data
	public static class SiteGenerator {

		private String requestsQueue = "site-generator-requests";

		private String requestsExchange = this.requestsQueue;

		private String requestsRoutingKey = this.requestsQueue;

	}

	@Data
	public static class Podbean {

		private String requestsQueue = "podbean-requests";

		private String requestsExchange = this.requestsQueue;

		private String requestsRoutingKey = this.requestsQueue;

		private File podbeanDirectory;

		/***
		 * Should the episodes be mark
		 */
		private boolean publishPublicly = false;

	}

	@Data
	public static class Notifications {

		private String fromEmail;

		private String toEmail;

		private String subject;

	}

	@Data
	public static class S3 {

		private String inputBucketName = "podcast-input-bucket-development";

		private String outputBucketName = "podcast-output-bucket-development";

		private File stagingDirectory;

	}

	@Data
	public static class Processor {

		private File inboundPodcastsDirectory;

		// private String inputBucketName = "podcast-input-bucket-development";

		// todo this needs to be changed here AND in the Python Processor code
		private String requestsQueue = "podcast-processor-requests";

		private String requestsExchange = this.requestsQueue;

		private String requestsRoutingKey = this.requestsQueue;

		// todo this needs to be changed here AND in the Python Processor code
		private String repliesQueue = "podcast-processor-replies";

		private String repliesExchange = this.repliesQueue;

		private String repliesRoutingKey = this.repliesQueue;

	}

}
