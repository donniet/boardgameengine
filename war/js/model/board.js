
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


function Board(token, boardUrl, actionUrl) {
	this.modelElements_ = new Object();
	this.boardUrl_ = boardUrl;
	this.actionUrl_ = actionUrl;
	this.userToken_ = token;
	this.channel_ = null;
	this.socket_ = null;
		
	this.poly_ = new Array();
	this.edge_ = new Array();
	this.vertex_ = new Array();
	this.polytype_ = new Array();
	this.dice_ = new Array();
	this.port_ = new Array();
	
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
	
}

Board.prototype.sendAction = function(action, data) {
	var responder = new Object();
	
	var url = this.actionUrl_ + action;
	
	
	
	return responder;
}

Board.prototype.load = function() {
	var self = this;
	var responder = new Object();
	
	jQuery.ajax(this.boardUrl_, {
		async: true,
		cache: false,
		complete: function(xhr, status) {
			var xml = xhr.responseXML;
			var boardnode = null;
			
			
			var evaluator = new XPathEvaluator();
			boardnode = evaluator.evaluate("//scxml:data/game:board", xml, boardNsResolver, XPathResult.FIRST_ORDERED_NODE_TYPE, null);

			if(!boardnode || !boardnode.singleNodeValue) {
				Event.fire(self, "loaderror", []);
			}
			else {
				self.loadXML(boardnode.singleNodeValue);
				Event.fire(self, "load", [self]);
			}
		},
	});
}

Board.prototype.loadXML = function(xml) {
	forChildNodes(xml, {
		"polys": this.loadPolys,
		"verteces": this.loadVerteces,
		"edges": this.loadEdges,
		"ports": this.loadPorts,
		"player": function(n) {
			var p = new Player();
			p.loadXML(n);
			this.player_.push(p);
		},
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
}
Player.prototype.loadXML = function(xml) {
	forChildNodes(xml, {
		"playerId": function(n) {
			this.playerId_ = getTextContent(n);
		},
		"development": function(n) {
			var d = {
				type: n.getAttribute("type"),
				count: parseInt(n.getAttribute("count"))
			};
			this.developments_.push(d);
		}
	}, this);
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
	this.player_ = xml.getAttribute("player");
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