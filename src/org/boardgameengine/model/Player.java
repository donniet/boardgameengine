package org.boardgameengine.model;

import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.boardgameengine.persist.PMF;
import org.mozilla.javascript.ScriptableObject;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

@PersistenceCapable
public class Player extends ScriptableObject {

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
		
	@Persistent
	private Key gameUserKey;
	
	@Persistent
	private Game game;
	
	transient private GameUser gameUser = null;
	
	@Persistent
	private String role;
	
	public Player() {}
	
	protected Player(Game game, GameUser gameUser, String role) {
		this.game = game;
		this.gameUserKey = gameUser.getKey();
		this.gameUser = gameUser;
		this.role = role;
	}
		
	public Key getKey() { return key; }
	public GameUser getGameUser() {
		if(gameUser == null) {
			PersistenceManager pm = PMF.getInstance().getPersistenceManager();
			
			Query q = pm.newQuery(GameUser.class);
			q.setFilter("key == keyIn");
			q.declareParameters(Key.class.getName() + " keyIn");
			List<GameUser> results = (List<GameUser>)q.execute(gameUserKey);
			
			if(results.size() > 0)
				gameUser = results.get(0);
		}
		
		return gameUser; 
	}
	
	public String getRole() {
		return role;
	}
	
	//scriptable object stuff
	@Override
	public String getClassName() {
		return "Player";
	}
	
	public GameUser jsGet_gameUser() {
		return getGameUser();
	}
	
	public String jsGet_role() {
		return getRole();
	}
	
	
	
}
