package integration;

import fm.bootifulpodcast.rabbitmq.RabbitMqHelper;
import integration.aws.AwsS3Service;
import integration.database.PodcastRepository;
import integration.self.ServerUriResolver;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.style.ToStringCreator;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Log4j2
@SpringBootApplication
@EnableConfigurationProperties(PipelineProperties.class)
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

	// todo remove this if rabbitmq-utilities is added back to the classpath
	@Bean
	RabbitMqHelper rabbitMqHelper(AmqpAdmin amqpAdmin) {
		return new RabbitMqHelper(amqpAdmin);
	}

	// todo remove this
	@Bean
	CachingConnectionFactory rabbitConnectionFactoryPreferred(RabbitProperties properties,
			ObjectProvider<ConnectionNameStrategy> connectionNameStrategy) throws Exception {

		PropertyMapper map = PropertyMapper.get();
		CachingConnectionFactory factory = new CachingConnectionFactory(
				getRabbitConnectionFactoryBean(properties).getObject());
		map.from(properties::determineAddresses).to(factory::setAddresses);
		map.from(properties::isPublisherReturns).to(factory::setPublisherReturns);
		map.from(properties::getPublisherConfirmType).whenNonNull().to(factory::setPublisherConfirmType);
		RabbitProperties.Cache.Channel channel = properties.getCache().getChannel();
		map.from(channel::getSize).whenNonNull().to(factory::setChannelCacheSize);
		map.from(channel::getCheckoutTimeout).whenNonNull().as(Duration::toMillis)
				.to(factory::setChannelCheckoutTimeout);
		RabbitProperties.Cache.Connection connection = properties.getCache().getConnection();
		map.from(connection::getMode).whenNonNull().to(factory::setCacheMode);
		map.from(connection::getSize).whenNonNull().to(factory::setConnectionCacheSize);
		map.from(connectionNameStrategy::getIfUnique).whenNonNull().to(factory::setConnectionNameStrategy);
		return factory;
	}

	private RabbitConnectionFactoryBean getRabbitConnectionFactoryBean(RabbitProperties properties) throws Exception {
		System.out.println(properties.toString());
		PropertyMapper map = PropertyMapper.get();
		RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
		map.from(properties::determineHost).whenNonNull().to(factory::setHost);
		map.from(properties::determinePort).to(factory::setPort);
		map.from(properties::determineUsername).whenNonNull().to(factory::setUsername);
		map.from(properties::determinePassword).whenNonNull().to(factory::setPassword);
		map.from(properties::determineVirtualHost).whenNonNull().to(factory::setVirtualHost);
		map.from(properties::getRequestedHeartbeat).whenNonNull().asInt(Duration::getSeconds)
				.to(factory::setRequestedHeartbeat);
		map.from(properties::getRequestedChannelMax).to(factory::setRequestedChannelMax);
		RabbitProperties.Ssl ssl = properties.getSsl();
		if (ssl.determineEnabled()) {
			factory.setUseSSL(true);
			map.from(ssl::getAlgorithm).whenNonNull().to(factory::setSslAlgorithm);
			map.from(ssl::getKeyStoreType).to(factory::setKeyStoreType);
			map.from(ssl::getKeyStore).to(factory::setKeyStore);
			map.from(ssl::getKeyStorePassword).to(factory::setKeyStorePassphrase);
			map.from(ssl::getTrustStoreType).to(factory::setTrustStoreType);
			map.from(ssl::getTrustStore).to(factory::setTrustStore);
			map.from(ssl::getTrustStorePassword).to(factory::setTrustStorePassphrase);
			map.from(ssl::isValidateServerCertificate)
					.to((validate) -> factory.setSkipServerCertificateValidation(!validate));
			map.from(ssl::getVerifyHostname).to(factory::setEnableHostnameVerification);
		}
		map.from(properties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(factory::setConnectionTimeout);
		factory.afterPropertiesSet();

		var tsc = new ToStringCreator(properties);
		System.out.println(tsc.toString());
		System.out.println("addresses: " + properties.determineAddresses());
		System.out.println("host: " + properties.determineHost());
		System.out.println("pw: " + properties.determinePassword());
		System.out.println("username : " + properties.determineUsername());
		System.out.println("vhost: " + properties.determineVirtualHost());
		System.out.println("port: " + properties.determinePort());

		return factory;
	}

	@Bean
	RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}

	@Bean
	PipelineService pipelineService(PodcastRepository repository, AwsS3Service s3, ServerUriResolver resolver,
			Step1UnproducedPipelineIntegrationConfiguration left, Step1PreproducedIntegrationConfiguration right) {
		return new PipelineService(left.unproducedPipelineMessageChannel(), right.preproducedPipelineMessageChannel(),
				s3, repository, resolver);
	}

}
