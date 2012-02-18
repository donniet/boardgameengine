import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.TriggerEvent;
import org.apache.commons.scxml.io.SCXMLParser;
import org.apache.commons.scxml.model.SCXML;
import org.boardgameengine.scxml.js.JsContext;
import org.boardgameengine.scxml.js.JsEvaluator;
import org.xml.sax.ErrorHandler;
import org.xml.sax.helpers.DefaultHandler;


public class Main2 {

	public static void main(String[] args) {

		try {
			SCXMLExecutor exec = null;
			
			final Log log = LogFactory.getLog(Main2.class);
			
			ErrorHandler errHandler = new DefaultHandler();
			SCXML scxml = SCXMLParser.parse(Main2.class.getResource("/test2.xml"), errHandler); //, customActions);
			
			exec = new SCXMLExecutor();
			exec.setStateMachine(scxml);
			
			
			JsEvaluator eval = new JsEvaluator();
			JsContext context = new JsContext();
			
			exec.setEvaluator(eval);
			exec.setRootContext(context);	
			exec.setErrorReporter(new ErrorReporter() {
				@Override
				public void onError(String errCode, String errDetail, Object errCtx) {
					log.error(errCode + ": " + errDetail);				
				}
			});
			exec.go();
			
			EventPayload payload = new EventPayload(0, new EventPayload.Point(1,2));
			
			
			exec.triggerEvent(new TriggerEvent("game.playerJoin", TriggerEvent.SIGNAL_EVENT, payload));
			payload.setPlayer(1);
			
			exec.triggerEvent(new TriggerEvent("game.playerJoin", TriggerEvent.SIGNAL_EVENT, payload));
			exec.triggerEvent(new TriggerEvent("game.startGame",  TriggerEvent.SIGNAL_EVENT));
			

			//context.setLocal("blah", 1);
			Object util = eval.evalData(context, "{\"blue\":2}");
			context.setLocal("util2", util);
			//eval.eval(context, "template = {\"blau\":3};");

			//System.out.println(eval.eval(context, "blah;").toString());			
			//System.out.println(eval.eval(context, "util.blue;"));			
			//System.out.println(eval.eval(context, "template.blau;"));
			
			
			byte[] serializedContext = context.serializeScope();
			log.info("Byte Array Size: " + serializedContext.length);
			
			
			context = new JsContext();
			eval = new JsEvaluator();
			
			
			context.deserializeScope(serializedContext);
			
			payload.setPlayer(0);
			exec.triggerEvent(new TriggerEvent("board.vertexClick",  TriggerEvent.SIGNAL_EVENT, payload));
		

			/*
			System.out.println(eval.eval(context, "blah;").toString());			
			System.out.println(eval.eval(context, "util.blue;"));			
			System.out.println(eval.eval(context, "template.blau;"));
			*/
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
