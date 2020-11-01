package integration.events;

import org.springframework.context.ApplicationEvent;

import java.util.Date;

/**
 * Emitted whenever the site needs to be re-generated
 */
public class SiteIndexInvalidatedEvent extends ApplicationEvent {

	public SiteIndexInvalidatedEvent(Date date) {
		super(date);
	}

	@Override
	public Date getSource() {
		return (Date) super.getSource();
	}

}
