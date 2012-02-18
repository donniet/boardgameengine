package org.apache.commons.scxml.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.Context;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.PathResolver;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.SCXMLHelper;

public class Script extends Action implements Serializable, PathResolverHolder {
	private static final long serialVersionUID = 1L;
	
	private String src;
	private String type;
	private String body;
	private PathResolver pathResolver;
	

	public Script() {
		this.src = null;
	}
	
	public final String getSrc() {
		return src;
	}
	public final void setSrc(final String src) {
		this.src = src;
	}
	
	public final String getBody() {
		return body;
	}
	public final void setBody(final String body) {
		this.body = body;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	

    /**
     * Get the {@link PathResolver}.
     *
     * @return Returns the pathResolver.
     */
    public PathResolver getPathResolver() {
        return pathResolver;
    }

    /**
     * Set the {@link PathResolver}.
     *
     * @param pathResolver The pathResolver to set.
     */
    public void setPathResolver(final PathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }
    
    public void executeForInitialization(Context ctx, Evaluator eval) throws SCXMLExpressionException {
		if(src == null || SCXMLHelper.isStringEmpty(src)) {
			ctx.setLocal(getNamespacesKey(), getNamespaces());
			eval.eval(ctx, body);
			ctx.setLocal(getNamespacesKey(), null);
		}
		else {
			throw new SCXMLExpressionException("<script> element does not support src attribute at this time.");
		}
    }

	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException {
		TransitionTarget parentTarget = getParentTransitionTarget();
		Context ctx = scInstance.getContext(parentTarget);
		Evaluator eval = scInstance.getEvaluator();

		if(src == null || SCXMLHelper.isStringEmpty(src)) {
			ctx.setLocal(getNamespacesKey(), getNamespaces());
			Object ret = eval.eval(ctx, body);
			ctx.setLocal(getNamespacesKey(), null);
		}
		else {
			throw new SCXMLExpressionException("<script> element does not support src attribute at this time.");
		}
	}
	
}
