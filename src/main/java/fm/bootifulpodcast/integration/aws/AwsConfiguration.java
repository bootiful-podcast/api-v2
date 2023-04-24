package fm.bootifulpodcast.integration.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import fm.bootifulpodcast.integration.PipelineProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
class AwsConfiguration {

	@Bean
	AwsS3Service awsS3Service(PipelineProperties properties, AmazonS3 s3) {
		var s3Properties = properties.getS3();
		return new AwsS3Service(s3Properties.getInputBucketName(), s3Properties.getOutputBucketName(), s3);
	}

	@Bean
	AmazonS3 amazonS3(@Value("${AWS_ACCESS_KEY}") String accessKey, @Value("${AWS_ACCESS_KEY_SECRET}") String secret,
			@Value("${AWS_REGION}") String region) {
		log.info("connecting to " + region);
		var credentials = new BasicAWSCredentials(accessKey, secret);
		var timeout = 5 * 60 * 1000;
		var clientConfiguration = new ClientConfiguration().withClientExecutionTimeout(timeout)
				.withConnectionMaxIdleMillis(timeout).withConnectionTimeout(timeout).withConnectionTTL(timeout)
				.withRequestTimeout(timeout);

		return AmazonS3ClientBuilder.standard().withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.fromName(region))
				.build();
	}

}
