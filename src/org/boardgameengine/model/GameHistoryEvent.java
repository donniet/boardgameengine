package org.boardgameengine.model;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable
public class GameHistoryEvent {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private Game game;
	
	@Persistent
	private Date eventDate = new Date();
	
	protected GameHistoryEvent(Game game) {
		this.game = game;
	}
	public GameHistoryEvent() {
		game = null;
		eventDate = new Date();
	}	

	public void setEventDate(Date eventDate_) {
		this.eventDate = eventDate_;
	}

	public Date getEventDate() {
		return eventDate;
	}
}
