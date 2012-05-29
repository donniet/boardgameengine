package org.boardgameengine.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.commons.scxml.TriggerEvent;
import org.apache.commons.scxml.io.SCXMLParser;
import org.apache.commons.scxml.model.CustomAction;
import org.apache.commons.scxml.model.Datamodel;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.SCXML;
import org.apache.commons.scxml.model.State;
import org.apache.commons.scxml.model.Transition;
import org.apache.commons.scxml.model.TransitionTarget;
import org.boardgameengine.config.Config;
import org.boardgameengine.error.GameLoadException;
import org.boardgameengine.persist.PMF;
import org.boardgameengine.persist.PersistenceCommand;
import org.boardgameengine.persist.PersistenceCommandException;
import org.boardgameengine.scxml.js.JsContext;
import org.boardgameengine.scxml.js.JsEvaluator;
import org.boardgameengine.scxml.js.JsFunctionJsonTransformer;
import org.boardgameengine.scxml.model.Error;
import org.boardgameengine.scxml.model.FlagStateAsImportant;
import org.boardgameengine.scxml.semantics.SCXMLGameSemanticsImpl;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.xml.XMLLib;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
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
import javax.xml.bind.annotation.XmlList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


@PersistenceCapable
public class Game extends ScriptableObject implements EventDispatcher, SCXMLListener {
	
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private Key owner;
	
	@Persistent
	private Date created;
		
	@Persistent
	private Key gameTypeKey;
	
	@Persistent(defaultFetchGroup = "true")
	@Element(dependent = "true", mappedBy = "game", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="role asc"))	
	private List<Player> players;
	
	@Persistent(defaultFetchGroup = "true")
	@Element(dependent = "true", mappedBy = "game", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="eventDate asc"))
	private List<GameHistoryEvent> events;
	
	@Persistent(defaultFetchGroup = "false")
	@Element(dependent = "true", mappedBy = "game", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="stateDate asc"))
	private List<GameState> states;
	
	@NotPersistent
	transient private Log log = LogFactory.getLog(Game.class);
	

	@NotPersistent
	transient private boolean isDirty_ = false;
	
	@NotPersistent
	transient private boolean isError_ = false;
	
	@NotPersistent
	transient private boolean isImportant_ = false;
	
	@NotPersistent
	transient private String errorMessage_ = "";
		
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
	
	public Game() {
		events = new ArrayList<GameHistoryEvent>();
		states = new ArrayList<GameState>();
		params = new HashMap<String,String>();
		players = new ArrayList<Player>();
	}
	
	public Game(GameType gt) throws GameLoadException {
		this();
		
		setGameType(gt);
		setCreated(new Date());
		init();
	}
	
	private void init() throws GameLoadException {
		//GameType gt = getGameType();
		//InputStream bis = new ByteArrayInputStream(gt.getStateChart());
		InputStream bis = Game.class.getResourceAsStream("/pilgrims.xml");
		
		loadWarnings = new ArrayList<Exception>();
		
		List<CustomAction> customActions = new ArrayList<CustomAction>();
		customActions.add(new CustomAction("http://www.pilgrimsofnatac.com/schemas/game.xsd", "error", Error.class));
		customActions.add(new CustomAction("http://www.pilgrimsofnatac.com/schemas/game.xsd", "flagStateAsImportant", FlagStateAsImportant.class));
				
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
			}, customActions);
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
		
		exec = new SCXMLExecutor(eval, null, rep, new SCXMLGameSemanticsImpl());
		exec.addListener(scxml, this);
		exec.setEventdispatcher(this);
		exec.setStateMachine(scxml);
		
		cxt = (JsContext)exec.getRootContext();
		cxt.setLocal("game", this);		
		
		try {
			GameState gs = getMostRecentState();
			
			if(gs != null) {				
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
				
				persistGameState(true);
			}
		}
		catch(ModelException e) {
			throw new GameLoadException("Could not start the machine.", e);
		}
		isDirty_ = false;
		
	}
	

	public GameState persistGameState(boolean isImportant) {
		GameState.deleteOldStatesForGame(this);
		
		GameState gs = new GameState(this);
		
		Set<State> s = exec.getCurrentStatus().getStates();
		for(State state : s) {
			gs.getStateSet().add(state.getId());
		}
		gs.setStateDate(new Date());
		gs.extractFrom(scxml.getDatamodel(), cxt);
		gs.setImportant(isImportant_ || isImportant);
		states.add(gs);
		
		makePersistent();
		isDirty_ = false;
		isError_ = false;
		isImportant_ = false;
		
		return gs;
	}

	@Override
	public void cancel(String sendId) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void send(String sendId, String target, String targetType,
			String event, Map params, Object hints, long delay,
			Object content, List externalNodes) {
		
		log.info(String.format("Send Event '%s'", event));
		
		boolean success = false;
		
		if(	targetType.equals("http://www.pilgrimsofnatac.com/schemas/game.xsd#GameEventProcessor") &&
			target.equals("http://www.pilgrimsofnatac.com/schemas/game.xsd#GameEvent")) {
			if(event.equals("game.playerJoined")) {
				String playerid = params.get("playerid").toString();
				String role = params.get("role").toString();
				
				GameUser gameUser = GameUser.findByHashedUserId(playerid);
				
				if(gameUser != null) {
					addPlayer(gameUser, role);
					success = true;
					isError_ = false;
				}
				else {
					isError_ = true;
					errorMessage_ = "invalid player id, likely a problem with the scxml game rules";
				}
			}
			else if(event.equals("game.error")) {
				errorMessage_ = params.get("message").toString();
				log.error("[error]: " + params.get("message"));
				isError_ = true;
			}
			else if(event.equals("game.flagStateAsImportant")) {
				isImportant_ = Boolean.parseBoolean(params.get("important").toString());
			}
			else {
				success = true;
				isError_ = false;
			}
		}
		
		if(success) {
			//persistGameState();
			sendWatcherMessage(event, params, content);
		}
	}
	
	public boolean getIsError() {
		return isError_;
	}
	public String getErrorMessage() {
		return errorMessage_;
	}
		
	public void sendWatcherMessage(String event, Map params, Object content) {
		DocumentBuilderFactory docbuilderfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = docbuilderfactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return;
		}
		Document doc = builder.newDocument();
		
		Node eventNode = doc.createElement("event");
		Node eventName = doc.createAttribute("name");
		eventName.setNodeValue(event);
		eventNode.getAttributes().setNamedItem(eventName);
		
		Set paramsKeys = params.keySet();
		for(Iterator i = paramsKeys.iterator(); i.hasNext(); ) {
			String paramName = (String)i.next();
			
			Node paramNode = doc.createElement("param");
			Node paramNameNode = doc.createAttribute("name");
			paramNameNode.setNodeValue(paramName);
			paramNode.getAttributes().setNamedItem(paramNameNode);
			
			paramNode.setTextContent(params.get(paramName).toString());
			
			eventNode.appendChild(paramNode);
		}
		
		if(content != null) {
			
			Node contentNode = doc.createElement("content");
			Node contentBody = null;
			try {
				contentBody = XMLLibImpl.toDomNode(content);
			}
			catch(IllegalArgumentException e) {
				contentBody = null;
			}
			
			if(contentBody != null) {
				Node imported = doc.importNode(contentBody, true);
				contentNode.appendChild(imported);
			}
			else {
				contentNode.setTextContent(content.toString());
			}
			eventNode.appendChild(contentNode);
		}
		
		doc.appendChild(eventNode);
		
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer trans = null;				
										
		try {
			trans = factory.newTransformer(new StreamSource(Config.getInstance().getDataModelTransformStream()));
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			return;
		}
		
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		
		List<Watcher> watchers = getWatchers();
		for(Watcher w : watchers) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			
			trans.setParameter(Config.getInstance().getDataModelTransformPlayerIdParam(), KeyFactory.keyToString(w.getGameUserKey()));				
						
			try {
				trans.transform(new DOMSource(doc), new StreamResult(bos));
			} catch (TransformerException e) {
				e.printStackTrace();
				continue;
			}	
			
			String strmessage = "";
			
			try {
				strmessage = bos.toString(Config.getInstance().getEncoding());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				continue;
			}
			
			channelService.sendMessage(new ChannelMessage(w.getChannelkey(), strmessage));
		}
	}
	
	public String[] getTransitionEvents() {
		Set<String> ret = new HashSet<String>();
		
		Set<State> s = exec.getCurrentStatus().getStates();
		for(State state : s) {
			List transitions = state.getTransitionsList();
			for(Object o : transitions) {
				Transition t = (Transition)o;
				String event = t.getEvent();
				if(event != null && !event.equals("")) {
					ret.add(event);
				}
			}
		}
		
		return ret.toArray(new String[0]);
	}
	
	public static Game findUninitializedGameByKey(final Key key) {
		Game ret = null;
		
		try {
			ret = (Game)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					return pm.getObjectById(Game.class, key);
				}
			});
		}
		catch(PersistenceCommandException e) {
			e.printStackTrace();
			ret = null;
		}
		
		return ret;
	}
	
	public static Game findGameByKey(Key key) throws GameLoadException {
		
		Game ret = findUninitializedGameByKey(key);
		
		if(ret != null) {
			ret.init();
		}

		return ret;
	}
	
	protected SCXMLExecutor getExec() {
		return exec;
	}
	
	public Set<State> getCurrentStates() {
		return getExec().getCurrentStatus().getStates();
	}
	
	public GameUser getOwner() {
		GameUser ret = null;
		
		try {
			ret = (GameUser)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					return pm.getObjectById(GameUser.class, owner);
				}
			});
		}
		catch(PersistenceCommandException e) {
			ret = null;
		}
		
		return ret;
	}
	public void setOwner(User u) {
		GameUser gu = GameUser.findOrCreateGameUserByUser(u);
		owner = gu.getKey();
	}
	
	public List<GameHistoryEvent> getEvents() {
		return events;
	}
	public GameState getMostRecentState() {
		final Game param = this;
		GameState ret = null;
		
		try {
			ret = (GameState)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					Query q = pm.newQuery(GameState.class);
					q.setFilter("game == gameIn");
					q.setOrdering("stateDate desc");
					q.setRange(0, 1);
					q.declareParameters(Game.class.getName() + " gameIn");
					
					List<GameState> results = (List<GameState>)q.execute(param);
					
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
			e.printStackTrace();
			ret = null;
		}
		
		return ret;
	}
	public Map<String,String> getParameters() {
		return params;
	}
	public Date getCurrentTime() {
		return new Date();
	}
	/*
	public void addGameState(Date stateDate, Status currentStatus, Datamodel datamodel, JsContext cxt, boolean important) {
		
		
		GameState s = new GameState(this);
		s.setStateDate(stateDate);
		s.getStateSet().clear();
		s.getStateSet().addAll(currentStatus.getStates());
		s.extractFrom(datamodel, cxt);
		s.setImportant(important);
		states.add(s);
	}
	*/
	public void addEvent(Date eventDate) {
		GameHistoryEvent e = new GameHistoryEvent(this);
		e.setEventDate(eventDate);
		events.add(e);
	}
	public List<Player> getPlayers() {
		return players;
	}
	private void addPlayer(User user, String role) {
		GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		players.add(new Player(this, gu, role));
	}
	private void addPlayer(GameUser gameUser, String role) {
		Player p = new Player(this, gameUser, role);
		players.add(p);		
	}

	public boolean sendStartGameRequest(User user) {
		isError_ = false;
		
		GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		
		if(gu.getKey().compareTo(owner) != 0) {
			return false;
		}
		
		DocumentBuilderFactory docbuilderfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = docbuilderfactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return false;
		}
		Document doc = builder.newDocument();
		
		Node player = doc.createElementNS(Config.getInstance().getGameEngineNamespace(), "player");
		player.appendChild(doc.createTextNode(gu.getHashedUserId()));
		doc.appendChild(player);
				
		try {
			getExec().triggerEvent(new TriggerEvent("game.startGame", TriggerEvent.SIGNAL_EVENT, doc));
		} catch (ModelException e) {
			e.printStackTrace();
			return false;
		}
		
		if(isDirty_ && !isError_) {
			persistGameState(true);
			isDirty_ = false;
			isError_ = false;
			return true;
		}
		else {
			isDirty_ = false;
			isError_ = false;
			return false;
		}
	}
	public boolean sendPlayerJoinRequest(User user) {
		isError_ = false;
		
		GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		
		DocumentBuilderFactory docbuilderfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = docbuilderfactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			//TODO: exception handling
			e.printStackTrace();
			return false;
		}
		Document doc = builder.newDocument();
		
		Node player = doc.createElementNS(Config.getInstance().getGameEngineNamespace(), "player");
		player.appendChild(doc.createTextNode(gu.getHashedUserId()));
		doc.appendChild(player);
		
		try {
			getExec().triggerEvent(new TriggerEvent("game.playerJoin", TriggerEvent.SIGNAL_EVENT, doc));
		} catch (ModelException e) {
			//TODO: exception handling...
			e.printStackTrace();
			return false;
		}
		
		if(isDirty_ && !isError_) {
			persistGameState(true);
		}
		else {
			isDirty_ = false;
			isError_ = false;
		}
		
		return true;
	}
	public boolean triggerEvent(String eventid, Node node) {
		boolean ret = true;
		
		isError_ = false;
		
		try {
			getExec().triggerEvent(new TriggerEvent(eventid, TriggerEvent.SIGNAL_EVENT, node));
		} catch (ModelException e) {
			if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
				e.printStackTrace();
			return false;
		}
		
		if(isDirty_ && !isError_) {
			persistGameState(false);
		}
		else if(isError_) {
			ret = false;
		}
		else if(!isDirty_) {
			ret = false;
			errorMessage_ = String.format("Event '%s' was not valid in the current state of the game.", eventid);
		}
		
		isDirty_ = false;
		isError_ = false;
		
		return ret;
	}
	public List<Watcher> getWatchers() {
		return Watcher.findWatchersByGame(this);
	}
	public Watcher addWatcher(User user) {
		final GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		final Game game = this;
		
		Watcher ret = null;
		
		try {
			ret = (Watcher)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					Watcher w = Watcher.findWatcherByGameAndGameUser(game, gu);
					
					if(w == null) {
						w = new Watcher(game, gu);
						pm.makePersistent(w);
					}
					
					return w;
				}
			});
		}
		catch(PersistenceCommandException e) {
			//TODO: handle this exception
			e.printStackTrace();
		}		
		
		return ret;
	}
	public boolean removeWatcher(User user) {
		final GameUser gu = GameUser.findOrCreateGameUserByUser(user);
		final Game game = this;
		
		boolean ret = false;
		
		try {
			ret = (Boolean)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					Watcher w = Watcher.findWatcherByGameAndGameUser(game, gu);
					
					if(w == null) {
						return false;
					}
					else {
						pm.deletePersistent(w);
						return true;
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
	public void setGameType(GameType gt) {
		if(gt != null) this.gameTypeKey = gt.getKey();
		else this.gameTypeKey = null;
	}
	public GameType getGameType() {
		if(this.gameTypeKey == null) 
			return null;
		else 
			return GameType.findByKey(this.gameTypeKey);
	}
	
	public void makePersistent() {
		final Game persist = this;
		
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
		
	public void serialize(Writer out) {
		JSONSerializer json = new JSONSerializer();
		
		for(Player p : players) {
			
		}
		
		json.include("events")
			.include("parameters")
			.include("players")
			.exclude("watchers")
			.include("owner")
			.include("states")
			.include("gameType")
			.exclude("exec")
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

	@Override
	public void onEntry(TransitionTarget state) {
		log.info("OnEntry: " + state.getId());			
	}

	@Override
	public void onExit(TransitionTarget state) {
		log.info("OnExit: " + state.getId());	
	}

	@Override
	public void onTransition(TransitionTarget from, TransitionTarget to,
			Transition transition) {
		log.info("OnTransition: " + from.getId() + " -> " + to.getId() + ": [" + transition.getEvent() + "]");
		isDirty_ = true;		
	}

	public Key getKey() {
		return key;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}


}
