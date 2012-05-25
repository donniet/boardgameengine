
function JsonToXML(obj, node, doc, ns, maxDepth) {
	if(maxDepth <= 0 || !obj) return;
		
	for(var k in obj) {
		var v = obj[k];
		
		var re = /[^A-Za-z0-9\-\_]/g;
		var key = k.replace(re, "");
		var f = key.substring(0,1);
		if(f >= "0" && f <= "9") 
			key = "_" + key;
		
		switch(typeof v) {
		case "object":
			if(v == null || typeof v.length != "number") {
				var n = doc.createElementNS(ns, key);
				
				JsonToXML(v, n, doc, ns, maxDepth - 1);
				
				node.appendChild(n);				
			} 
			else {
				for(var i = 0; i < v.length; i++) {
					var n = doc.createElementNS(ns, key);
					
					JsonToXML(v[i], n, doc, ns, maxDepth - 1);
					
					node.appendChild(n);
				}
			}
			break;
		case "string":
		case "boolean":
		case "number":
			node.setAttribute(key, v);
			break;
		default:
			console.log("notsupported: " + k + "=" + v + ", " + typeof v);
			break;			
		}		
	}
}

function SerializeAsyncCalls(asyncFunctions) {
	var current = 0;
	var responder = null;
	
	var ret = new Object();
	
	var handler = function() {		
		delete responder;
		if(current < asyncFunctions.length) {
			var cur = asyncFunctions[current++];
			
			responder = cur.func.apply(cur.scope, arguments);
			
			Event.addListener(responder, cur.event, handler);
		}
		else {
			Event.fire(ret, "complete", arguments);
		}
	}
	
	handler();
	return ret;
}

function getTextContent(xml) {
	if(!xml) return null;
	
	var ret = "";
	for(var n = xml.firstChild; n; n = n.nextSibling) {
		if(n.nodeType == Node.TEXT_NODE) {
			ret += n.nodeValue;
		}
	}
	return ret;
}

function forChildNodes(node, nodehandlers, scope) {
	if(!scope) scope = this;
	
	if(node && nodehandlers) for(var n = node.firstChild; n; n = n.nextSibling) {
		if(n.nodeName) {
			var handler = nodehandlers[n.nodeName];
			if(typeof handler == "function") {
				handler.apply(scope, [n]);
			}
		}
	}
}

function boardNsResolver(prefix) {
	var ns = {
		"scxml": "http://www.w3.org/2005/07/scxml",
		"game": "http://www.pilgrimsofnatac.com/schemas/game.xsd"
	}
	return ns[prefix] || null;
}
/*
 * 
 * <?xml version="1.0" encoding="UTF-8"?>
 * <!--current player: aglub19hcHBfaWRyDgsSCEdhbWVVc2VyGAEM-->
 * <event name="board.resourcesDistributed">
 * 		<content>
 * 			<players xmlns="http://www.pilgrimsofnatac.com/schemas/game.xsd"><player><playerId>aglub19hcHBfaWRyDgsSCEdhbWVVc2VyGAEM</playerId><color>red</color><development count="3" type="settlement"/><development count="4" type="city"/><development count="13" type="road"/><resources><resource count="0" type="Ore"/><resource count="1" type="Grain"/><resource count="1" type="Wool"/><resource count="4" type="Brick"/><resource count="1" type="Wood"/></resources></player><player><playerId>aglub19hcHBfaWRyDgsSCEdhbWVVc2VyGAEM</playerId><color>green</color><development count="3" type="settlement"/><development count="4" type="city"/><development count="13" type="road"/><resources><resource count="2" type="Ore"/><resource count="1" type="Grain"/><resource count="1" type="Wool"/><resource count="3" type="Brick"/><resource count="0" type="Wood"/></resources></player><player><playerId>aglub19hcHBfaWRyDgsSCEdhbWVVc2VyGAEM</playerId><color>blue</color><development count="3" type="settlement"/><development count="4" type="city"/><development count="13" type="road"/><resources><resource count="2" type="Ore"/><resource count="2" type="Grain"/><resource count="0" type="Wool"/><resource count="3" type="Brick"/><resource count="0" type="Wood"/></resources></player><player><playerId>aglub19hcHBfaWRyDgsSCEdhbWVVc2VyGAEM</playerId><color>orange</color><development count="3" type="settlement"/><development count="4" type="city"/><development count="13" type="road"/><resources><resource count="2" type="Ore"/><resource count="0" type="Grain"/><resource count="0" type="Wool"/><resource count="3" type="Brick"/><resource count="0" type="Wood"/></resources></player></players>
 * 		</content>
 * </event>
 */

function GameEvent() {
	this.event_ = "";
	this.params_ = new Object();
	this.content_ = null;
}
GameEvent.prototype.loadXML = function(xml) {
	console.log("GameEvent: loading xml: " + xml);
	forChildNodes(xml, {
		"event": this.loadEventNode
	}, this);
};
GameEvent.prototype.loadEventNode = function(xml) {
	console.log("GameEvent: loadingEventNode: " + xml);
	this.event_ = xml.getAttribute("name");
	forChildNodes(xml, {
		"param": this.loadParam,
		"content": this.loadContent
	}, this);
};
GameEvent.prototype.loadParam = function(xml) {
	console.log("GameEvent: loading param: " + xml);
	this.params_[xml.getAttribute("name")] = getTextContent(xml);
};
GameEvent.prototype.loadContent = function(xml) {
	console.log("GameEvent: loading content: " + xml);
	this.content_ = xml;
};


function Board(token, boardUrl, actionUrl, detailsUrl) {
	this.modelElements_ = new Object();
	this.boardUrl_ = boardUrl;
	this.actionUrl_ = actionUrl;
	this.detailsUrl_ = detailsUrl;
	this.userToken_ = token;
	this.channel_ = null;
	this.socket_ = null;
	this.gameDetails_ = null;
		
	this.poly_ = new Array();
	this.edge_ = new Array();
	this.vertex_ = new Array();
	this.polytype_ = new Array();
	this.dice_ = new Array();
	this.port_ = new Array();
	
	this.socketMessageHandlers_ = {
		"board.placeVertexDevelopment": this.handlePlaceVertexDevelopment,
		"board.placeEdgeDevelopment": this.handlePlaceEdgeDevelopment,
		"board.diceRolled": this.handleDiceRoll,
		"board.resourcesDistributed": this.handleResourcesDistributed,
		"board.currentPlayerChanged": this.handleCurrentPlayerChange
	};
	
	this.player_ = new Array();
	
	this.currentPlayer_ = null;
	this.currentVertex_ = null;
	
	this.myself_ = null;
}

Board.prototype.createChannel = function() {
	this.channel_ = new goog.appengine.Channel(this.userToken_);
	this.socket_ = this.channel_.open();
	
	var self = this;
	this.socket_.onopen = function() {
		self.connected_ = true;
		
		Event.fire(self, "socketOpen", []);
	};
	this.socket_.onmessage = function() {
		self.handleSocketMessage.apply(self, arguments);
	};
	this.socket_.onerror = function() {
		self.handleSocketError.apply(self, arguments);		
	};
	this.socket_.onclose = function() {
		self.handleSocketClose.apply(self, arguments);
	};
}

Board.prototype.handleSocketError = function() {
	Event.fire(this, "error", ["There was an error connecting to the server.  Please refresh your browser window.", true]);
}
Board.prototype.handleSocketClose = function() {
	self.connected_ = false;
	
	Event.fire(self, "socketClose", []);
	Event.fire(this, "error", ["Communication with the server has been lost.  Please refresh your browser window.", true]);
}
Board.prototype.handleSocketMessage = function(msg) {
	console.log("entire socket message: " + msg.data);
	var event = new GameEvent();
	event.loadXML(( new window.DOMParser() ).parseFromString(msg.data, "text/xml"));
		
	console.log("socket message: " + event.event_);
	
	var handler = this.socketMessageHandlers_[event.event_];
	
	if(typeof handler == "function") {
		handler.apply(this, [event]);
	}
}

Board.prototype.handleCurrentPlayerChange = function(event) {
	console.log("currentPlayerChange: " + event.params_.currentPlayer);
	
	this.currentPlayer_ = parseInt(event.params_.currentPlayer);
	
	Event.fire(this, "currentPlayerChange", [this.currentPlayer_]);
}
Board.prototype.handleDiceRoll = function(event) {
	this.dice_ = new Array();
	
	if(event.params_.diceValues != null) {
		var values = event.params_.diceValues.split(" ");
		for(var i = 0; i < values.length; i++) {
			if(values[i] != "")
				this.dice_.push(parseInt(values[i]));
		}
		console.log("handleDiceRoll: " + (this.dice_[0] + this.dice_[1]));
		Event.fire(this, "diceRolled", [this.dice_]);
	}
}

Board.prototype.handleResourcesDistributed = function(event) {
	forChildNodes(event.content_, {
		"players": function(xml) {
			this.loadPlayers(xml);
		}
	}, this);
	
	Event.fire(this, "resourcesUpdated", [this.players_]);
}

Board.prototype.handlePlaceVertexDevelopment = function(event) {
	var d = new Development();
	d.count_ = 1;
	d.type_ = event.params_.type;
	d.color_ = event.params_.color;
	
	var vertex = null;
	for(var i = 0; i < this.vertex_.length; i++) {
		var v = this.vertex_[i];
		if(v.x_ == event.params_.x >>> 0 && v.y_ == event.params_.y >>> 0) {
			vertex = v;
			break;
		}
	}
	console.log("handleSocketMessage: found: " + vertex);
	if(vertex != null) {
		vertex.development_.push(d);
		Event.fire(this, "placeVertexDevelopment", [vertex, d]);
	}
}
Board.prototype.handlePlaceEdgeDevelopment = function(event) {
	var d = new Development();
	d.count_ = 1;
	d.type_ = event.params_.type;
	d.color_ = event.params_.color;
	
	var edge = null;
	for(var i = 0; i < this.edge_.length; i++) {
		var e = this.edge_[i];
		if(e.x1_ == event.params_.x1 >>> 0 && e.y1_ == event.params_.y1 >>> 0 && e.x2_ == event.params_.x2 >>> 0 && e.y2_ == event.params_.y2 >>> 0) {
			edge = e;
			break;
		}
	}
	console.log("handleSocketMessage: found: " + edge);
	if(edge != null) {
		edge.development_.push(d);
		Event.fire(this, "placeEdgeDevelopment", [edge, d]);
	}
}

Board.prototype.getDice = function() { return this.dice_; };

Board.prototype.sendAction = function(action, data) {
	var responder = new Object();
	
	var url = this.actionUrl_ + action;
	var ns = boardNsResolver("game");
	
	var serializer = new XMLSerializer();
		
	var doc = document.implementation.createDocument(ns, "data", null);
	var n = doc.createElementNS(ns, "data");
	doc.firstChild.appendChild(n);
	
	JsonToXML(data, n, doc, ns, 10);
	
	var datastring = null;
	if(n.childNodes.length == 1) {
		datastring = serializer.serializeToString(n.firstChild);
	}
	else {
		datastring = serializer.serializeToString(n);
	}
	
	jQuery.ajax({
		type: "POST",
		url: url,
		data: datastring,
		dataType: "json",
		success: function(data,status,jqXHR) { Event.fire(responder, "load", [data, status, jqXHR]); },
		error: function(jqXHR,status,error) { Event.fire(responder, "error", [error, status, jqXHR]); }
	});	
	
	return responder;
}

Board.prototype.load = function() {
	var self = this;
	
	var responder = SerializeAsyncCalls([
	    { func: this.loadBoard,   event: "load", scope: this },
	    { func: this.loadDetails, event: "load", scope: this }
	]);
	
	Event.addListener(responder, "complete", function() {
		self.createChannel();
		Event.fire(self, "load", [self]);
	});
}

Board.prototype.loadBoard = function() {
	var self = this;
	var responder = new Object();
	
	// load board xml
	jQuery.ajax(this.boardUrl_, {
		async: true,
		cache: false,
		context: this,
		complete: function(xhr, status) {
			var xml = xhr.responseXML;
			var boardnode = null;
			
			
			var evaluator = new XPathEvaluator();
			boardnode = evaluator.evaluate("//scxml:data/game:board", xml, boardNsResolver, XPathResult.FIRST_ORDERED_NODE_TYPE, null);

			if(!boardnode || !boardnode.singleNodeValue) {
				Event.fire(responder, "error", []);
			}
			else {
				this.loadXML(boardnode.singleNodeValue);
				Event.fire(responder, "load", []);
			}
			
		},
	});
	
	return responder;
}

Board.prototype.loadDetails = function() {
	var self = this;
	var responder = new Object();
	
	jQuery.ajax(this.detailsUrl_, {
		async: true,
		cache: false,
		context: this,
		dataType: "json",
		success: function(data, status, xhr) {
			this.handleLoadDetails(data);			
			Event.fire(responder, "load", []);
		},
		error: function() {
			Event.fire(responder, "error", []);
		}
	});
	
	return responder;
}

Board.prototype.handleLoadDetails = function(details) {
	console.log("handling details...");
	
	for(var i = 0; i < this.player_.length; i++) {
		var p = this.player_[i];
		console.log(p.color_);
		for(var j = 0; j < details.players.length; j++) {
			var dp = details.players[j];
			
			if(p.color_ == dp.role) {
				p.hashedEmail_ = dp.gameUser.hashedEmail;
				console.log(p.color_ + ": " + p.hashedEmail_);
			}
		}
	}
}

Board.prototype.loadXML = function(xml) {
	forChildNodes(xml, {
		"polys": this.loadPolys,
		"verteces": this.loadVerteces,
		"edges": this.loadEdges,
		"ports": this.loadPorts,
		"players": this.loadPlayers,
		"polytypes": this.loadPolytypes,
		"currentPlayer": function(n) {
			this.currentPlayer_ = parseInt(getTextContent(n));
		},
		"currentVertex": function(n) {
			this.currentVertex_ = {
				x: parseInt(n.getAttribute("x")),
				y: parseInt(n.getAttribute("y"))
			};
		},
		"dice": this.loadDice
	}, this);
}

Board.prototype.loadPlayers = function(xml) {
	this.player_ = new Array();
	forChildNodes(xml, {
		"player": function(n) {
			var p = new Player();
			p.loadXML(n);
			this.player_.push(p);
		}
	}, this);
}

Board.prototype.loadDice = function(xml) {
	this.dice_ = new Array();
	
	forChildNodes(xml, {
		"die": function(n) {
			this.dice_.push(parseInt(n.getAttribute("value")));
		}
	}, this);
	
}

Board.prototype.loadPolytypes = function(xml) {
	forChildNodes(xml, {
		"polytype": function(n) {
			var p = new Polytype();
			p.loadXML(n);
			this.polytype_.push(p);
		}
	}, this);
}

Board.prototype.loadPorts = function(xml) {
	forChildNodes(xml, {
		"port": function(n) {
			var p = new Port();
			p.loadXML(n);
			this.port_.push(p);
		}
	}, this);
}

Board.prototype.loadPolys = function(xml) {
	forChildNodes(xml, {
		"poly": function(n) {
			var p = new Poly();
			p.loadXML(n);
			this.poly_.push(p);
		}
	}, this);
}

Board.prototype.loadVerteces = function(xml) {
	forChildNodes(xml, {
		"vertex": function(n) {
			var v = new Vertex();
			v.loadXML(n);
			this.vertex_.push(v);			
		}
	}, this);
}

Board.prototype.loadEdges = function(xml) {
	forChildNodes(xml, {
		"edge": function(n) {
			var v = new Edge();
			v.loadXML(n);
			this.edge_.push(v);
		}
	}, this);
}

function Player() {
	this.developments_ = new Array();
	this.resources_ = new Array();
}
Player.prototype.loadXML = function(xml) {
	forChildNodes(xml, {
		"playerId": function(n) {
			this.playerId_ = getTextContent(n);
		},
		"color": function(n) {
			this.color_ = getTextContent(n);
		},
		"development": function(n) {
			var d = {
				type: n.getAttribute("type"),
				count: parseInt(n.getAttribute("count"))
			};
			this.developments_.push(d);
		},
		"resources": this.loadResources
	}, this);
};
Player.prototype.getResources = function() {
	return this.resources_;
}
Player.prototype.loadResources = function(xml) {
	forChildNodes(xml, {
		"resource": function(n) {
			var pr = new PlayerResource();
			pr.loadXML(n);
			this.resources_.push(pr);
		}
	}, this);
}

function PlayerResource() {}
PlayerResource.prototype.loadXML = function(xml) {
	this.type_ = xml.getAttribute("type");
	this.count_ = parseInt(xml.getAttribute("count"));
}

function Vertex() {
	this.development_ = new Array();
}
Vertex.prototype.loadXML = function(xml) {
	this.x_ = parseInt(xml.getAttribute("x"));
	this.y_ = parseInt(xml.getAttribute("y"));
	
	forChildNodes(xml, {
		"development": function(n) {
			var d = new Development();
			d.loadXML(n);
			this.development_.push(d);
		}
	}, this);
}

function Edge() {
	this.development_ = new Array();
}
Edge.prototype.loadXML = function(xml) {
	this.x1_ = parseInt(xml.getAttribute("x1"));
	this.y1_ = parseInt(xml.getAttribute("y1"));
	this.x2_ = parseInt(xml.getAttribute("x2"));
	this.y2_ = parseInt(xml.getAttribute("y2"));
	
	forChildNodes(xml, {
		"development": function(n) {
			var d = new Development();
			d.loadXML(n);
			this.development_.push(d);
		}
	}, this);
}


function Poly(x, y, type, value) {
	this.x_ = x;
	this.y_ = y;
	this.type_ = type;
	this.value_ = value;
	
	this.development_ = new Array();
}
Poly.prototype.loadXML = function(xml) {
	this.x_ = parseInt(xml.getAttribute("x"));
	this.y_ = parseInt(xml.getAttribute("y"));
	this.type_ = xml.getAttribute("type");
	this.value_ = parseInt(xml.getAttribute("value"));
	
	forChildNodes(xml, {
		"development": function(n) {
			var d = new Development();
			d.loadXML(n);
			this.development_.push(d);
		}
	}, this);
}

function Polytype() {}
Polytype.prototype.loadXML = function(xml) {
	this.count_ = parseInt(xml.getAttribute("count"));
	this.produces_ = xml.getAttribute("produces") ? xml.getAttribute("produces") : null;
	this.type_ = xml.getAttribute("type");
}

function Development() {}
Development.prototype.loadXML = function(xml) {
	this.count_ = parseInt(xml.getAttribute("count"));
	this.color_ = xml.getAttribute("color");
	this.type_ = xml.getAttribute("type");
}

function Port() {}
Port.prototype.loadXML = function(xml) {
	this.resource_ = xml.getAttribute("resource");
	this.tradeIn_ = parseInt(xml.getAttribute("tradeIn"));
	this.tradeOut_ = parseInt(xml.getAttribute("tradeOut"));
	this.x_ = parseInt(xml.getAttribute("x"));
	this.y_ = parseInt(xml.getAttribute("y"));
}