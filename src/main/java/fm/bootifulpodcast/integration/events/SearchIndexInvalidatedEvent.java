package fm.bootifulpodcast.integration.events;

import org.springframework.context.ApplicationEvent;

import java.util.Date;

/**
 * Emitted whenever the Lucene index needs to be regenerated
 */

public class SearchIndexInvalidatedEvent extends ApplicationEvent {

	public SearchIndexInvalidatedEvent(Date date) {
		super(date);
	}

	@Override
	public Date getSource() {
		return (Date) super.getSource();
	}

}
