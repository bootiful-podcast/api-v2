package fm.bootifulpodcast.integration;

import fm.bootifulpodcast.integration.events.SearchIndexInvalidatedEvent;
import fm.bootifulpodcast.integration.events.SiteIndexInvalidatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Slf4j
@RestController
@CrossOrigin(originPatterns = "*")
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
