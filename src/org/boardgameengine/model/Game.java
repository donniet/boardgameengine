package org.boardgameengine.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.SCXMLListener;
import org.apache.commons.scxml.Status;
import org.apache.commons.scxml.io.SCXMLParser;
import org.apache.commons.scxml.model.Datamodel;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.SCXML;
import org.apache.commons.scxml.model.State;
import org.apache.commons.scxml.model.Transition;
import org.apache.commons.scxml.model.TransitionTarget;
import org.boardgameengine.config.Config;
import org.boardgameengine.error.GameLoadException;
import org.boardgameengine.persist.PMF;
import org.boardgameengine.scxml.js.JsContext;
import org.boardgameengine.scxml.js.JsEvaluator;
import org.boardgameengine.scxml.semantics.SCXMLGameSemanticsImpl;
import org.mozilla.javascript.ScriptableObject;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.api.utils.SystemProperty;

import flexjson.JSONSerializer;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Extension;


@PersistenceCapable
public class Game extends ScriptableObject {
	
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String gameid;
		
	@Persistent
	private List<Key> watchers;
	
	@Persistent
	private Key gameTypeKey;
	
	//not persistent
	@NotPersistent
	transient private Map<String,String> params;
	
	@NotPersistent
	transient private SCXML scxml;
	
	@NotPersistent
	transient private List<Exception> loadWarnings;
	
	@NotPersistent
	private
	transient SCXMLExecutor exec;
	
	@NotPersistent
	transient private JsEvaluator eval;
	
	@NotPersistent
	transient private JsContext cxt;
	
	@Persistent
	@Element(dependent = "true", mappedBy = "game", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="role asc"))	
	private List<Player> players;
	
	@Persistent
	@Element(dependent = "true", mappedBy = "game", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="eventDate asc"))
	private List<GameHistoryEvent> events;
	
	@Persistent
	@Element(dependent = "true", mappedBy = "game", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="stateDate asc"))
	private List<GameState> states;
	
	private static String nextGameId() {
		return new BigInteger(60, Config.getInstance().getRandom()).toString(32).toUpperCase();
	}
	
	public Game() {
		gameid = nextGameId();
		events = new ArrayList<GameHistoryEvent>();
		states = new ArrayList<GameState>();
		params = new HashMap<String,String>();
		players = new ArrayList<Player>();
		watchers = new ArrayList<Key>();
	}
	
	public Game(GameType gt) throws GameLoadException {
		this();
		
		setGameType(gt);
		init();
	}
	
	private void init() throws GameLoadException {
		//GameType gt = getGameType();
		//InputStream bis = new ByteArrayInputStream(gt.getStateChart());
		InputStream bis = Game.class.getResourceAsStream("/pilgrims.xml");
		
		loadWarnings = new ArrayList<Exception>();
		
		final Log log = LogFactory.getLog(Game.class);
		
		scxml = null;
		try {
			scxml = SCXMLParser.parse(new InputSource(bis), new ErrorHandler() {
				@Override
				public void warning(SAXParseException exception) throws SAXException {
					loadWarnings.add(exception);
				}
				
				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;				
				}
				
				@Override
				public void error(SAXParseException exception) throws SAXException {
					throw exception;							
				}
			});
		} catch (SAXException e) {
			throw new GameLoadException("Fatal parse error.", e);
		} catch (ModelException e) {
			throw new GameLoadException("Fatal parse error.", e);
		} catch (IOException e) {
			throw new GameLoadException("Fatal parse error.", e);
		}

		eval = new JsEvaluator();
		ErrorReporter rep = new ErrorReporter() {
			@Override
			public void onError(String errCode, String errDetail, Object errCtx) {
				// TODO really handle errors here.
				log.error(errCode + ": " + errDetail);
			}
		};
		EventDispatcher disp = new EventDispatcher() {

			@Override
			public void cancel(String sendId) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void send(String sendId, String target, String targetType,
					String event, Map params, Object hints, long delay,
					List externalNodes) {
				// TODO Auto-generated method stub
				
			}
			
		};
		SCXMLListener listener = new SCXMLListener() {

			@Override
			public void onEntry(TransitionTarget state) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onExit(TransitionTarget state) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTransition(TransitionTarget from,
					TransitionTarget to, Transition transition) {
				// TODO Auto-generated method stub
				
			}
			
		};
		
		exec = new SCXMLExecutor(eval, null, rep, new SCXMLGameSemanticsImpl());		
		exec.setStateMachine(scxml);
		
		cxt = (JsContext)exec.getRootContext();
		
		
		try {
			if(states != null && states.size() > 0) {
				GameState gs = states.get(states.size() - 1);
				
				gs.injectInto(cxt);
				
				Set s = exec.getCurrentStatus().getStates();
				Map ms = exec.getStateMachine().getTargets();
				s.clear();
				
				Set<String> ss = gs.getStateSet();
				for(String state : ss) {
					s.add(ms.get(state));
				}				
			}
			else {
				exec.go();
				// record initial state
				GameState gs = new GameState(this);
				
				Set<State> s = exec.getCurrentStatus().getStates();
				for(State state : s) {
					gs.getStateSet().add(state.getId());
				}
				gs.setStateDate(new Date());
				gs.extractFrom(scxml.getDatamodel(), cxt);
				states.add(gs);
				/*
				PersistenceManager pm = PMF.getInstance().getPersistenceManager();
				
				pm.makePersistent(gs);
				*/
			}
		}
		catch(ModelException e) {
			throw new GameLoadException("Could not start the machine.", e);
		}
	}
	
	public GameState persistGameState() {
		// record initial state
		GameState gs = new GameState(this);
		
		Set<State> s = exec.getCurrentStatus().getStates();
		for(State state : s) {
			gs.getStateSet().add(state.getId());
		}
		gs.setStateDate(new Date());
		gs.extractFrom(scxml.getDatamodel(), cxt);
		states.add(gs);
		
		this.makePersistent();
		
		return gs;
	}
	
	public static Game findGameById(String gameid) throws GameLoadException {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		Game ret = null;
		
		Query q = pm.newQuery(Game.class);
		q.setFilter("gameid == gameIdToFind");
		q.declareParameters("String gameIdToFind");
		List<Game> results = (List<Game>)q.execute(gameid.toUpperCase());
		
		
		if(results.size() > 0) {
			ret = results.get(0);
			ret.init();
		}

		pm.close();
		
		return ret;
	}
	
	public SCXMLExecutor getExec() {
		return exec;
	}
	
	public List<GameHistoryEvent> getEvents() {
		return events;
	}
	protected void setGameId(String gameid_) {
		this.gameid = gameid_;
	}
	public String getGameId() {
		return gameid;
	}
	public List<GameState> getStates() {
		return states;
	}
	public Map<String,String> getParameters() {
		return params;
	}
	public Date getCurrentTime() {
		return new Date();
	}
	public void addGameState(Date stateDate, Status currentStatus, Datamodel datamodel, JsContext cxt) {
		GameState s = new GameState(this);
		s.setStateDate(stateDate);
		s.getStateSet().clear();
		s.getStateSet().addAll(currentStatus.getStates());
		s.extractFrom(datamodel, cxt);
		states.add(s);
	}
	public void addEvent(Date eventDate) {
		GameHistoryEvent e = new GameHistoryEvent(this);
		e.setEventDate(eventDate);
		events.add(e);
	}
	public List<Player> getPlayers() {
		return players;
	}
	public List<GameUser> getWatchers() {
		List<GameUser> ret = new ArrayList<GameUser>();
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		Query q = pm.newQuery(GameUser.class);
		q.setFilter("key == keyIn");
		q.declareParameters(Key.class.getName() + " keyIn");
		
		for(Key k : watchers) {
			List<GameUser> l = (List<GameUser>)q.execute(k);
			if(l != null && l.size() > 0) {
				ret.add(l.get(0));
			}
		}
		
		return ret;
	}
	public void addPlayer(User user, String role) {
		GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		players.add(new Player(this, gu, role));
	}
	public void addWatcher(User user) {
		GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		watchers.add(gu.getKey());
	}
	public void setGameType(GameType gt) {
		this.gameTypeKey = gt.getKey();
	}
	public GameType getGameType() {
		return GameType.findByKey(this.gameTypeKey);
	}
	
	public void makePersistent() {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		try {
			pm.makePersistent(this);
			/*
			for(GameState gs : states) {
				pm.makePersistent(gs);
			}
			*/
		}
		catch(Exception e) {
			//TODO: handle this somehow...
			e.printStackTrace();
		}
		finally {		
			pm.close();
		}
	}
	
	public void serialize(Writer out) {
		JSONSerializer json = new JSONSerializer();
		
		json.include("events")
			.include("parameters")
			.include("players")
			.include("watchers")
			.exclude("*.class")
			.exclude("*.className")
			.exclude("*.key")
			.exclude("*.extensible")
			.exclude("*.parentScope")
			.exclude("*.prototype")
			.exclude("*.sealed")
			.exclude("*.typeOf")
			.exclude("*.empty")
			.exclude("*.user")
			.exclude("_ALL_STATES")
			.exclude("states.state")
			.exclude("states.game")
			.exclude("events.game")
			.exclude("players.game")
			.exclude("gameType.stateChart")
			
			.transform(Config.getInstance().getDateTransfomer(), Date.class);
			
		if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
			json.prettyPrint(true);
		else
			json.prettyPrint(false);
					
		json.serialize(this, out);
	}

	@Override
	public String getClassName() {
		return "Game";
	}


}
