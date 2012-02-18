package org.boardgameengine.model;

import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.boardgameengine.persist.PMF;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

@PersistenceCapable
public class GameType {
	
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String typeName;
	
	@Persistent
	private Key creator;
	
	@Persistent
	private String description;
	
	@Persistent
	private Blob stateChart;

	public Key getKey() {
		return key;
	}
	
	public static GameType findByTypeName(String typeName) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		Query q = pm.newQuery(GameType.class);
		q.setFilter("typeName == typeNameIn");
		q.declareParameters("String typeNameIn");
		
		List<GameType> results = (List<GameType>)q.execute(typeName);
		if(results.size() > 0) {
			return results.get(0);
		}
		else {
			return null;
		}
	}
	public static GameType findByKey(Key key) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		Query q = pm.newQuery(GameType.class);
		q.setFilter("key == keyIn");
		q.declareParameters(Key.class.getName() + " keyIn");
		
		List<GameType> results = (List<GameType>)q.execute(key);
		if(results.size() > 0) {
			return results.get(0);
		}
		else {
			return null;
		}
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setStateChart(byte[] stateChart) {
		this.stateChart = new Blob(stateChart);
	}

	public byte[] getStateChart() {
		return stateChart.getBytes();
	}

	public void setCreator(GameUser creator) {		
		this.creator = creator.getKey();
	}

	public GameUser getCreator() {
		return GameUser.findByKey(this.creator);
	}
	
	
	
}
