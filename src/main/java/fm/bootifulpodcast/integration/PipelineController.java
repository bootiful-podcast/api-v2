package fm.bootifulpodcast.integration;

import fm.bootifulpodcast.integration.database.PodcastRepository;
import fm.bootifulpodcast.integration.utils.CopyUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URI;
import java.util.*;

@Slf4j
@RestController
class PipelineController {

	private final File file;

	private final PipelineService service;

	private final String accessControlAllowOriginHeaderValue = "https://bootifulpodcast.fm";

	private final PodcastRepository podcastRepository;

	private final String processingMessage = "processing";

	private final String audioUploadFinishedMessage = "audio-upload-complete";

	private final String audioProductionFinishedMessage = "audio-complete";

	private final String podbeanUploadFinishedMessage = "podbean-complete";

	private final MediaType photoContentType = MediaType.IMAGE_JPEG;

	private final MediaType audioContentType = MediaType.parseMediaType("audio/mpeg");

	private final PodcastViewService podcastViewService;

	PipelineController(PipelineProperties props, PodcastViewService podcastViewService, PodcastRepository repository,
			PipelineService service) {
		this.file = CopyUtils.ensureDirectoryExists(props.getS3().getStagingDirectory());
		this.service = service;
		this.podcastViewService = podcastViewService;
		this.podcastRepository = repository;
	}

	@GetMapping("/podcasts")
	ResponseEntity<Collection<PodcastView>> all() {
		var all = this.podcastViewService.from(this.podcastRepository.findAll());
		return ResponseEntity.ok(all);
	}

	// todo include this in the security config once were sure everythings moved on
	// without it

	@GetMapping("/podcasts/{uid}/status")
	ResponseEntity<?> getStatusForPodcast(@PathVariable String uid) {
		var byUid = podcastRepository.findByUid(uid);
		var response = byUid.map(podcast -> {

			// three levels of 'done'
			// 1. s3audiouri == null: not done keep polling
			// 2. s3audiouri != null: audio is done, but no images; keep polling
			// 3. s3audiouri and podbeanPhotoUri != null: it's 100% done, they can go now
			// if they want...
			var statusMap = new HashMap<String, String>();
			statusMap.put("status", this.processingMessage);

			log.info(podcast.toString());

			if (null != podcast.getS3AudioUri()) {
				var audioUriForPodcast = service.buildMediaUriForPodcastById(podcast.getId()).toString();
				var up = Map.of("media-url", audioUriForPodcast, "audio-url", audioUriForPodcast, "status",
						this.audioProductionFinishedMessage);
				statusMap.putAll(up);
				log.info("updating status map for s3audiourl");
			}

			if (null != podcast.getPodbeanPhotoUri()) {
				var up = Map.of("photo-url", podcast.getS3PhotoUri(), "status", this.podbeanUploadFinishedMessage);
				statusMap.putAll(up);
				log.info("updating status map for podbeanPhotoUri");
			}

			log.info("returning status: " + statusMap.toString() + " for " + uid);
			return statusMap;
		});
		return response.map(reply -> ResponseEntity.ok().body(reply)).orElse(ResponseEntity.noContent().build());
	}

	private String buildAccessControlAllowOriginHeader(RequestEntity<?> requestEntity) {
		var localhost = "localhost:9090";
		var bootifulPodcastFmHost = "bootifulpodcast.fm";
		var list = new ArrayList<>(
				requestEntity.getHeaders().getOrDefault(HttpHeaders.REFERER.toLowerCase(), new ArrayList<>()));
		list.add(requestEntity.getHeaders().getOrigin());
		return list//
				.stream()//
				.filter(Objects::nonNull)//
				.map(String::toLowerCase)//
				.filter(host -> host.contains(localhost) || host.contains(bootifulPodcastFmHost))//
				.map(host -> (host.contains(localhost)) ? "http://" + localhost
						: this.accessControlAllowOriginHeaderValue)//
				.findFirst()//
				.orElse(this.accessControlAllowOriginHeaderValue);
	}

	@SneakyThrows
	@GetMapping("/podcasts/{uid}/profile-photo")
	ResponseEntity<Resource> getProfilePhotoMedia(RequestEntity<?> requestEntity, @PathVariable String uid) {
		var podcastPhotoMedia = service.getPodcastPhotoMedia(uid);
		return ResponseEntity.ok()//
				.header("X-Podcast-UID", uid)//
				.header(HttpHeaders.ACCEPT_RANGES, "none")//
				.contentType(this.photoContentType)//
				.contentLength(podcastPhotoMedia.contentLength())
				.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, buildAccessControlAllowOriginHeader(requestEntity))
				.body(podcastPhotoMedia);
	}

	@SneakyThrows
	@GetMapping({ "/podcasts/{uid}/produced-audio", "/podcasts/{uid}/produced-audio.mp3" })
	ResponseEntity<Resource> getProducedAudioMedia(RequestEntity<?> requestEntity, @PathVariable String uid) {
		var podcastAudioMedia = service.getPodcastAudioMedia(uid);
		return ResponseEntity.ok()//
				.header("X-Podcast-UID", uid)//
				.contentType(this.audioContentType)//
				.contentLength(podcastAudioMedia.contentLength())//
				.header(HttpHeaders.ACCEPT_RANGES, "none")//
				.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, buildAccessControlAllowOriginHeader(requestEntity))
				.body(podcastAudioMedia);
	}

	@PostMapping("/podcasts/{uid}")
	ResponseEntity<?> beginProduction(@PathVariable("uid") String uid, @RequestParam("file") MultipartFile file)
			throws Exception {
		var newFile = new File(this.file, uid);
		file.transferTo(newFile);

		CopyUtils.assertFileExists(newFile);
		log.info("the newly POST'd file lives at " + newFile.getAbsolutePath() + '.');
		Assert.isTrue(this.service.launchProcessorPipeline(uid, newFile), "the pipeline says no.");
		var location = URI.create("/podcasts/" + uid + "/status");
		log.info("sending status location as : '" + location + "'");
		return ResponseEntity.accepted().location(location)
				.body(Collections.singletonMap("status-url", location.toString()))
		/* .build() */;// 202
	}

}
