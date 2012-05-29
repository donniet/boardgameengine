package org.boardgameengine.scxml.js;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.Builtin;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;
import org.ringojs.util.ScriptUtils;


public class JsContext
    implements org.apache.commons.scxml.Context, Serializable {

    private static final long serialVersionUID = 1L;
    public static final long MAXCONTEXTDEPTH = 100L;
    
    public static final String IN_FUNC_JS = "function(s){return _builtin.isMember(_ALL_STATES,s);}";
    public static final String DATA_FUNC_JS = "function(obj, xpath){return _builtin.data(_ALL_NAMESPACES, obj, xpath);}";
    
    private ContextFactory factory = null;
    private ScriptableObject rootScope = null;
    private Scriptable scope = null;
    
    protected ContextFactory getFactory() {
    	return factory;
    }
    public Scriptable getScope() {
    	return scope;
    }
    protected Scriptable getRootScope() {
    	return rootScope;
    }
    protected void setScope(Scriptable scope) {
    	this.scope = scope;
    }
    
    public byte[] serializeScope() throws IOException {
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	
    	Scriptable s = getScope();
    	
		
		ScriptableOutputStream out = new ScriptableOutputStream(bos, getRootScope());
		out.writeObject(getScope());
		
		bos.toByteArray();
		
		return bos.toByteArray();
    }
    
    public void deserializeScope(byte[] serialized) throws IOException, ClassNotFoundException {
    	ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
		ScriptableInputStream in = new ScriptableInputStream(bis, getRootScope());
		
		Object scope = in.readObject();
		setScope((Scriptable)scope);
    }
    
    public JsContext() {
        reset();
    }  
    
    
    @Override
    public void set(String name, Object value) {
    	setLocal(name, value);
    }

    @Override
    public void setLocal(final String name, final Object value) {
		ContextFactory factory = getFactory();
		factory.call(new ContextAction() {
			@Override
			public Object run(Context cx) {
				Log log = LogFactory.getLog(JsContext.class);
				
				Object wrapped = value;
				if(value instanceof org.w3c.dom.Node) {
					wrapped = ScriptUtils.javaToJS(value, scope);
					scope.put("__" + name, scope, wrapped);
					
					//TODO: I don't know a better way to get an e4x XML object!
					wrapped = cx.evaluateString(scope, "XML(__" + name + ")", "<wrapping>", 0, null);
					//scope.put(name, scope, wrapped);
					
					scope.delete("__" + name);
				}
				else if(!(value instanceof Scriptable)) {
					wrapped = ScriptUtils.javaToJS(value, scope);
					/*
					if(wrapped != null) {
						log.info(name + ": " + wrapped);
					}
					*/
				}
		        scope.put(name, scope, wrapped);
				return null;
			}
		});
    }
    //@Override
    public void removeLocal(final String name) {
    	ContextFactory factory = getFactory();
		factory.call(new ContextAction() {
			@Override
			public Object run(Context cx) {
				Log log = LogFactory.getLog(JsContext.class);
				
				scope.delete(name);
				return null;
			}
		});
    }

    @Override
    public Object get(String name) {
        if(scope.has(name, scope)) {
            return scope.get(name, scope);
        }
        else {
            return null;
        }
    }

    @Override
    public boolean has(String name) {
        if(scope.has(name, scope)) {
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public Map getVars() {
        Map ret = new HashMap<String, Object>();
        Object[] vars = scope.getIds();
        for(Object v : vars) {
        	if(v instanceof String) {
        		ret.put(v, scope.get((String)v, scope));
        	}
        	else if(v instanceof Number) {
        		ret.put(v.toString(), scope.get((Integer)v, scope));
        	}
        	else {
        		// should never get here
        	}
        }
        return ret;
    }

    @Override
    public void reset() {
		
		rootScope = new NativeObject(); // jsCtx.initStandardObjects();
		factory = new JsContextFactory();
		//factory = ContextFactory.getGlobal();
		
		factory.call(new ContextAction() {
			@Override
			public Object run(Context cx) {
				cx.setOptimizationLevel(1);
				
				Log log = LogFactory.getLog(JsContext.class);
				cx.initStandardObjects(rootScope);
				
				Object _builtin = ScriptUtils.javaToJS(new Builtin(), rootScope);
				rootScope.put("_builtin", rootScope, _builtin);
				rootScope.put("log", rootScope, log);

				Object InFunc = cx.evaluateString(rootScope, IN_FUNC_JS, "<builtin>", 1, null);
				Object DataFunc = cx.evaluateString(rootScope, DATA_FUNC_JS, "<builtin>", 1, null);
				//Object FuncFunc = cx.evaluateString(rootScope, "function() { return false; }", "<builtin>", 1, null);
				rootScope.put("In", rootScope, InFunc);
				rootScope.put("Data", rootScope, DataFunc);
				//rootScope.put("__dummy", rootScope, FuncFunc);
				/*
				try {
					new ScriptableOutputStream(new ByteArrayOutputStream(), rootScope);
				}
				catch(IOException e) {
					//TODO: stop ignoring this!
					e.printStackTrace();
				}
				*/

				rootScope.sealObject();
				
				scope = cx.newObject(rootScope);
				scope.setPrototype(rootScope);
				scope.setParentScope(null);

				return null;
			}
		});		
		

		//scope = jsCtx.initStandardObjects(rootScope);
		
		//scope.setParentScope(rootScope);
    }

    @Override
    public org.apache.commons.scxml.Context getParent() {
        return this;
    }

    
}
