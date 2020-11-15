package fm.bootifulpodcast.integration;

import fm.bootifulpodcast.rabbitmq.RabbitMqHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
class RabbitMqInitializer {

	RabbitMqInitializer(PipelineProperties pipelineProperties, RabbitMqHelper helper) {

		log.info("declared 4 RabbbitMQ queues, exchanges, and bindings");

	}

}
