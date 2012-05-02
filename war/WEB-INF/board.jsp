<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<link type="text/css" rel="stylesheet" href="/css/game.css"></link>
<script type="text/javascript" src="/js/lib/jquery-1.7.1.min.js"></script>
<script type="text/javascript" src="/js/lib/event.js"></script> 
<script type="text/javascript" src="/_ah/channel/jsapi"></script>
<script type="text/javascript" src="/js/model/board.js"></script>
<script type="text/javascript" src="/js/lib/transform2d.js"></script>
<script type="text/javascript" src="/js/view/diceView.js"></script>
<script type="text/javascript" src="/js/view/boardView.js"></script>
<script type="text/javascript" src="/js/view/playersView.js"></script>
<script type="text/javascript" src="/js/view/layout.js"></script>
<script type="text/javascript">//<![CDATA[

function handleLoad() {
	var channeltoken = "${channeltoken}";
	//var board = new Board(channeltoken, "/game/${gameid}/datamodel/${boarddatamember}");
	var board = new Board(channeltoken, "${boarddatamemberurl}", "${boardactionurl}");
	
	var layout = new Layout();
	
	
	var boardView = new BoardView($("#game-board"), [
   	    {type: "settlement", element:document.getElementById("settlement-model"), center:{x:15,y:15}},
   	    {type: "city", 		 element:document.getElementById("city-model"), 	  center:{x:25,y:15}},
   	    {type: "robber", 	 element:document.getElementById("robber-model"), 	  center:{x:40,y:30}}
   	]);
	
	boardView.setBoard(board);
	
	var playersView = new PlayersView($("#players"));
	
	playersView.setBoard(board);
	
	
	Event.addListener(boardView, "vertexclick", function(vertex, evt) { 
		var responder = board.sendAction("vertexClick", 
			{"vertex": {"x": vertex.x_, "y": vertex.y_}}
		); 
		Event.addListener(responder, "error", handleActionError);
	});
	Event.addListener(boardView, "edgeclick", function(edge, evt) { 
		var responder = board.sendAction("edgeClick", 
			{"edge": {"x1": edge.x1_, "y1": edge.y1_, "x2": edge.x2_, "y2": edge.y2_}}
		);
		Event.addListener(responder, "error", handleActionError);
	});
	Event.addListener(boardView, "diceclick", function() {
		console.log("board.jsp diceclick handler");
		var responder = board.sendAction("diceClick");
		Event.addListener(responder, "error", handleActionError);
	});
	Event.addListener(playersView, "playerclick", function(p) {
		console.log("player click: " + p.color_);
		var responder = board.sendAction("playerClick", [p.playerId_, p.color_]);
		Event.addListener(responder, "error", handleActionError);
	});
	
	layout.addItem(document.getElementById("header"), "top", 200);
	layout.addItem(document.getElementById("game-board"), "center");
	layout.addItem(document.getElementById("players"), "right");

	$('#endTurnButton').click(function() {
		board.sendAction("endTurn");
	});

	board.load();
}

function handleActionError(error, status, jqXHR) {
	var response = jQuery.parseJSON(jqXHR.responseText);
	console.log("sendAction Error: " + response.error);
}

$(handleLoad);

//]]></script>
<title>Board</title>
</head>
<body>
<div id="header">
<h1>Game: ${gameid}, Board Data Member: ${boarddatamember}</h1>

<form action="${joingameurl}" method="post">
	<input type="submit" value="JOIN" />
</form>

<form action="${startgameurl}" method="post">
	<input type="submit" value="START" />
</form>

<input type="submit" value="END TURN" id="endTurnButton" />

</div>
<div id="game-board"></div>

<div id="players"></div>



<svg xmlns="http://www.w3.org/2000/svg" style="display:none;" version="1.1" baseProfile="full" width="100%" height="100%">
	<defs>
    <marker id="Triangle"
      viewBox="0 0 10 10" refX="0" refY="5" 
      markerUnits="strokeWidth"
      markerWidth="4" markerHeight="3"
      orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 z" />
    </marker>
  </defs>
    <g id="prototypes" style="visibility:hidden;">
        <svg id="settlement-model" y="331.41016151377545" x="635">
            <g>
                <rect  width="30" height="30"
                       style="stroke: black;"/>
                <line x1="15" y1="0" x2="15" y2="30" style="stroke:black;" />
            </g>

        </svg>
        <svg id="city-model" x="325" y="158.20508075688772">
            <g>
                <rect  x="0" y="0" width="50" height="30"
                       style="stroke: black;"/>
                <line x1="15" y1="0" x2="15" y2="30" style="stroke:black;" />
                <line x1="30" y1="0" x2="30" y2="30" style="stroke:black;" />
            </g>
        </svg>
        <svg id="robber-model">
        	<g>
        		<circle cx="16" cy="16" r="15" style="stroke:black;fill:#444444;" />
        	</g>
        </svg>
    </g>
</svg>

</body>
</html>