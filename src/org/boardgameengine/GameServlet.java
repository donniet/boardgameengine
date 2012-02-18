package org.boardgameengine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.naming.BinaryRefAddr;
import javax.servlet.ServletInputStream;
import javax.servlet.http.*;
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
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.scxml.TriggerEvent;
import org.apache.commons.scxml.io.SCXMLParser;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.SCXML;
import org.apache.commons.scxml.model.State;
import org.boardgameengine.config.Config;
import org.boardgameengine.error.GameLoadException;
import org.boardgameengine.model.Game;
import org.boardgameengine.model.GameHistoryEvent;
import org.boardgameengine.model.GameState;
import org.boardgameengine.model.GameStateData;
import org.boardgameengine.model.GameType;
import org.boardgameengine.model.GameUser;
import org.boardgameengine.persist.PMF;
import org.boardgameengine.scxml.js.GaeScriptableSerializer;
import org.boardgameengine.scxml.js.JsContext;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.utils.SystemProperty;

import java.util.regex.*;

@SuppressWarnings("serial")
public class GameServlet extends HttpServlet {	
	private class PatternHandlerPair {
		public RequestHandler handler = null;
		public Pattern pattern = null;
		public PatternHandlerPair(final RequestHandler handler, final Pattern pattern) {
			this.pattern = pattern;
			this.handler = handler;
		}
		public PatternHandlerPair() {}
	}
	
	private static boolean isDebug() {
		return SystemProperty.environment.value() == SystemProperty.Environment.Value.Development;
	}
	
	private List<PatternHandlerPair> gethandlers_ = null;
	private List<PatternHandlerPair> postHandlers_ = null;
	
	public GameServlet() {
		super();
		gethandlers_ = new ArrayList<PatternHandlerPair>();
		postHandlers_ = new ArrayList<PatternHandlerPair>();
		
		addGetHandler("^/createGameType", new GameUserRequestHandler() {
			@Override
			public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matches) throws IOException {
				UserService userService = UserServiceFactory.getUserService();
				
				GameType t = new GameType();
				
				GameUser gu = GameUser.findOrCreateGameUserByUser(userService.getCurrentUser());
				
				t.setCreator(gu);
				t.setDescription("Test");
				t.setTypeName("Test");
								
				URL u = GameServlet.class.getResource("/test3.xml");
				URLConnection conn = u.openConnection();
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				InputStream is = conn.getInputStream();
				byte[] buffer = new byte[0x1000];
				
				int count = 0;
				while((count = is.read(buffer)) > 0) {
					bos.write(buffer, 0, count);
				}
				
				t.setStateChart(bos.toByteArray());
				
				PersistenceManager pm = PMF.getInstance().getPersistenceManager();
				
				pm.makePersistent(t);
				
				resp.sendRedirect("/createGame?type=Test");
			}
		});
		
		addGetHandler("^/createGame", new GameUserRequestHandler() {
			@Override
			public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matches) throws IOException {
				UserService userService = UserServiceFactory.getUserService();
				
				GameType gt = GameType.findByTypeName(req.getParameter("type"));
								
				Game h = null;
				try {
					h = new Game(gt);
				} catch (GameLoadException e) {
					resp.setStatus(500);
					resp.setContentType("text/plain");
					resp.getWriter().println("Could not load game type: " + req.getParameter("type"));
					return;
				}
				
				h.addPlayer(userService.getCurrentUser(), "green");
				h.addWatcher(userService.getCurrentUser());
				
				h.makePersistent();
				
				resp.sendRedirect(String.format("/game/%s/", h.getGameId()));
									
			}
		});
		
		addGetHandler("^/game/([^/]+)/datamodel", new GameServiceRequestHandler() {
			@Override
			public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matches) throws IOException {
				Game h = null;
				try {
					h = Game.findGameById(matches.group(1));
				} catch (GameLoadException e1) {
					resp.setStatus(500);
					if(isDebug()) {
						resp.setContentType("text/plain");
						e1.printStackTrace(resp.getWriter());
					}
				}
				
				if(h == null) {
					resp.setStatus(404);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						resp.getWriter().println("This game does not exist: " + matches.group(1));
					return;
				}
				
				List<GameState> states = h.getStates();
				if(states.size() == 0) {
					resp.setStatus(404);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						resp.getWriter().println("This game has no active states: " + matches.group(1));
					return;
				}
				
				GameState gs = states.get(states.size() - 1);
				
				gs.refreshDatamodel();
				
				List<GameStateData> datamodel = gs.getDatamodel();
				
				Document doc = null;
				
				try {
					DocumentBuilderFactory docbuildfactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = docbuildfactory.newDocumentBuilder();
					doc = builder.newDocument();
				}
				catch(ParserConfigurationException e) {
					resp.setStatus(500);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						e.printStackTrace(resp.getWriter());
					return;
				}
				TransformerFactory factory = TransformerFactory.newInstance();
				Transformer trans = null;

				try {
					trans = factory.newTransformer();
				} catch (TransformerConfigurationException e) {
					trans = null;
					resp.setStatus(500);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						e.printStackTrace(resp.getWriter());
					return;
				}
							
				Node datamodelNode = doc.createElementNS("http://www.w3.org/2005/07/scxml", "datamodel");
				
				for(GameStateData gsd : datamodel) {
					ByteArrayInputStream bis = new ByteArrayInputStream(gsd.getValue());						
					
					DOMResult dr = new DOMResult(datamodelNode);
					
					try {
						trans.transform(new StreamSource(bis), dr);
					} catch (TransformerException e) {
						resp.setStatus(500);
						if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
							e.printStackTrace(resp.getWriter());
						return;
					}
					
					
				}
				
				doc.appendChild(datamodelNode);
				
				try {
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						trans.setOutputProperty(OutputKeys.INDENT, "yes");
					trans.transform(new DOMSource(doc), new StreamResult(resp.getOutputStream()));
				} catch (TransformerException e) {
					resp.setStatus(500);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						e.printStackTrace(resp.getWriter());
					return;
				}
			}
			
		});
		
		addGetHandler("^/game/([^/]+)/datamodel/([^/]+)", new GameServiceRequestHandler() {
			@Override
			public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matches) throws IOException {
				Game h = null;
				try {
					h = Game.findGameById(matches.group(1));
				} catch (GameLoadException e1) {
					resp.setStatus(500);
					if(isDebug()) {
						resp.setContentType("text/plain");
						e1.printStackTrace(resp.getWriter());
					}
				}
				
				if(h == null) {
					resp.setStatus(404);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						resp.getWriter().println("This game does not exist: " + matches.group(1));
					return;
				}
				
				List<GameState> states = h.getStates();
				if(states.size() == 0) {
					resp.setStatus(404);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						resp.getWriter().println("This game has no active states: " + matches.group(1));
					return;
				}
				
				GameState gs = states.get(states.size() - 1);
				
				gs.refreshDatamodel();
				
				List<GameStateData> datamodel = gs.getDatamodel();
				
				for(GameStateData gsd : datamodel) {
					if(matches.group(2).equals(gsd.getId())) {
						TransformerFactory factory = TransformerFactory.newInstance();
						Transformer trans = null;
						try {
							trans = factory.newTransformer();
						} catch (TransformerConfigurationException e) {
							trans = null;
							resp.setStatus(404);
							if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
								e.printStackTrace(resp.getWriter());
							return;
						}
						
						ByteArrayInputStream bis = new ByteArrayInputStream(gsd.getValue());						
						
						try {
							trans.transform(new StreamSource(bis), new StreamResult(resp.getOutputStream()));
							resp.setContentType("application/xml");
						} catch (TransformerException e) {
							resp.setStatus(404);
							if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
								e.printStackTrace(resp.getWriter());
							return;
						}
						return;
					}
				}
				
				resp.setStatus(404);
				if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
					resp.getWriter().println("Dataitem: '" + matches.group(2) + "' not found in this game: " + matches.group(1));
				return;
			}
			
		});
		
		addGetHandler("^/game/([^/]+)/", new GameUserRequestHandler() {
			@Override
			public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matches) throws IOException {
				Game h = null;
				try {
					h = Game.findGameById(matches.group(1));
				} catch (GameLoadException e1) {
					resp.setStatus(500);
					if(isDebug()) {
						resp.setContentType("text/plain");
						e1.printStackTrace(resp.getWriter());
					}
				}
				
				if(h == null) {
					resp.setStatus(404);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						resp.getWriter().println("This game does not exist: " + matches.group(1));
					return;
				}
				
				
				resp.setContentType("text/plain");
				/*
				GaeScriptableSerializer s = new GaeScriptableSerializer(resp.getOutputStream(), scope);
				s.Serialize(scope);
				*/
				
				
			}
		});
		
		addPostHandler("^/game/([^/]+)/event/([^/]+)", new GameServiceRequestHandler() {
			@Override
			public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matches) throws IOException {
				Game h = null;
				try {
					h = Game.findGameById(matches.group(1));
				} catch (GameLoadException e1) {
					resp.setStatus(500);
					if(isDebug()) {
						resp.setContentType("text/plain");
						e1.printStackTrace(resp.getWriter());
					}
				}
				
				if(h == null) {
					resp.setStatus(404);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						resp.getWriter().println("This game does not exist: " + matches.group(1));
					return;
				}
				

				UserService userService = UserServiceFactory.getUserService();
				User u = userService.getCurrentUser();
				GameUser gu = GameUser.findOrCreateGameUserByUser(u);
				
				
				DocumentBuilderFactory docbuilderfactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder;
				try {
					builder = docbuilderfactory.newDocumentBuilder();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					resp.setStatus(500);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						e.printStackTrace(resp.getWriter());
					return;
				}
				Document doc = builder.newDocument();
				
				TransformerFactory factory = TransformerFactory.newInstance();
				Transformer trans;
				try {
					trans = factory.newTransformer();
				} catch (TransformerConfigurationException e) {
					resp.setStatus(500);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						e.printStackTrace(resp.getWriter());
					return;
				}
				
				DOMResult dr = new DOMResult(doc);
								
				if(req.getContentLength() > 0) {
					try {
						trans.transform(new StreamSource(req.getInputStream()), dr);
					} catch (TransformerException e) {
						// ignore error
						/*
						resp.setStatus(500);
						if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
							e.printStackTrace(resp.getWriter());
						return;
						*/
					}
				}
				
				Node n = doc.getFirstChild();
				Node player = doc.createElementNS("http://www.pilgrimsofnatac.com/schemas/game.xsd", "player");
				player.appendChild(doc.createTextNode(gu.getHashedUserId()));
				
				if(n == null) {
					doc.appendChild(player);
					n = player;
				}
				else {
					n.appendChild(player);
				}
				
				try {
					h.getExec().triggerEvent(new TriggerEvent(matches.group(2), TriggerEvent.SIGNAL_EVENT, n));
				} catch (ModelException e) {
					resp.setStatus(500);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						e.printStackTrace(resp.getWriter());
					return;
				}
				
				GameState gs = h.persistGameState();
				//h.makePersistent();
				
				resp.setStatus(200);
				resp.setContentType("text/plain");
				for(Object o : h.getExec().getCurrentStatus().getStates()) {
					State s = (State)o;
					resp.getWriter().write(s.getId() + "\n");
				}
			}
		});
		
		addGetHandler("^/game/([^/]+)/gameHistory", new GameServiceRequestHandler() {
			@Override
			public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matches)
					throws IOException {
				resp.setContentType("application/json");
				
				GregorianCalendar gc = new GregorianCalendar(2000, 0, 1);
				Date since = gc.getTime();
				
				String sinceParm = req.getParameter("since");
				
				if(sinceParm != null) {
					try {
						since = Config.getInstance().getDateFormat().parse(sinceParm);
					} catch (ParseException e) {
						since = null;
					}
				}
				
				if(since == null) {
					resp.setStatus(500);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						resp.getWriter().println("The since parameter is not in the correct format: " + sinceParm);
					return;
				}
								
				Game h = null;
				try {
					h = Game.findGameById(matches.group(1));
				} catch (GameLoadException e) {
					resp.setStatus(500);
					if(isDebug()) {
						resp.setContentType("text/plain");
						e.printStackTrace(resp.getWriter());
					}
					return;
				}
				if(h == null) {
					resp.setStatus(404);
					if(isDebug()) {
						resp.setContentType("text/plain");
						resp.getWriter().println("This game does not exist: " + matches.group(1));
					}
					return;
				}	
				
				h.serialize(resp.getWriter());
			}
		});
	}
	
	public void addGetHandler(String name, RequestHandler handler) {
		Pattern p = Pattern.compile(name);
		
		gethandlers_.add(new PatternHandlerPair(handler, p));
	}
	
	public void addPostHandler(String name, RequestHandler handler) {
		Pattern p = Pattern.compile(name);
		
		postHandlers_.add(new PatternHandlerPair(handler, p));
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		UserService userService = UserServiceFactory.getUserService();
		
		String thisURL = req.getRequestURI();
		
		if(req.getUserPrincipal() == null) {
			resp.sendRedirect(userService.createLoginURL(thisURL));
		}
		else {
			for(PatternHandlerPair p : gethandlers_) {
				Matcher m = p.pattern.matcher(req.getPathInfo());
				if(m != null && m.matches()) {
					p.handler.handle(req, resp, m);
					return;
				}
			}
	
			resp.setStatus(404);
			if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
				resp.getWriter().println("The page does not match the request regex: " + req.getPathInfo());
		
			return;
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		UserService userService = UserServiceFactory.getUserService();
		
		String thisURL = req.getRequestURI();
		
		boolean authenticated = (req.getUserPrincipal() != null);
		
		
		for(PatternHandlerPair p : postHandlers_) {
			Matcher m = p.pattern.matcher(req.getPathInfo());
			if(m != null && m.matches()) {
				if(authenticated || !p.handler.getAuthenticationRequired()) {
					p.handler.handle(req, resp, m);
					return;
				}
				else if(p.handler.getSignInIfNotAuthenticated()) {
					resp.sendRedirect(userService.createLoginURL(req.getPathInfo()));
					return;
				}
				else {
					resp.setStatus(401);
					if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
						resp.getWriter().println("You are not authenticated.");
					return;
				}
			}
		}
	
		resp.setStatus(404);
		
		if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Development)
			resp.getWriter().println("The page does not match the request regex: " + req.getPathInfo());
	}
}
