package org.boardgameengine.scxml.js;
import org.boardgameengine.config.Config;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import org.mozilla.javascript.ContextFactory;


public class JsContextFactory extends ContextFactory {	
	private static class MyContext extends Context {
		long startTime;
	}
	
	static {
		ContextFactory.initGlobal(new JsContextFactory());
	}
	
	protected Context makeContext() {
		MyContext cx = new MyContext();
		cx.setOptimizationLevel(-1);
		cx.setInstructionObserverThreshold(10000);
		return cx;
	}
	
	protected void observeInstructionCount(Context cx, int instructionCount) {
		
		MyContext mcx = (MyContext)cx;
		long currentTime = System.currentTimeMillis();
		if(currentTime - mcx.startTime > Config.getInstance().getMaxScriptRuntime() * 1000) {
			throw new java.lang.Error();
		}
	}
	
	protected Object doTopCall(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] arg) {
		MyContext mcx = (MyContext)cx;
		mcx.startTime = System.currentTimeMillis();
		
		return super.doTopCall(callable, mcx, scope, thisObj, arg);
	}
}
