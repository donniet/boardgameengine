package org.boardgameengine.model;

import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.boardgameengine.persist.PMF;
import org.boardgameengine.persist.PersistenceCommand;
import org.boardgameengine.persist.PersistenceCommandException;
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
	private boolean connected = false;
	
	@Persistent
	private Game game;
	
	@NotPersistent
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
			try {
				gameUser = (GameUser)PMF.executeCommandInTransaction(new PersistenceCommand() {
					@Override
					public Object exec(PersistenceManager pm) {
						GameUser ret = pm.getObjectById(GameUser.class, gameUserKey);
						if(ret != null)
							pm.makeTransient(ret);
						return ret;
					}
				});
			}
			catch(PersistenceCommandException e) {
				e.printStackTrace();
				gameUser = null;
			}			
		}
		
		return gameUser; 
	}
	
	public String getRole() {
		return role;
	}
	
	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	
	public void makePersistent() {
		final Player persist = this; 
		try {
			PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					pm.makePersistent(persist);
					return null;
				}
			});
		}
		catch(PersistenceCommandException e) {
			e.printStackTrace();
		}
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
