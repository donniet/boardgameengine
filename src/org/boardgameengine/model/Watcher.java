package org.boardgameengine.model;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Persistent;

import org.apache.commons.codec.binary.Base64;
import org.boardgameengine.config.Config;
import org.boardgameengine.persist.PMF;
import org.boardgameengine.persist.PersistenceCommand;
import org.boardgameengine.persist.PersistenceCommandException;
import org.mozilla.javascript.ScriptableObject;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@PersistenceCapable
public class Watcher extends ScriptableObject {
	private static final long serialVersionUID = 1L;

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String channelkey;
	
	@Persistent
	private Key game;
	
	@Persistent
	private Key gameUser;
		
	public Watcher() {}
	
	public Watcher(Game gameIn, GameUser gameUserIn) {
		game = gameIn.getKey();
		setGameUser(gameUserIn);
		
		channelkey = hashGameAndGameUser(gameIn, gameUserIn);
	}
	
	public static Watcher findWatcherByChannelKey(final String channelkeyIn) {
		Watcher ret = null;
		
		try {
			ret = (Watcher)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					Query q = pm.newQuery(Watcher.class);
					q.setFilter("channelkey == channelkeyIn");
					q.declareParameters(String.class.getName() + " channelkeyIn");
					List<Watcher> results = (List<Watcher>)q.execute(channelkeyIn);
					
					if(results.size() > 0) {
						return results.get(0);
					}
					else {
						return null;
					}
				}
			});
		}
		catch(PersistenceCommandException e) {
			//TODO: handle this exception
			e.printStackTrace();
		}
		
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Watcher> findWatchersByGame(final Game gameIn) {
		List<Watcher> ret = null;
		
		try {
			ret = (List<Watcher>)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					Query q = pm.newQuery(Watcher.class);
					q.setFilter("game == gameIn");
					q.declareParameters(Key.class.getName() + " gameIn");
					List<Watcher> results = (List<Watcher>)q.execute(gameIn.getKey());
					
					return results;
				}
			});
		}
		catch(PersistenceCommandException e) {
			//TODO: handle this exception
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public static Watcher findWatcherByGameAndGameUser(final Game gameIn, final GameUser gameUserIn) {
		Watcher ret = null;
		
		try {
			ret = (Watcher)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					Query q = pm.newQuery(Watcher.class);
					q.setFilter("game == gameIn && gameUser == gameUserKeyIn");
					q.declareParameters(Key.class.getName() + " gameIn, " + Key.class.getName() + " gameUserKeyIn");
					List<Watcher> results = (List<Watcher>)q.execute(gameIn.getKey(), gameUserIn.getKey());
					
					if(results.size() > 0) {
						Watcher w = results.get(0);
						pm.makeTransient(w);
						return w;
					}
					else {
						return null;
					}
				}
			});
		}
		catch(PersistenceCommandException e) {
			//TODO: handle this exception
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public static String hashGameAndGameUser(final Game gameIn, final GameUser gameUserIn) {
		Config config = Config.getInstance();
		
		String ret = "";
		
		try {
			MessageDigest digest = MessageDigest.getInstance(config.getDigestAlgorithm());
			digest.reset();
			// salt with the class name
			digest.update(Watcher.class.getName().getBytes(config.getEncoding()));
			digest.update(KeyFactory.keyToString(gameIn.getKey()).getBytes(config.getEncoding()));
			digest.update(KeyFactory.keyToString(gameUserIn.getKey()).getBytes(config.getEncoding()));
			byte[] out = digest.digest();
			ret = Base64.encodeBase64URLSafeString(out);
		}
		catch(UnsupportedEncodingException e) {
			//TODO: handle exception
			e.printStackTrace();
		} 
		catch (NoSuchAlgorithmException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}
	
	public Key getGameUserKey() {
		return gameUser;
	}

	public GameUser getGameUser() {
		GameUser ret = null;
		
		try {
			ret = (GameUser)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					GameUser gu = pm.getObjectById(GameUser.class, gameUser);
					if(gu != null) pm.makeTransient(gu);
					return gu;
				}
			});
		}
		catch(PersistenceCommandException e) {
			e.printStackTrace();
			ret = null;
		}
		
		return ret;
	}
	
	public void setGameUser(GameUser gu) {
		gameUser = gu.getKey();
	}
	
	public Key getGameKey() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game.getKey();
	}
	
	public void makePersistent() {
		final Watcher watcher = this;
		try {
			PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					pm.makePersistent(watcher);
					return null;
				}
			});
		}
		catch(PersistenceCommandException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getClassName() {
		return "Watcher";
	}

	public String getChannelkey() {
		return channelkey;
	}

}
