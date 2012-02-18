import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;


public class Main3 {
	public static void main(String[] args) {
		try {
			final Log log = LogFactory.getLog(Main3.class);
			
			ContextFactory factory = ContextFactory.getGlobal();
			
			final ScriptableObject rootScope = new NativeObject();
			final Scriptable scope = (Scriptable)factory.call(new ContextAction() {
				@Override
				public Object run(Context cx) {
					cx.setOptimizationLevel(-1);
					cx.initStandardObjects(rootScope);
					
					rootScope.sealObject();
					
					Scriptable scope = cx.newObject(rootScope);
					scope.setPrototype(rootScope);
					scope.setParentScope(null);
					
					Object util = cx.evaluateString(scope, "function __dummy() {}", "<script>", 0, null);
										
					return scope;
				}
			});
						
			byte[] serializedContext;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			
			ScriptableOutputStream out = new ScriptableOutputStream(bos, rootScope);
			out.writeObject(scope);
			
			serializedContext = bos.toByteArray();
			log.info("Byte Array Size: " + serializedContext.length);
			
			
			final ScriptableObject rootScope2 = new NativeObject();
			factory.call(new ContextAction() {
				@Override
				public Object run(Context cx) {
					cx.setOptimizationLevel(-1);
					cx.initStandardObjects(rootScope2);
					
					rootScope2.sealObject();
					return null;
				}
			});
			
			
			ByteArrayInputStream bis = new ByteArrayInputStream(serializedContext);
			ScriptableInputStream in = new ScriptableInputStream(bis, rootScope);
			
			Object scope2 = in.readObject();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
