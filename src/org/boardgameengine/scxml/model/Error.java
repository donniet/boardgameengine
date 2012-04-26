package org.boardgameengine.scxml.model;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.model.Action;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.State;


public class Error extends Action {

	private String message;
	private Log log;
	
	public Error() {
		log = LogFactory.getLog(Error.class);
	}

	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException {
		Set states = scInstance.getExecutor().getCurrentStatus().getStates();
		String currentState = "[unknown]";
		if(states.size() > 0) {
			currentState = ((State)states.iterator().next()).getId();
		}
		
		log.info("[" + currentState + "] " + message);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
