package fm.bootifulpodcast.integration.events;

import fm.bootifulpodcast.integration.PodcastPackageManifest;
import org.springframework.context.ApplicationEvent;

public class PodcastArchiveUploadedEvent extends ApplicationEvent {

	public PodcastArchiveUploadedEvent(PodcastPackageManifest source) {
		super(source);
	}

	@Override
	public PodcastPackageManifest getSource() {
		return (PodcastPackageManifest) super.getSource();
	}

}
