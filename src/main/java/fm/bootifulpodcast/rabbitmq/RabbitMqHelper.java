package fm.bootifulpodcast.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMqHelper {

	private final AmqpAdmin amqpAdmin;

	public void defineDestination(String exchange, String queue, String routingKey) {

		Queue q = this.queue(queue);
		amqpAdmin.declareQueue(q);

		Exchange e = this.exchange(exchange);
		amqpAdmin.declareExchange(e);

		Binding b = this.binding(q, e, routingKey);
		amqpAdmin.declareBinding(b);
	}

	public Exchange exchange(String requestExchange) {
		var e = ExchangeBuilder.topicExchange(requestExchange).durable(true).build();
		return e;
	}

	public Queue queue(String requestsQueue) {
		var q = QueueBuilder.durable(requestsQueue).build();
		q.setShouldDeclare(true);
		return q;
	}

	public Binding binding(Queue q, Exchange e, String routingKey) {
		var b = BindingBuilder.bind(q).to(e).with(routingKey).noargs();
		b.setShouldDeclare(true);
		return b;
	}

}
