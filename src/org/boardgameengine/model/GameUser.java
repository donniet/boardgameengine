package org.boardgameengine.model;

import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.boardgameengine.config.Config;
import org.boardgameengine.persist.PMF;
import org.mozilla.javascript.ScriptableObject;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

@PersistenceCapable
public class GameUser extends ScriptableObject {
	private static final long serialVersionUID = 4649802893267737141L;

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private User user;
	
	@Persistent
	private String userId;
	
	@NotPersistent
	transient private String hashedUserId;
	
	@Persistent
	private byte[] salt;
	
	// crypto stuff...
	private void createSalt() {
		salt = new byte[8];
		Config.getInstance().getRandom().nextBytes(salt);
	}
		
	public GameUser() {
		createSalt();
	}
	
	private GameUser(final User user) {
		createSalt();
		setUser(user);
	}
	
	public static GameUser findByKey(final Key key) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		GameUser ret = null;
		
		Query q = pm.newQuery(GameUser.class);
		q.setFilter("key == keyIn");
		q.declareParameters(Key.class.getName() + " keyIn");
		List<GameUser> results = (List<GameUser>)q.execute(key);
		
		if(results != null && results.size() > 0) {
			ret = results.get(0);
		}
		
		return ret;
	}
	
	public static GameUser findOrCreateGameUserByUser(final User user) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		GameUser ret = null;
		
		Query q = pm.newQuery(GameUser.class);
		q.setFilter("user == userIn");
		q.declareParameters(User.class.getName() + " userIn");
		List<GameUser> results = (List<GameUser>)q.execute(user);
		
		if(results.size() > 0) {
			ret = results.get(0);
			pm.close();
			ret.hashedUserId = ret.createHashedUserId();
			
			return ret;
		}
		/*
		// next try by userId
		q = pm.newQuery(GameUser.class);
		q.setFilter("userId == userIdIn");
		q.declareParameters("String userIn");
		results = (List<GameUser>)q.execute(user.getUserId());
		
		if(results.size() > 0) {
			ret = results.get(0);
			ret.setUser(user);
			pm.makePersistent(ret);
			pm.close();
			
			return ret;
		}
		*/
		
		// ok it must not exist, make one...
		ret = new GameUser(user);
		pm.makePersistent(ret);
		pm.close();
		
		return ret;
	}
	
	public Key getKey() { return key; }
	public User getUser() { return user; }
	public void setUser(final User user) { 
		this.user = user;
		this.userId = user.getUserId();
		this.hashedUserId = createHashedUserId();
	}
	public String getHashedUserId() {
		if(hashedUserId == null) {
			hashedUserId = createHashedUserId();
		}
		return hashedUserId;
	}
	
	public String createHashedUserId() {
		return Config.getInstance().getHash(user.getUserId(), salt);
	}
	
	// scriptableobject stuff...
	public String jsGet_hashedUserId() {
		return getHashedUserId();
	}

	@Override
	public String getClassName() {
		return "GameUser";
	}
}