package org.boardgameengine.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.model.Data;
import org.apache.commons.scxml.model.Datamodel;
import org.boardgameengine.config.Config;
import org.boardgameengine.persist.PMF;
import org.boardgameengine.persist.PersistenceCommand;
import org.boardgameengine.persist.PersistenceCommandException;
import org.boardgameengine.scxml.js.JsContext;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;

@PersistenceCapable(detachable="true")
public class GameState {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private Game game;
	
	@Persistent
	private Date stateDate = new Date();
	
	@Persistent
	private boolean important = false;
	
	@Persistent(defaultFetchGroup = "true")
	@Element(dependent = "true", mappedBy = "gameState", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="id asc"))	
	private List<GameStateData> datamodel;
	
	@Persistent
	private Set<String> stateSet;
	
	protected GameState(Game game) {
		this.game = game;
		stateDate = new Date();
		setDatamodel(new ArrayList<GameStateData>());
		stateSet = new HashSet<String>();
	}
	public GameState() {
		game = null;
		stateDate = new Date();
		setDatamodel(new ArrayList<GameStateData>());
		stateSet = new HashSet<String>();
	}
	protected void setGame(Game game) {
		this.game = game;
	}
	protected Game getGame() {
		return game;
	}	
	public void setStateDate(Date stateDate_) {
		this.stateDate = stateDate_;
	}
	public Date getStateDate() {
		return stateDate;
	}
	public Set<String> getStateSet() {
		return stateSet;
	}
	
	public static void deleteOldStatesForGame(final Game game) {
		try {
			PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					if(game.getPersisted()) {
					
						Query q = pm.newQuery(GameState.class, "game == gameIn && important == false");
						q.declareParameters(Game.class.getName() + " gameIn");
						q.setResult("count(key)");
						
						long results = (Long)q.execute(game);
						
						int stateHistorySize = Config.getInstance().getStateHistorySize();
											
						if(results > 2 * stateHistorySize) {
							q = pm.newQuery(GameState.class, "game == gameIn && important == false");
							q.setOrdering("stateDate asc");
							q.declareParameters(Game.class.getName() + " gameIn");
							q.setRange(0, results - stateHistorySize);
							q.deletePersistentAll(game);
						}
					}
					
					return null;
				}
			});
		}
		catch(PersistenceCommandException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * inject the datamodel into the context
	 * 
	 * @param cxt the context to be injected into
	 */
	protected void injectInto(JsContext cxt) {
		Log log = LogFactory.getLog(GameState.class);
		
		DocumentBuilderFactory docbuilderfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = docbuilderfactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			builder = null;
			log.error("Could not get transformer", e);
			return;
		}
		Document doc = builder.newDocument();
		
		
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer trans = null;
		try {
			trans = factory.newTransformer();
		} catch (TransformerConfigurationException e) {
			trans = null;
			log.error("Could not get transformer", e);
			return;
		}
		
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		
		
		for(Iterator<GameStateData> i = getDatamodel().iterator(); i.hasNext(); ) {
			GameStateData d = i.next();
			
			ByteArrayInputStream bis = new ByteArrayInputStream(d.getValue());
			DOMResult dr = new DOMResult(doc);
			
			try {
				trans.transform(new StreamSource(bis), dr);
			} catch (TransformerException e) {
				dr = new DOMResult();
				log.error("Transform failed: " + d.getId(), e);
			}
			
			cxt.setLocal(d.getId(), doc);
		}
	}
	
	protected void extractFrom(Datamodel datamodel, JsContext cxt) {
		Log log = LogFactory.getLog(GameState.class);
		
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer trans = null;
		try {
			trans = factory.newTransformer();
		} catch (TransformerConfigurationException e) {
			trans = null;
			log.error("Could not get transformer", e);
			return;
		}
		
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		
		for(Iterator<Data> i = datamodel.getData().iterator(); i.hasNext(); ) {
			Data d = i.next();
			
			GameStateData gsd = new GameStateData(this);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Object o = cxt.get(d.getId());
			Node n = XMLLibImpl.toDomNode(o);
			
			try {
				trans.transform(new DOMSource(n), new StreamResult(bos));
			} catch(TransformerException e) {
				log.error("Transform failed: " + d.getId(), e);
			}
			
			gsd.setId(d.getId());
			gsd.setValue(bos.toByteArray());
			
			this.getDatamodel().add(gsd);			
		}
	}
	protected void setDatamodel(List<GameStateData> datamodel) {
		this.datamodel = datamodel;
	}
	public List<GameStateData> getDatamodel() {
		return datamodel;
	}
	
	@SuppressWarnings("unchecked")
	public void refreshDatamodel() {
		List<GameStateData> results = null;
		try {
			results = (List<GameStateData>)PMF.executeCommandInTransaction(new PersistenceCommand() {
				@Override
				public Object exec(PersistenceManager pm) {
					Query q = pm.newQuery(GameStateData.class);
					q.setFilter("gameState == gameStateToFind");
					q.declareParameters(GameState.class.getName() + " gameStateToFind");
					List<GameStateData> results = (List<GameStateData>)q.execute(this);
					
					pm.makeTransientAll(results);
					
					return results;
				}
			});
		}
		catch(PersistenceCommandException e) {
			e.printStackTrace();
		}
		
		if(results != null) {
			setDatamodel(results);
		}
	}
	public boolean isImportant() {
		return important;
	}
	public void setImportant(boolean important) {
		this.important = important;
	}
}
