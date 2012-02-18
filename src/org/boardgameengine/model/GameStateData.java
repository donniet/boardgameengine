package org.boardgameengine.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable
public class GameStateData {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY) 
	private Key key;
	
	@Persistent
	private String id;
	
	@Persistent
	private Blob value;
	
	@Persistent
	private GameState gameState;
	
	protected Key getKey() {
		return key;
	}
	protected GameStateData(GameState state) {
		setGameState(state);
		value = null;
		id = null;
	}
	public GameStateData() {
		setGameState(null);
		value = null;
		id = null;
	}	

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public byte[] getValue() {
		if(value == null) {
			return null;
		}
		else {
			return value.getBytes();
			
		}
	}
	public void setValue(final byte[] value) {
		if(value == null) {
			this.value = null;
		}
		else {
			this.value = new Blob(value);
		}
	}
	protected void setGameState(GameState gameState) {
		this.gameState = gameState;
	}
	public GameState getGameState() {
		return gameState;
	}
	
	
		
}
