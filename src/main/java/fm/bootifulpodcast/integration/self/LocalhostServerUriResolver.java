package fm.bootifulpodcast.integration.self;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

import java.net.InetAddress;
import java.net.URI;

@Slf4j
class LocalhostServerUriResolver extends AbstractServerUriResolver
		implements ServerUriResolver, ApplicationListener<WebServerInitializedEvent> {

	private int port;

	private String host;

	@Override
	@SneakyThrows
	public void onApplicationEvent(WebServerInitializedEvent event) {
		var localHost = InetAddress.getLocalHost();
		this.host = localHost.getHostName();
		this.port = event.getWebServer().getPort();
	}

	@Override
	public URI resolveCurrentRootUri() {
		return this.buildUriFor(this.host + ':' + this.port);
	}

}
