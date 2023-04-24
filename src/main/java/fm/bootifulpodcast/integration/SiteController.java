package fm.bootifulpodcast.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.templates.MarkdownService;
import fm.bootifulpodcast.integration.database.Podcast;
import fm.bootifulpodcast.integration.database.PodcastRepository;
import fm.bootifulpodcast.integration.events.PodcastPublishedToPodbeanEvent;
import fm.bootifulpodcast.integration.events.SearchIndexInvalidatedEvent;
import fm.bootifulpodcast.integration.utils.DateUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Provides endpoints designed to support the working of the site
 */
@Slf4j
@RestController
@RequestMapping("/site")
@RequiredArgsConstructor
class SiteController {

	private final ObjectMapper objectMapper;

	private final Map<String, String> mapOfRenderedMarkdown = new ConcurrentHashMap<>();

	private final MarkdownService markdownService;

	private final PodcastRepository repository;

	private final AtomicReference<String> json = new AtomicReference<>();

	@GetMapping(path = "/podcasts", produces = MediaType.APPLICATION_JSON_VALUE)
	String get() {
		return this.json.get();
	}

	@EventListener({ PodcastPublishedToPodbeanEvent.class, ApplicationReadyEvent.class,
			SearchIndexInvalidatedEvent.class })
	public void refresh() {
		log.info("Rebuilding podcast listing in " + this.getClass().getName() + '.');
		var dateFormat = DateUtils.date();
		var podcastList = new ArrayList<Podcast>();
		this.repository.findAll().forEach(podcastList::add);
		var allPodcasts = podcastList//
				.stream()//
				.peek(pr -> this.mapOfRenderedMarkdown.put(pr.getUid(),
						markdownService.convertMarkdownTemplateToHtml(pr.getDescription()).trim()))
				.map(p -> new PodcastRecord(p, "episode-photos/" + p.getUid() + ".jpg", dateFormat.format(p.getDate()),
						this.mapOfRenderedMarkdown.get(p.getUid())))
				.collect(Collectors.toList());
		this.json.set(this.buildJsonForAllPodcasts(allPodcasts));
	}

	private String printJsonString(JsonNode jsonNode) {
		try {
			var json = this.objectMapper.readValue(jsonNode.toString(), Object.class);
			var objectWriter = this.objectMapper.writerWithDefaultPrettyPrinter();
			return objectWriter.writeValueAsString(json);
		}
		catch (Exception e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}
		return null;
	}

	private String buildJsonForAllPodcasts(List<PodcastRecord> allPodcasts) {
		var collect = allPodcasts.stream().map(this::jsonNodeForPodcast).collect(Collectors.toList());
		var arrayNode = this.objectMapper.createArrayNode().addAll(collect);
		return printJsonString(arrayNode);
	}

	private JsonNode jsonNodeForPodcast(PodcastRecord pr) {
		var objectNode = this.objectMapper.createObjectNode();
		objectNode.put("id", Long.toString(pr.getPodcast().getId()));
		objectNode.put("uid", pr.getPodcast().getUid());
		objectNode.put("title", pr.getPodcast().getTitle());
		objectNode.put("date", pr.getPodcast().getDate().getTime());
		objectNode.put("episodePhotoUri", pr.getPodcast().getPodbeanPhotoUri());
		objectNode.put("description", this.mapOfRenderedMarkdown.get(pr.getPodcast().getUid()));
		objectNode.put("dateAndTime", pr.getDateAndTime()); // correct
		objectNode.put("dataAndTime", pr.getDateAndTime()); // does anything else use
		// this?
		// mistaken property?
		objectNode.put("episodeUri", "/podcasts/" + pr.getPodcast().getUid() + "/produced-audio");
		return objectNode;
	}

}

@RequiredArgsConstructor
@Data
class PodcastRecord {

	private final Podcast podcast;

	private final String imageSrc, dateAndTime;

	private final String htmlDescription; // the HTML rendered from the Markdown

}
