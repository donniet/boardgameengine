import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.env.jexl.JexlContext;
import org.apache.commons.scxml.model.Action;
import org.apache.commons.scxml.model.ExternalContent;
import org.apache.commons.scxml.model.ModelException;
import org.w3c.dom.Node;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.XPath;


public class Script extends Action implements ExternalContent {
	private ArrayList<Node> externalNodes_;
		
	public Script() {
		super();
		externalNodes_ = new ArrayList<Node>();
	}
	
	@Override
	public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException {
		
		org.mozilla.javascript.Context cxt = org.mozilla.javascript.Context.enter();
		Scriptable scope = cxt.initStandardObjects();
		
		
		Set keys = scInstance.getRootContext().getVars().keySet();
		for(Iterator i = keys.iterator(); i.hasNext(); ) {
			String key = (String)i.next();
			Object wrapped = cxt.javaToJS(scInstance.getRootContext().getVars().get(key), scope);
			scope.put(key, scope, wrapped);
		}
		Object wrapped = cxt.javaToJS(LogFactory.getLog(Script.class), scope);
		scope.put("log", scope, wrapped);
		
		

				
		System.out.println("script action");
		for(Node n : externalNodes_) {
			switch(n.getNodeType()) {
			case Node.TEXT_NODE:
				try {
					cxt.evaluateString(scope, n.getNodeValue(), "<script>", 1, null);					
				}
				catch(Exception ex) {
					System.out.println(ex.getLocalizedMessage());
				}
				break;
			default:
				System.out.println("Script Node Type not found: " + n.getNodeType());
				break;
			}
		}
		
		cxt.exit();
	}

	@Override
	public List getExternalNodes() {
		return externalNodes_;
	}
	
}
