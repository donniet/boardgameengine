package org.boardgameengine.scxml.model;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.model.Action;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.State;


public class Error extends Action {

	private String message = null;
	private String messageexpr = null;
	private Log log;
	
	public Error() {
		log = LogFactory.getLog(Error.class);
	}

	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException {
		
		String msg = "";
		
		if(messageexpr != null) {
			Evaluator eval = scInstance.getEvaluator();
			
			msg = eval.eval(scInstance.getRootContext(), messageexpr).toString();
		}
		else {
			msg = message;
		}
		
		Map<String,String> params = new HashMap<String,String>();
		
		params.put("message", msg);
		
		evtDispatcher.send(
				"game.error", 
				"http://www.pilgrimsofnatac.com/schemas/game.xsd#GameEvent", 
				"http://www.pilgrimsofnatac.com/schemas/game.xsd#GameEventProcessor", 
				"game.error", 
				params, null, 0L, null, null);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	public String getMessageExpr() {
		return messageexpr;
	}

	public void setMessageExpr(String message) {
		this.messageexpr = messageexpr;
	}
}
