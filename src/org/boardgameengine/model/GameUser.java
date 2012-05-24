package org.boardgameengine.model;

import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.boardgameengine.config.Config;
import org.boardgameengine.persist.PMF;
import org.boardgameengine.persist.PersistenceCommand;
import org.boardgameengine.persist.PersistenceCommandException;
import org.mozilla.javascript.ScriptableObject;

import sun.security.provider.MD5;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;

@PersistenceCapable
public class GameUser extends ScriptableObject {
	private static final long serialVersionUID = 4649802893267737141L;

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
		
	@Persistent
	private User user;
			
	public GameUser() {}
	
	private GameUser(final User user) {
		setUser(user);
	}
	
	public static GameUser findByKey(final Key key) {
		GameUser ret = null;
		
		try {
			ret = (GameUser)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					return pm.getObjectById(GameUser.class, key);
				}
			});
		}
		catch(PersistenceCommandException e) {
			e.printStackTrace();
			ret = null;
		}
				
		return ret;
	}
	
	public static GameUser findByHashedUserId(final String hashedUserId) {
		//String key = Config.getInstance().decryptString(hashedUserId);
		Key key = KeyFactory.stringToKey(hashedUserId);
		
		return findByKey(key);		
	}
	
	public static GameUser findOrCreateGameUserByUser(final User user) {
		GameUser ret = null;
		
		try {
			ret = (GameUser)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					GameUser ret = null;
					
					Query q = pm.newQuery(GameUser.class);
					q.setFilter("user == userIn");
					q.declareParameters(User.class.getName() + " userIn");
					List<GameUser> results = (List<GameUser>)q.execute(user);
					
					if(results.size() > 0) {
						ret = results.get(0);
					}
					else {
						// ok it must not exist, make one...
						ret = new GameUser(user);
						pm.makePersistent(ret);
					}
					
					return ret;
				}
			});
		}
		catch(PersistenceCommandException e) {
			e.printStackTrace();
			ret = null;
		}
		
		return ret;
	}
	
	public String getHashedEmail() {
		String email = user.getEmail().trim().toLowerCase();
		
		return Config.getInstance().getMD5Hash(email);
	}
	
	public Key getKey() { return key; }
	public User getUser() { return user; }
	public void setUser(final User user) { 
		this.user = user;
	}
	public String getHashedUserId() {
		return KeyFactory.keyToString(key);
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