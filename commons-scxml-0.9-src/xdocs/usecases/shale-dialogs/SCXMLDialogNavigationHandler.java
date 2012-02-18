/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * You may build in a package on your choice. Dependency information:
 *
 * Commons SCXML dependencies -
 * http://commons.apache.org/scxml/dependencies.html
 *
 * Apache Shale dependencies -
 * http://shale.apache.org/dependencies.html
 */
package org.apache.commons.scxml.usecases;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.scxml.SCXMLDigester;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.TriggerEvent;
import org.apache.commons.scxml.env.SimpleDispatcher;
import org.apache.commons.scxml.env.SimpleErrorHandler;
import org.apache.commons.scxml.env.SimpleErrorReporter;
import org.apache.commons.scxml.env.SimpleSCXMLListener;
import org.apache.commons.scxml.env.faces.SessionContext;
import org.apache.commons.scxml.env.faces.ShaleDialogELEvaluator;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.SCXML;
import org.apache.commons.scxml.model.TransitionTarget;

import org.apache.shale.dialog.Globals;
import org.apache.shale.dialog.Status;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

/**
 * <p>SCXML configuration file(s) driven Shale dialog navigation handler.</p>
 *
 * <p>Recipe for using SCXML documents to drive Shale dialogs:
 *   <ol>
 *    <li>Build the <code>SCXMLDialogNavigationHandler</code> (available
 *     below, use a Commons SCXML nightly build 10/09/05 or later) and make it
 *     available to your web application classpath (<code>WEB-INF/classes</code>).
 *    </li>
 *    <li>Update the &quot;<code>WEB-INF/faces-config.xml</code>&quot;
 *     for your web application such that the
 *     &quot;<code>faces-config/application/navigation-handler</code>&quot;
 *     entry points to
 *     &quot;<code>org.apache.commons.scxml.usecases.SCXMLDialogNavigationHandler</code>&quot;
 *     (with the appropriate package name, if you changed it).
 *    </li>
 *    <li>As an alternative to (1) and (2), you can place a <i>jar</i> in the 
 *     <code>WEB-INF/lib</code> directory which contains the
 *     <code>SCXMLDialogNavigationHandler</code> and a
 *     <code>META-INF/faces-config.xml</code> with just the entry in (2).</li>
 *    <li>Use SCXML documents to describe Shale dialog flows (details below)
 *     in your application. You may have multiple mappings from transition
 *     targets to JSF views to support multi-channel applications.</li>
 *    <li>The SCXML-based dialog is entered when
 *     <code>handleNavigation()</code> is called with a logical outcome
 *     of the form &quot;<code>dialog:xxx</code>&quot; and there is no current
 *     dialog in progress, where &quot;<code>xxx</code>&quot; is the URL pointing
 *     to the SCXML document.</li>
 *   </ol>
 * </p>
 *
 * <p>Using SCXML documents to define the Shale dialog "flows":
 *   <ul>
 *     <li>ActionState instances may be mapped to executable content
 *         in UML &lt;onentry&gt (and may be chained similarly).</li>
 *     <li>ViewState instances may be mapped to UML transition
 *         targets.</li>
 *     <li>SubdialogState instances may be mapped to external SCXML
 *         documents.</li>
 *     <li>EndState instances may be mapped to SCXML final states.</li>
 *     <li>The {@link SCXMLDialogNavigationHandler} defines a
 *         &quot;faces.outcome&quot; event which the relevant SCXML
 *         transitions from a &quot;view state&quot; can wait for.</li>
 *   </ul>
 * </p>
 *
 * <p>Towards pluggable dialog management in Shale - A &quot;black box&quot;
 *    dialog may consist of the following tuple:
 *   <ul>
 *     <li>Unique dialog identifier</li>
 *     <li>A generic NavigationHandler (i.e. dialog strategy)</li>
 *     <li>An dialog/flow configuration resource (Ex: SCXML document)</li>
 *     <li>Optionally, multiple other configuration resources,
 *      (Ex: one for each channel - web, voice, small device, etc.)</li>
 *   </ul>
 *    The Shale DialogNavigationHandler may then delegate appropriately.
 * </p>
 */
public final class SCXMLDialogNavigationHandler extends NavigationHandler {

    // ------------------------------------------------------------ Constructors
    /**
     * <p>Create a new {@link SCXMLDialogNavigationHandler}, wrapping the
     * specified standard navigation handler implementation.</p>
     *
     * @param handler Standard <code>NavigationHandler</code> we are wrapping
     */
    public SCXMLDialogNavigationHandler(NavigationHandler handler) {

        this.handler = handler;

    }

    // -------------------------------------------------------- Static Variables
    /**
     * <p>The prefix on a logical outcome String that indicates the remainder
     * of the string is the URL of a SCXML-based Shale dialog to be entered.</p>
     */
    public static final String PREFIX = "dialog:";

    // ------------------------------------------------------ Instance Variables

    /**
     * <p>The standard <code>NavigationHandler</code> implementation that
     * we are wrapping.</p>
     */
    private NavigationHandler handler = null;

    /**
     * <p>The <code>Log</code> instance for this class.</p>
     */
    private final Log log = LogFactory.getLog(getClass());

    /**
     * <p>Key under which we will store the SCXMLExecutor (more generally,
     * some session scoped state pertaining to the current dialog).</p>
     */
    private String dialogKey = null; // Cached on first use

    /**
     * <p>Map storing SCXML state IDs as keys and JSF view IDs as values.</p>
     */
    private Map target2viewMap = null;

    // ----------------------------------------------- NavigationHandler Methods

    /**
     * <p>Handle the navigation request implied by the specified parameters.</p>
     *
     * @param context <code>FacesContext</code> for the current request
     * @param fromAction The action binding expression that was evaluated
     *  to retrieve the specified outcome (if any)
     * @param outcome The logical outcome returned by the specified action
     *
     * @exception IllegalArgumentException if the configuration information
     *  for a previously saved position cannot be found
     * @exception IllegalArgumentException if an unknown State type is found
     */
    public void handleNavigation(FacesContext context, String fromAction,
                                 String outcome) {

        if (log.isDebugEnabled()) {
            log.debug("handleNavigation(viewId=" +
                      context.getViewRoot().getViewId() +
                      ",fromAction=" + fromAction +
                      ",outcome=" + outcome + ")");
        }

        SCXMLExecutor exec = getDialogExecutor(context);
        String viewId = null;
        
        if (exec == null && outcome != null && outcome.startsWith(PREFIX)) {
        	
        	/**** DIALOG ENTRY ****/
        	// dialog is a state machine, parse & obtain an executor
        	exec = initDialogExecutor(context, outcome.substring(PREFIX.
        			length()));

        	if (exec != null) {
        		// cache executor in session scope
        		// TODO: Shale caches Dialog instances. SCXMLExecutor
        		// knows what state(s) the dialog is in, so Dialog#findState()
        		// is not needed.
        		setDialogExecutor(context, exec);
        		// obtain our initial view
        		viewId = getCurrentViewId(exec);
        	}
        	// else delegate
        	
        } else if (exec != null) {
        	
        	/**** SUBSEQUENT TURNS OF DIALOG ****/
        	// pass a handle to the current ctx (for evaluating binding exprs)
        	updateEvaluator(context, outcome);
        	// fire a "faces.outcome" event on the dialog's state machine
        	TriggerEvent[] te = { new TriggerEvent("faces.outcome",
        			TriggerEvent.SIGNAL_EVENT) };
        	try {
        		exec.triggerEvents(te);
        	} catch (ModelException me) {
        		log.error(me.getMessage(), me);
        	}
        	// obtain next view
        	viewId = getCurrentViewId(exec);
        }

        if (viewId != null) {
        
        	// we understood this "outcome" and we have a new view to render
        	log.info("Rendering view: " + viewId);
        	updateDialogStatus(context, exec);
        	render(context, viewId);

        } else {
        
        	/**** DELEGATE BY DEFAULT ****/
        	handler.handleNavigation(context, fromAction, outcome);

        }


    }

    /**
     * <p>Return the SCXMLExecutor for the specified SCXML document, if it
     * exists; otherwise, return <code>null</code>.</p>
     *
     * @param context <code>FacesContext</code> for the current request
     * @param dialogIdentifier URL of the SCXML document for the requested
     *                         dialog
     */
    private SCXMLExecutor initDialogExecutor(FacesContext context,
    		String dialogIdentifier) {

        assert context != null;
        assert dialogIdentifier != null;
        
        // We're parsing the SCXML dialog just in time here
        URL scxmlDocument = null;
        try {
        	scxmlDocument = context.getExternalContext().
				getResource(dialogIdentifier);
        } catch (MalformedURLException mue) {
        	log.error(mue.getMessage(), mue);
        }

        if (scxmlDocument == null) {
        	log.warn("No SCXML document at: " + dialogIdentifier);
        	return null;
        }
        
        SCXML scxml = null;
        ShaleDialogELEvaluator evaluator = new ShaleDialogELEvaluator();
        evaluator.setFacesContext(context);
        try {
            scxml = SCXMLDigester.digest(scxmlDocument,
            		new SimpleErrorHandler(), new SessionContext(context),
                	evaluator);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if (scxml == null) {
        	log.warn("Could not parse SCXML document at: " + dialogIdentifier);
        	return null;
        }
        
        SCXMLExecutor exec = null;
        try {
            exec = new SCXMLExecutor(evaluator, new SimpleDispatcher(),
            		new SimpleErrorReporter());
            scxml.addListener(new SimpleSCXMLListener());
            exec.setSuperStep(true);
            exec.setStateMachine(scxml);
        } catch (ModelException me) {
        	log.warn(me.getMessage(), me);
        	return null;
        }
        
        // read SCXML state IDs to JSF view IDs map, channel dependent
        readState2ViewMap(context, dialogIdentifier, null);
        
        // FIXME: Remove dependence on the org.apache.shale.dialog.impl package
        // below (introduced so we can reuse the existing StatusImpl and the
        // AbstractFacesBean subtypes in the usecases war for the proof of
        // concept).
        // Ignoring STATUS_PARAM since usecases war doesn't use it for the
        // log on / edit profile dialogs.
        // TODO: The next line should be Dialog Manager implementation agnostic
        Status status =	new org.apache.shale.dialog.impl.StatusImpl();

        context.getExternalContext().getSessionMap().put(Globals.STATUS, status);
        status.push(new Status.Position(dialogIdentifier, getCurrentViewId(exec)));
        
        return exec;
        
    }

    /**
     * <p>Set the {@link SCXMLExecutor} instance for the current user.</p>
     *
     * @param context <code>FacesContext</code> for the current request
     * @param exec <code>SCXMLExecutor</code> that will run the dialog
     */
    private void setDialogExecutor(FacesContext context, SCXMLExecutor exec) {

    	assert context != null;
    	assert exec != null;
    	
        Map map = context.getExternalContext().getSessionMap();
        String key = getDialogKey(context);
        assert key != null;
        map.put(key, exec);
        
    }

    /**
     * <p>Return the {@link SCXMLExecutor} instance for the current user.</p>
     *
     * @param context <code>FacesContext</code> for the current request
     */
    private SCXMLExecutor getDialogExecutor(FacesContext context) {

        assert context != null;

        Map map = context.getExternalContext().getSessionMap();
        String key = getDialogKey(context);
        return (SCXMLExecutor) map.get(key);

    }

    /**
     * Update evaluator with current FacesContext for evaluation of
     * binding expressions used in Shale dialog.
     */
    private void updateEvaluator(FacesContext context, String outcome) {
    	
    	assert context != null;
    	
    	((ShaleDialogELEvaluator) getDialogExecutor(context).getEvaluator()).
			setFacesContext(context);
    	context.getExternalContext().getSessionMap().put("outcome", outcome);
    }

    /**
     * Update dialog Status
     * 
     * @param context The FacesContext
     * @param exec The SCXMLExecutor
     */
    private void updateDialogStatus(FacesContext context, SCXMLExecutor exec) {
    	
    	assert context != null;
    	assert exec != null;
    	
    	// TODO: Test this
    	Status status = (Status) context.getExternalContext().getSessionMap().
			get(Globals.STATUS);
    	if (exec.getCurrentStatus().isFinal()) {
            setDialogExecutor(context, null);
            status.pop();
    	} else {
            status.peek().setStateName(getCurrentViewId(exec));
    	}
    }

    /**
     * Get next view to render, assuming one view at a time. 
     *
     * @param currentStates The set of current states
     * @return String The JSF viewId of the next view
     */
    private String getCurrentViewId(SCXMLExecutor exec) {
    	
    	assert exec != null;
    	
    	Set currentStates = exec.getCurrentStatus().getStates();
    	for (Iterator i = currentStates.iterator(); i.hasNext(); ) {
    		String targetId = ((TransitionTarget) i.next()).getId();
    		if (target2viewMap.containsKey(targetId)) {
    			return (String) target2viewMap.get(targetId);
    		}
    	}
    	return null;
    }

    /**
     * <p>Return the session scope attribute key under which we will
     * store dialog state for the current user.  The value
     * is specified by a context init parameter named by constant
     * <code>Globals.DIALOG_STATE_PARAM</code>, or defaults to the value
     * specified by constant <code>Globals.DIALOG_STATE</code>.</p>
     *
     * @param context <code>FacesContext</code> for the current request
     */
    private String getDialogKey(FacesContext context) {
    	
    	assert context != null;
    	
        if (dialogKey == null) {
            dialogKey =
              context.getExternalContext().
			  	getInitParameter(Globals.DIALOG_STATE_PARAM);
            if (dialogKey == null) {
                dialogKey = Globals.DIALOG_STATE;
            }
        }
        return dialogKey;

    }

    /**
     * <p>Render the view corresponding to the specified view identifier.</p>
     *
     * @param context <code>FacesContext</code> for the current request
     * @param viewId View identifier to be rendered, or <code>null</code>
     *  to rerender the current view
     */
    private void render(FacesContext context, String viewId) {

        assert context != null;

        if (log.isDebugEnabled()) {
            log.debug("render(viewId=" + viewId + ")");
        }

        // Stay on the same view if requested
        if (viewId == null) {
            return;
        }

        // Create the specified view so that it can be rendered
        ViewHandler vh = context.getApplication().getViewHandler();
        UIViewRoot view = vh.createView(context, viewId);
        view.setViewId(viewId);
        context.setViewRoot(view);

    }

    /**
     * FIXME: - Placeholder for SCXML state ID to JSF view ID mapper.
     * Provides multi-channel aspect to Shale dialog management.
     * 
     */
    private void readState2ViewMap(FacesContext context,
    		String dialogIdentifier, String channel) {
    	
    	assert context != null;

    	String STATE_TO_VIEW_MAP = "/WEB-INF/dialogstate2view.xml";
    	target2viewMap = new HashMap();

		Digester digester = new Digester();
		digester.clear();
		digester.setNamespaceAware(false);
        digester.setUseContextClassLoader(false);
        digester.setValidating(false);
        digester.addRule("map/entry", new Rule() {
        	/** SCXML target ID. */
        	private String targetId;
        	/** JSF view ID. */
        	private String viewId;
            /** {@inheritDoc} */
            public final void begin(final String namespace, final String name,
                    final Attributes attributes) {
            	targetId = attributes.getValue("targetId");
            	viewId = attributes.getValue("viewId");
            }
            /** {@inheritDoc} */
            public void end(final String namespace, final String name) {
            	target2viewMap.put(targetId, viewId);
            }
        });
        
        try {
            URL mapURL = context.getExternalContext().getResource(STATE_TO_VIEW_MAP);
        	InputSource source = new InputSource(mapURL.toExternalForm());
        	source.setByteStream(mapURL.openStream());
        	digester.parse(source);
        } catch (Exception e) {
        	log.error(e.getMessage(), e);
        }
    }

}
