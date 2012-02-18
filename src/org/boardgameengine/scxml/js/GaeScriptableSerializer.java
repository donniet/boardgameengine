package org.boardgameengine.scxml.js;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.HashSet;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;

public class GaeScriptableSerializer {
	private OutputStream os = null;
	private Scriptable scope = null;
	private PrintWriter pr = null;
	private HashMap<Object,String> set = null;
	
	
	public GaeScriptableSerializer(final OutputStream os, final Scriptable scope) {
		this.os = os;
		this.scope = scope;
		this.pr = new PrintWriter(os);
		this.set = new HashMap<Object,String>();
	}
	public void Serialize(final Object obj) {
		ContextFactory factory = new JsContextFactory();
		
		final GaeScriptableSerializer self = this;
		factory.call(new ContextAction() {
			@Override
			public Object run(Context cx) {
				Serialize((Scriptable)obj, obj, "/", 0);
				return null;
			}
		});		
		
		pr.flush();
	}
	private void writeTabs(int tab) {
		while(tab-- > 0) pr.write("\t");
	}
	public void Serialize(Scriptable start, Object obj, String loc, int tab) {
		if(obj == null) {
			pr.write("null");
		}
		else if(set.containsKey(obj)) {
			pr.write("/* [" + obj.getClass().getName() + " " + set.get(obj) + "] */");
		}
		else if(obj instanceof String) {
			pr.write("\"" + obj.toString() + "\"");
		}
		else if(obj instanceof Number) {
			pr.write(obj.toString());
		}
		else if(obj instanceof UniqueTag) {
			UniqueTag u = (UniqueTag)obj;
			
			if(u.equals(UniqueTag.NULL_VALUE)) {
				pr.write("null");
			}
			else if(u.equals(UniqueTag.NOT_FOUND)) {
				pr.write("__undefined__");
			}
			else if(u.equals(UniqueTag.DOUBLE_MARK)) {
				pr.write("__double_mark__");
			}
/*
			set.put(obj,loc);
			
			Serialize(u.readResolve());
*/	
		}
		else if(obj instanceof Scriptable) {
			Scriptable s = (Scriptable)obj;
			String className = s.getClassName();
			
			Object[] ids = s.getIds();
			
			pr.write("{\n");
			writeTabs(tab);
			
			for(Object o : ids) {
				Object val = null;
				String nloc = loc + "/";
				if(o instanceof String) {
					val = s.get((String)o, s);
					pr.write("\"" + o.toString() + "\":");
					nloc += (String)o;
				}
				else {
					val = s.get((Integer)o, s);
					pr.write("" + o.toString() + ":");
					nloc += o.toString(); 
				}
				
				set.put(obj, loc);
				
				if(val == null) {
					pr.write("null,\n");
					writeTabs(tab);
				}
				else {
					Serialize(start, val, nloc, tab + 1);
					pr.write(",\n");
					writeTabs(tab);
				}
					
			}
			pr.write("}\n");
			writeTabs(tab);
		}
		else {
			pr.write("/* " + obj.getClass().getName() + " */\n");
			writeTabs(tab);
		}
	}
}
