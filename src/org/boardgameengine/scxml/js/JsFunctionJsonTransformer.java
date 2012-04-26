package org.boardgameengine.scxml.js;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import flexjson.transformer.AbstractTransformer;
import flexjson.transformer.Transformer;

public class JsFunctionJsonTransformer extends AbstractTransformer {

	@Override
	public void transform(Object object) {
		if(Function.class.isInstance(object)) {
			Function f = (Function)object;
			getContext().writeQuoted(f.toString());
		}
		else if(Scriptable.class.isInstance(object)) {
			Scriptable s = (Scriptable)object;
			getContext().writeQuoted(s.toString());
		}
	}

}
