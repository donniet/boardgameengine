import java.util.List;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.env.AbstractStateMachine;
import org.apache.commons.scxml.env.jexl.JexlContext;
import org.apache.commons.scxml.env.jexl.JexlEvaluator;
import org.apache.commons.scxml.io.SCXMLParser;
import org.apache.commons.scxml.model.CustomAction;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.SCXML;
import org.boardgameengine.scxml.js.JsContext;
import org.boardgameengine.scxml.js.JsEvaluator;
import org.boardgameengine.scxml.model.Error;
import org.boardgameengine.scxml.model.Script;
import org.xml.sax.ErrorHandler;
import org.xml.sax.helpers.DefaultHandler;


public class TestStateMachine {
	public SCXMLExecutor exec = null;
	
	public TestStateMachine(final URL uri) {
		List<CustomAction> customActions = new ArrayList<CustomAction>();
		customActions.add(new CustomAction("http://test.com/scxmltest", "error", Error.class));
		customActions.add(new CustomAction("http://test.com/scxmltest", "script", Script.class));
				
		SCXML scxml = null;
		try {
			ErrorHandler errHandler = new DefaultHandler();
			scxml = SCXMLParser.parse(uri, errHandler, customActions);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		exec = new SCXMLExecutor();
		exec.setStateMachine(scxml);
		
		final Log log = LogFactory.getLog(TestStateMachine.class);
		
		JsContext ctx = new JsContext();		
		JsEvaluator eval = new JsEvaluator();
		
		exec.setEvaluator(eval);
		exec.setRootContext(ctx);	
		exec.setErrorReporter(new ErrorReporter() {
			@Override
			public void onError(String errCode, String errDetail, Object errCtx) {
				log.error(errCode + ": " + errDetail);				
			}
		});
	}
	
	public void go() {
		try {
			exec.go();
		} catch (ModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
