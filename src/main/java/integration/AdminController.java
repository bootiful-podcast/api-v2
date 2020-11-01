package integration;

import integration.events.SearchIndexInvalidatedEvent;
import integration.events.SiteIndexInvalidatedEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Log4j2
@RestController
@RequestMapping("/admin")
class AdminController {

	private final MessageChannel siteGenerationChannel;

	private final ApplicationEventPublisher publisher;

	AdminController(Step3PodbeanIntegrationConfiguration configuration, ApplicationEventPublisher publisher) {
		this.siteGenerationChannel = configuration.siteGenerationChannel();
		this.publisher = publisher;
	}

	@DeleteMapping("/caches")
	ResponseEntity<?> invalidateAndRebuildEverything() {
		rebuildSearch();
		rebuildSite();
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/caches/search")
	ResponseEntity<?> rebuildSearch() {
		log.info("rebuilding the search index");
		this.publisher.publishEvent(new SearchIndexInvalidatedEvent(new Date()));
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/caches/site")
	ResponseEntity<?> rebuildSite() {
		log.info("rebuilding the site...");
		this.publisher.publishEvent(new SiteIndexInvalidatedEvent(new Date()));
		return ResponseEntity.ok().build();
	}

}
