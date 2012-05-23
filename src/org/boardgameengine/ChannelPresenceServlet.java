package org.boardgameengine;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.boardgameengine.model.Game;
import org.boardgameengine.model.GameUser;
import org.boardgameengine.model.Player;
import org.boardgameengine.model.Watcher;
import org.boardgameengine.persist.PMF;
import org.boardgameengine.persist.PersistenceCommand;
import org.boardgameengine.persist.PersistenceCommandException;

import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class ChannelPresenceServlet extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// In the handler for _ah/channel/connected/
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		ChannelPresence presence = channelService.parsePresence(req);
		
		String channelKey = presence.clientId();
		
		final Watcher w = Watcher.findWatcherByChannelKey(channelKey);

		if(w == null) return;
		
		if(!presence.isConnected()) {
			try {
				PMF.executeCommandInTransaction(new PersistenceCommand() {				
					@Override
					public Object exec(PersistenceManager pm) {
						pm.deletePersistent(w);
						return null;
					}
				});
			}
			catch(PersistenceCommandException e) {
				//TODO: add exception handling
				e.printStackTrace();
			}
		}
		
		
		Game game = Game.findUninitializedGameByKey(w.getGameKey());
		
		if(game == null) return;
		
		List<Player> players = game.getPlayers();
		
		for(Iterator<Player> i = players.iterator(); i.hasNext();) {
			Player p = i.next();
			
			GameUser gu = p.getGameUser();
			
			if(gu.getKey().equals(w.getGameUserKey())) {
				p.setConnected(presence.isConnected());
				p.makePersistent();
			}
			
		}
	}
}
