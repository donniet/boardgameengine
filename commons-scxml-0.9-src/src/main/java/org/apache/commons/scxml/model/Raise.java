package org.apache.commons.scxml.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.Context;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.SCXMLHelper;
import org.apache.commons.scxml.TriggerEvent;
import org.apache.commons.scxml.semantics.ErrorConstants;

public class Raise extends Action {	
	private String event;
	private String namelist;

	public Raise() {
		super();
	}
	
	/**
     * Get the namelist.
     *
     * @return String Returns the namelist.
     */
    public final String getNamelist() {
        return namelist;
    }

    /**
     * Set the namelist.
     *
     * @param namelist The namelist to set.
     */
    public final void setNamelist(final String namelist) {
        this.namelist = namelist;
    }
	
	/**
     * Get the event to send.
     *
     * @param event The event to set.
     */
    public final void setEvent(final String event) {
        this.event = event;
    }

    /**
     * Set the event to send.
     *
     * @return String Returns the event.
     */
    public final String getEvent() {
        return this.event;
    }
    
	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException {
		
		TransitionTarget parentTarget = getParentTransitionTarget();
        Context ctx = scInstance.getContext(parentTarget);
        ctx.setLocal(getNamespacesKey(), getNamespaces());
        Evaluator eval = scInstance.getEvaluator();
		
        Map params = null;
        if (!SCXMLHelper.isStringEmpty(namelist)) {
            StringTokenizer tkn = new StringTokenizer(namelist);
            params = new HashMap(tkn.countTokens());
            while (tkn.hasMoreTokens()) {
                String varName = tkn.nextToken();
                Object varObj = ctx.get(varName);
                if (varObj == null) {
                    //considered as a warning here
                    errRep.onError(ErrorConstants.UNDEFINED_VARIABLE,
                            varName + " = null", parentTarget);
                }
                params.put(varName, varObj);
            }
        }
        
		String eventValue = event;
        if (!SCXMLHelper.isStringEmpty(event)) {
            eventValue = (String) eval.eval(ctx, event);
            if (SCXMLHelper.isStringEmpty(eventValue)
                    && appLog.isWarnEnabled()) {
                appLog.warn("<send>: event expression \"" + event
                    + "\" evaluated to null or empty String");
            }
        }
        
        if (appLog.isDebugEnabled()) {
            appLog.debug("<send>: Enqueued event '" + eventValue
                + "' with no delay");
        }
        derivedEvents.add(new TriggerEvent(eventValue,
            TriggerEvent.SIGNAL_EVENT, params));
        
        ctx.setLocal(getNamespacesKey(), null);		
	}
}
