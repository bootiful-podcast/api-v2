package integration.self;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
class ServerUriResolverConfiguration {

	@Bean
	@Profile({ "default", "ci" })
	LocalhostServerUriResolver localhostServerUriResolver() {
		return new LocalhostServerUriResolver();
	}

	// todo make this better! this should actually talk to the API, get the current
	// endpoint, etc
	@Bean
	@Profile("cloud")
	// @ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
	LocalhostServerUriResolver kubernetesServerUriResolver(ObjectMapper om) {
		return new LocalhostServerUriResolver();
	}

}
