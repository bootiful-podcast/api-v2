package fm.bootifulpodcast.integration;

import fm.bootifulpodcast.integration.aws.AwsS3Service;
import fm.bootifulpodcast.integration.database.PodcastRepository;
import fm.bootifulpodcast.integration.self.ServerUriResolver;
import fm.bootifulpodcast.rabbitmq.RabbitMqHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@Log4j2
@SpringBootApplication
@EnableConfigurationProperties(PipelineProperties.class)
@RequiredArgsConstructor
public class ApiApplication {

	public static void main(String[] args) {

		SpringApplication.run(ApiApplication.class, args);
	}

	@Bean
	InitializingBean initializingBean(PipelineProperties pipelineProperties) {
		return () -> {
			var publishPublicly = pipelineProperties.getPodbean().isPublishPublicly();
			log.info("podbean.publishPublicly: " + publishPublicly);
		};
	}

	// todo remove this if rabbitmq-utilities is added back to the classpath
	@Bean
	RabbitMqHelper rabbitMqHelper(AmqpAdmin amqpAdmin) {
		return new RabbitMqHelper(amqpAdmin);
	}

	@Bean
	RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}

	@Bean
	PipelineService pipelineService(PipelineProperties pipelineProperties, RabbitMqHelper helper,
			PodcastRepository repository, AwsS3Service s3, ServerUriResolver resolver,
			Step1UnproducedPipelineIntegrationConfiguration left, Step1PreproducedIntegrationConfiguration right) {

		log.info("initializing PipelineService: 1. Declare required RabbitMQ bindings");
		helper.defineDestination(pipelineProperties.getSiteGenerator().getRequestsExchange(),
				pipelineProperties.getSiteGenerator().getRequestsQueue(),
				pipelineProperties.getSiteGenerator().getRequestsRoutingKey());

		helper.defineDestination(pipelineProperties.getPodbean().getRequestsExchange(),
				pipelineProperties.getPodbean().getRequestsQueue(),
				pipelineProperties.getPodbean().getRequestsRoutingKey());

		helper.defineDestination(pipelineProperties.getProcessor().getRequestsExchange(),
				pipelineProperties.getProcessor().getRequestsQueue(),
				pipelineProperties.getProcessor().getRequestsRoutingKey());

		helper.defineDestination(pipelineProperties.getProcessor().getRepliesExchange(),
				pipelineProperties.getProcessor().getRepliesQueue(),
				pipelineProperties.getProcessor().getRepliesRoutingKey());

		log.info("initializing PipelineService: 2. Build PipelineService");
		return new PipelineService(left.unproducedPipelineMessageChannel(), right.preproducedPipelineMessageChannel(),
				s3, repository, resolver);
	}

}
