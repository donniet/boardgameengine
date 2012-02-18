package org.boardgameengine.scxml.js;
import java.io.Serializable;

import org.apache.commons.scxml.Context;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityUtilities;
import org.mozilla.javascript.json.JsonParser;
import org.mozilla.javascript.json.JsonParser.ParseException;
import org.w3c.dom.Node;


public class JsEvaluator implements Evaluator, Serializable {
	private static final long serialVersionUID = 1L;


    /** Error message if evaluation context is not a JexlContext. */
    private static final String ERR_CTX_TYPE = "Error evaluating JS "
        + "expression, Context must be a JsContext";
	
	@Override
	public Object eval(Context ctx, final String expr)
			throws SCXMLExpressionException {
		if(expr == null) {
			return null;
		}
		JsContext jsCtx = null;
		if(ctx instanceof JsContext) {
			jsCtx = (JsContext)ctx;
		}
		else {
			throw new SCXMLExpressionException(ERR_CTX_TYPE);
		}

		ContextFactory factory = jsCtx.getFactory();
		final Scriptable scope = jsCtx.getScope();
		
		Object ret = null;
		try {
			ret = factory.call(new ContextAction() {
				@Override
				public Object run(org.mozilla.javascript.Context cx) {
					//cx.setOptimizationLevel(-1);
					return cx.evaluateString(scope, expr, "<script>", 1, null);
				}
			});
		}
		catch(EcmaError e) {
			int lineNo = e.lineNumber() - 1;
			String sep = SecurityUtilities.getSystemProperty("line.separator");
			int pos = 0;
			while(lineNo > 0 && pos + sep.length() < expr.length()) {
				pos = expr.indexOf(sep, pos + sep.length());
				lineNo--;
			}
			String line = "";
			if(lineNo <= 0 && pos + sep.length() < expr.length()) {
				int n = expr.indexOf(sep, pos + sep.length());
				if(n < 0) n = expr.length() - (pos + sep.length());
				if(pos + sep.length() < n - 1) line = expr.substring(pos + sep.length(), n-1);
			}
			line = line.trim();
			
			throw new SCXMLExpressionException(e.sourceName() + "#" + e.lineNumber() + ":" + line + " -- " + e.getErrorMessage(), e);
		}		
		
		return ret;
	}
	
	@Override
	public Object evalData(Context ctx, String data)
		throws SCXMLExpressionException
	{
		if(data == null) return null;
		
		JsContext jsCtx = null;
		if(ctx instanceof JsContext) {
			jsCtx = (JsContext)ctx;
		}
		else {
			throw new SCXMLExpressionException(ERR_CTX_TYPE);
		}

		ContextFactory factory = jsCtx.getFactory();
		org.mozilla.javascript.Context wrappedCtx = factory.enterContext();
		Scriptable scope = jsCtx.getScope();
		
		JsonParser parser = new JsonParser(wrappedCtx, scope);
		
		Object ret = null;
		try {
			ret = parser.parseValue(data);
		}
		catch(ParseException e) {
			throw new SCXMLExpressionException("eval('" + data + "'):" + e.getMessage(), e);
		}
		finally {
			wrappedCtx.exit();
		}
		
		return ret;
	}

	@Override
	public Boolean evalCond(Context ctx, String expr)
			throws SCXMLExpressionException {
		Object ret = null;
		try {
			ret = eval(ctx, expr);
		}
		catch(SCXMLExpressionException e) {
			throw e;
		}
		
		if(ret == null) 
			return false;
		else if(ret instanceof Boolean) 
			return (Boolean)ret;
		else {
			Boolean r = true;
			try {
				r = (Boolean)ret;
			}
			catch(Exception e) {
				r = true;
			}
			return r;
		}
	}

	@Override
	public Node evalLocation(Context ctx, String expr)
			throws SCXMLExpressionException {
		Object obj = null;
		try {
			obj = eval(ctx, expr);
		}
		catch(SCXMLExpressionException e) {
			throw e;
		}
		
		if(obj == null)
			return null;
		else if(obj instanceof Node)
			return (Node)obj;
		else {
			Node ret = null;
			try {
				ret = (Node)obj;
			}
			catch(Exception e) {
				ret = null;
			}
			return ret;
		}
	}

	@Override
	public Context newContext(Context parent) {
		if(parent != null) return parent;
		
		JsContext ret = null;
		try {
			//ret = new JsContext(parent);
			ret = new JsContext();
		}
		catch(IllegalArgumentException e) {
			ret = null;
		}
		return ret;
	}

}
