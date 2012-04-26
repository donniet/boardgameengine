

function BoardView(boardContainer, modelElements, options) {
	this.svgns_ = "http://www.w3.org/2000/svg";
	this.edgeLength_ = !options || typeof options.edgeLength == "undefined" ? 85 : options.edegeLength;
	this.sqrt3over2_ = Math.sqrt(3)/2;
    this.marginLeft_ = 20;
    this.marginTop_ = 20;
	this.boardElement_ = null;
	this.diceElement_ = null;
	this.dicePosition_ = "bottom right";
	this.diceView_ = null;
	this.boardContainer_ = boardContainer;
	this.vertexDevListener_ = null;
	this.edgeDevListener_ = null;
	this.hexDevListener_ = null;
	this.removeHexDevListener_ = null;
	this.loadListener_ = null;
	this.scale_ = 1.0;
	this.totalWidth_ = 0;
	this.totalHeight_ = 0;
	
	this.centerPosition_ = {x:0,y:0};
	
	this.modelElements_ = new Object();
	for(var i = 0; modelElements && i < modelElements.length; i++) {
		var m = modelElements[i];
		this.setModelElement(m.type, m.element, m.center);
	}
	
	this.board_ = null;
	
	var self = this;
}

BoardView.prototype.hexCoords = function (nx, ny) {
    return [
      this.c(nx + 1, ny),
      this.c(nx + 3, ny),
      this.c(nx + 4, ny + 1),
      this.c(nx + 3, ny + 2),
      this.c(nx + 1, ny + 2),
      this.c(nx + 0, ny + 1)
   ];
}
BoardView.prototype.setBoard = function(board) {
	if(this.board_) {
		Event.removeListenerById(this.board_, "placeVertexDevelopment", this.vertexDevListener_);
		Event.removeListenerById(this.board_, "placeEdgeDevelopment", this.edgeDevListener_);
		Event.removeListenerById(this.board_, "placeHexDevelopment", this.hexDevListener_);
		Event.removeListenerById(this.board_, "removeHexDevelopment", this.removeHexDevListener_);
		Event.removeListenerById(this.board_, "load", this.loadListener_);
	}
	
	this.board_ = board;
	
	var self = this;

	this.vertexDevListener_ = Event.addListener(board, "placeVertexDevelopment", function() {
		self.handlePlaceVertexDevelopment.apply(self, arguments);
	});

	this.edgeDevListener_ = Event.addListener(board, "placeEdgeDevelopment", function() {
		self.handlePlaceEdgeDevelopment.apply(self, arguments);
	});
	this.hexDevListener_ = Event.addListener(board, "placeHexDevelopment", function() {
		self.handlePlaceHexDevelopment.apply(self, arguments);
	});
	this.removeHexDevListener_ = Event.addListener(board, "removeHexDevelopment", function() {
		self.handleRemoveHexDevelopment.apply(self, arguments);
	});
	
	this.loadListener_ = Event.addListener(board, "load", function() {
		self.render();
	});
	
}

BoardView.prototype.handlePlaceVertexDevelopment = function(vertex, development) {
	if (vertex.svgEl_) {
		this.renderVertexDevelopment(vertex, development, vertex.svgEl_);
	}
}

BoardView.prototype.handlePlaceEdgeDevelopment = function(edge, development) {
	if (edge.svgEl_ ) {
	    this.renderEdgeDevelopment(edge, development, edge.svgEl_);
	}
}
BoardView.prototype.handlePlaceHexDevelopment = function(hex, development) {
	if(hex.svgEl_) {
		this.renderHexDevelopment(hex, development, hex.svgEl_);
	}
}
BoardView.prototype.handleRemoveHexDevelopment = function(hex, development) {
	if(hex.svgEl_ && development.svgEl_) {
		this.removeHexDevelopment(hex, development, hex.svgEl_, development.svgEl_);
	}
}

BoardView.prototype.render = function() {
	this.diceElement_ = $("<div/>");
	this.diceElement_.css("position", "absolute");
	var pos = this.dicePosition_.split(" ");
	for(var i = 0; i < pos.length; i++) {
		this.diceElement_.css(pos[i], "0");
	}
	
	this.boardContainer_.append(this.diceElement_);
	this.diceView_ = new DiceView(this.diceElement_);
	this.diceView_.setBoard(this.board_);	
	
	var svg = document.createElementNS(this.svgns_, "svg");
	svg.setAttribute("version", "1.1");
	svg.setAttribute("baseProfile", "full");
	svg.setAttribute("width", "100%");
	svg.setAttribute("height", "100%");
	svg.setAttribute("id", "svg");
	
	this.boardElement_ = document.createElementNS(this.svgns_, "g");
	this.boardElement_.setAttribute("id", "board");
	
	svg.appendChild(this.boardElement_);
	$(this.boardContainer_).append(svg);		
	
	while(this.boardElement_.firstChild)
		this.boardElement_.removeChild(this.boardElement_.firstChild);
	this.renderBoard(this.boardElement_);
	
	enableBoardPanZoom(this.boardElement_, this.boardContainer_);
	
	Event.fire(this, "scale", [this.scale_ * this.totalWidth_, this.scale_ * this.totalHeight_]);
}
BoardView.prototype.setModelElement = function (modelName, svgEl, centerPosition) {
    this.modelElements_[modelName] = {
        "svgElement": svgEl,
        "centerPosition": centerPosition
    };
}

/* transform grid coords to pixel coords */
BoardView.prototype.c = function (/* int */nx, /* int */ny) {
    var fx = 0.5 * (nx + 1.2) * this.edgeLength_;
    var fy = this.sqrt3over2_ * (ny + 0.5) * this.edgeLength_;

    return {
        "x": fx + this.marginLeft_,
        "y": fy + this.marginTop_
    };
}
BoardView.prototype.handleMouseWheel = function(e) {
	var delta = 0;
	if(e.detail) {
		delta = -e.detail/3;
	}
	
	if(delta != 0) {
		this.scale_ *= Math.pow(1.2, delta);
		this.boardElement_.setAttribute("transform", "scale(" + this.scale_ + "," + this.scale_ + ")");
	}
	
	if (e.preventDefault)
        e.preventDefault();
	e.returnValue = false;
	
	Event.fire(this, "scale", [this.scale_ * this.totalWidth_, this.scale_ * this.totalHeight_]);
}
BoardView.prototype.renderBoard = function (svgEl) {
	this.calculateCenter();
	
    this.renderHexes(svgEl);
    this.renderEdges(svgEl);
    this.renderPorts(svgEl);
    this.renderVertexes(svgEl);
    
    /*
    var self = this;
    if(window.addEventListener) 
    	window.addEventListener('DOMMouseScroll', function(e) {self.handleMouseWheel(e);}, false);
    
    window.onmousewheel = function(e) { self.handleMouseWheel(e); };
    */
    
}
BoardView.prototype.calculateCenter = function() {
	var sx = 0, sy = 0;
    for (var i = 0; i < this.board_.vertex_.length; i++) {
    	var v = this.board_.vertex_[i];
    	
    	sx += v.x_;
    	sy += v.y_;
    }
    
    if(this.board_.vertex_.length > 0) {
    	this.centerPosition_ = {x: sx/this.board_.vertex_.length, y: sy/this.board_.vertex_.length};
    }
}
BoardView.prototype.renderPorts = function(svgEl) {
	var prev = null;
	for(var i = 0; i < this.board_.port_.length; i++) {
		var p = this.board_.port_[i];
		this.renderPort(p, i % 2 == 1 ? prev : null, svgEl);
		
		prev = p;
	}
}
BoardView.prototype.renderVertexes = function (svgEl) {
    for (var i = 0; i < this.board_.vertex_.length; i++) {
    	var v = this.board_.vertex_[i];
        this.renderVertex(v, svgEl);
    }
}
BoardView.prototype.renderVertex = function (vertex, svgEl) {
    var g = vertex.svgEl_;
    if (!g) {
        g = document.createElementNS(this.svgns_, "g");
        svgEl.appendChild(g);
        vertex.svgEl_ = g;
    }

    for (var i = 0; i < vertex.development_.length; i++) {
        this.renderVertexDevelopment(vertex, vertex.development_[i], g);
    }

    this.renderVertexHitArea(vertex, g);
}
BoardView.prototype.renderPort = function(port, prev, svgEl) {
	var g = port.svgEl_;
	if(!g) {
		g = document.createElementNS(this.svgns_, "g");
		g.setAttribute("class", "port");
		svgEl.appendChild(g);
		port.svgEl_ = g;
	}
	

	var pp = this.c(port.x_, port.y_);
	
    var c = svgEl.ownerDocument.createElementNS(this.svgns_, "circle");
    c.setAttribute("cx", pp.x);
    c.setAttribute("cy", pp.y);
    c.setAttribute("r", this.edgeLength_ * 0.125);
    g.appendChild(c);
    
    var tps = [null, {x:2,y:0}, {x:1, y:-1}];
    
    if(prev != null && (prev.resource_ == port.resource_ || (prev.resource_ == null && port.resource_ == null))) {
    	var g = document.createElementNS(this.svgns_, "g");
    	g.setAttribute("class", "port-marker port-" + (port.resource_ ? port.resource_ : "any"));
    	    	
    	var pps = [{x:port.x_,y:port.y_}, {x:prev.x_, y:prev.y_}];
    	pps.sort(function(a,b) { return a.x < b.x ? -1 : 1; });
    	
    	var p3 = tps[pps[1].x - pps[0].x];
    	
    	var ref = pps[0];
    	
    	if(pps[0].x > this.centerPosition_.x && p3.x < 0) p3.x = -p3.x;
    	else if(pps[0].x < this.centerPosition_.x && p3.x > 1) { ref = pps[1]; p3.x = -p3.x; }
    	if(pps[0].y > this.centerPosition_.y && p3.y < 0) p3.y = -p3.y;
    	else if(pps[0].y < this.centerPosition_.y && p3.y > 0) p3.y = -p3.y;
    	
    	p3.x += ref.x;
    	p3.y += ref.y;    	
    	
    	var pp = this.c(p3.x, p3.y);
    	var pp0 = this.c(pps[0].x, pps[0].y);
    	var pp1 = this.c(pps[1].x, pps[1].y);
    	
    	if(pp.x > this.totalWidth_) this.totalWidth_ = pp.x;
    	if(pp.y > this.totalHeight_) this.totalHeight_ = pp.y;
    	
    	var l1 = svgEl.ownerDocument.createElementNS(this.svgns_, "line");
    	l1.setAttribute("x1", pp.x); l1.setAttribute("x2", 0.875 * (pp0.x - pp.x) + pp.x);
    	l1.setAttribute("y1", pp.y); l1.setAttribute("y2", 0.875 * (pp0.y - pp.y) + pp.y);
    	g.appendChild(l1);
    	
    	var l2 = svgEl.ownerDocument.createElementNS(this.svgns_, "line");
    	l2.setAttribute("x1", pp.x); l2.setAttribute("x2", 0.875 * (pp1.x - pp.x) + pp.x);
    	l2.setAttribute("y1", pp.y); l2.setAttribute("y2", 0.875 * (pp1.y - pp.y) + pp.y);
    	g.appendChild(l2);
    	
    	var c = svgEl.ownerDocument.createElementNS(this.svgns_, "circle");
    	c.setAttribute("cx", pp.x);
    	c.setAttribute("cy", pp.y);
    	c.setAttribute("r", this.edgeLength_ * 0.2);
    	g.appendChild(c);
    	
    	var gtext = svgEl.ownerDocument.createElementNS(this.svgns_, "g");
    	    	
    	var txt = svgEl.ownerDocument.createElementNS(this.svgns_, "text");
    	prevp = this.c(prev.x_, prev.y_);
    	portp = this.c(port.x_, port.y_);
    	delta = {"x":-prevp.x + portp.x, "y":-prevp.y + portp.y};
    	
    	r = Math.sqrt(delta.x*delta.x + delta.y*delta.y);
    	if(r > 0.01) {
    		delta.x = delta.x / r;
    		delta.y = delta.y / r;
    		
    		txt.setAttribute("transform", "matrix(" + delta.x + "," + delta.y + "," + (-delta.y) + "," + delta.x + ",0,0)");
    		//txt.setAttribute("transform", "rotate(45)");
    	}
    	
	    txt.appendChild(svgEl.ownerDocument.createTextNode(port.tradeIn_ + ":" + port.tradeOut_));

	    gtext.setAttribute("transform", "translate(" + pp.x + "," + pp.y + ")");
	    gtext.appendChild(txt);
	    g.appendChild(gtext);
	    
	    svgEl.appendChild(g);
    	
    }
}
BoardView.prototype.renderVertexDevelopment = function (vertex, vertexDevelopment, svgEl) {
    var n = svgEl.firstChild;
    for (; n && n.getAttribute("class") != "development-container"; n = n.nextSibling) { }

    if (!n) {
        n = document.createElementNS(this.svgns_, "g");
        n.setAttribute("class", "development-container");
        if (svgEl.firstChild) svgEl.insertBefore(n, svgEl.firstChild);
        else svgEl.appendChild(n);
    }

    var model = null;
    if (model = this.modelElements_[vertexDevelopment.model]) {
        var newNode = model.svgElement.cloneNode(true);
        var newPos = this.c(vertex.x_, vertex.y_);
        newNode.setAttribute("x", newPos.x - model.centerPosition.x);
        newNode.setAttribute("y", newPos.y - model.centerPosition.y);
        newNode.setAttribute("class", "vertex-development " + vertexDevelopment.player);
        n.appendChild(newNode);
        vertexDevelopment.svgEl_ = newNode;
    }
}
BoardView.prototype.removeHexDevelopment = function(hex, development, hexEl, devEl) {
	var n = hexEl.firstChild;
	for(; n && n.getAttribute("class") != "development-container"; n = n.nextSibling) {}
	
	n.removeChild(devEl);
	development.svgEl_ = null;
}
BoardView.prototype.renderHexDevelopment = function(hex, hexDevelopment, svgEl) {
	var n = svgEl.firstChild;
	for(; n && n.getAttribute("class") != "development-container"; n = n.nextSibling) {}
	
	if(!n) {
		n = document.createElementNS(this.svgns_, "g");
        n.setAttribute("class", "development-container");
        /*if (svgEl.firstChild) svgEl.insertBefore(n, svgEl.firstChild);
        else */svgEl.appendChild(n);
	}
	
	var model = null;
	
	var pp = this.hexCoords(hex.x_, hex.y_);
	
    if (model = this.modelElements_[hexDevelopment.model]) {
        var newNode = model.svgElement.cloneNode(true);
        var newPos = {"x":(pp[0].x + pp[1].x) / 2, "y":(pp[0].y + pp[4].y) / 2};
        	    
        newNode.setAttribute("x", newPos.x - model.centerPosition.x);
        newNode.setAttribute("y", newPos.y - model.centerPosition.y);
        newNode.setAttribute("class", "hex-development " + (hexDevelopment.player ? hexDevelopment.player : ""));
        n.appendChild(newNode);
        hexDevelopment.svgEl_ = newNode;
    }
}
BoardView.prototype.renderVertexHitArea = function (vertex, svgEl) {
    var pp = this.c(vertex.x_, vertex.y_);

    var c = svgEl.ownerDocument.createElementNS(this.svgns_, "circle");
    c.setAttribute("cx", pp.x);
    c.setAttribute("cy", pp.y);
    c.setAttribute("r", this.edgeLength_ * 0.2);
    c.setAttribute("class", "vertex");

    var self = this;
    c.onclick = function (evt) { Event.fire(self, "vertexclick", [vertex, evt]); };
    c.onmouseover = function (evt) { Event.fire(self, "vertexover", [vertex, evt]); };
    c.onmouseout = function (evt) { Event.fire(self, "vertexout", [vertex, evt]); };

    svgEl.appendChild(c);
}

BoardView.prototype.renderHexes = function (svgEl) {
    for (var i = 0; i < this.board_.poly_.length; i++) {
        this.renderHex(this.board_.poly_[i], svgEl);
    }
}
BoardView.prototype.renderEdges = function (svgEl) {
    for (var i = 0; i < this.board_.edge_.length; i++) {
        this.renderEdge(this.board_.edge_[i], svgEl);
    }
}
BoardView.prototype.calculateEdgePoly = function (edgeWidth, p1, p2) {
    var l = edgeWidth;
    var xm = (p2.x - p1.x) / (p2.y - p1.y);
    var ym = 1.0 / xm;
    var dx = l / Math.sqrt(xm * xm + 1);
    var dy = l / Math.sqrt(ym * ym + 1);
    if (p2.x > p1.x) { dx = -dx; }
    if (p2.y > p1.y) { dy = -dy; }

    return [
        { x: p1.x - dx, y: p1.y + dy },
        { x: p2.x - dx, y: p2.y + dy },
        { x: p2.x + dx, y: p2.y - dy },
        { x: p1.x + dx, y: p1.y - dy }
    ];
}
BoardView.prototype.renderEdge = function (edge, svgEl) {
    var g = edge.svgEl_;
    if (!g) {
        g = document.createElementNS(this.svgns_, "g");
        svgEl.appendChild(g);
        edge.svgEl_ = g;
    }

    for (var i = 0; i < edge.development_.length; i++) {
        this.renderEdgeDevelopment(edge, edge.development_[i], g);
    }

    this.renderEdgeHitArea(edge, g);
}
BoardView.prototype.renderEdgeDevelopment = function (edge, edgeDevelopment, svgEl) {
    //TODO: how to represent models of roads? for now we just draw a poly...
    var n = svgEl.firstChild
    for (; n && n.getAttribute("class") != "development-container"; n = n.nextSibling) { }

    if (!n) {
        n = document.createElementNS(this.svgns_, "g");
        n.setAttribute("class", "development-container");
        if (svgEl.firstChild) svgEl.insertBefore(n, svgEl.firstChild);
        else svgEl.appendChild(n);
    }

    var p1 = this.c(edge.x1_, edge.y1_);
    var p2 = this.c(edge.x2_, edge.y2_);

    var pps = this.calculateEdgePoly(this.edgeLength_ * 0.075, p1, p2);

    var p = svgEl.ownerDocument.createElementNS(this.svgns_, "polygon");
    var points = "";
    for (var i = 0; i < pps.length; i++) {
        points += pps[i].x + "," + pps[i].y + " ";
    }

    p.setAttribute("points", points);
    p.setAttribute("class", "edge-development " + edgeDevelopment.player);
    n.appendChild(p);
    edgeDevelopment.svgEl_ = p;
}
BoardView.prototype.renderEdgeHitArea = function (edge, svgEl) {
    var p1 = this.c(edge.x1_, edge.y1_);
    var p2 = this.c(edge.x2_, edge.y2_);

    var pps = this.calculateEdgePoly(this.edgeLength_ * 0.1, p1, p2);

    var p = svgEl.ownerDocument.createElementNS(this.svgns_, "polygon");
    var points = "";
    for (var i = 0; i < pps.length; i++) {
        points += pps[i].x + "," + pps[i].y + " ";
    }

    p.setAttribute("points", points)
    p.setAttribute("class", "edge");
    svgEl.appendChild(p);

    var self = this;
    p.onclick = function (evt) { Event.fire(self, "edgeclick", [edge, evt]); };
    p.onmouseover = function (evt) { Event.fire(self, "edgeover", [edge, evt]); };
    p.onmouseout = function (evt) { Event.fire(self, "edgeout", [edge, evt]); };
}
BoardView.prototype.renderHex = function (hex, svgEl) {
    var g = hex.svgEl_;
    if (!g) {
        g = document.createElementNS(this.svgns_, "g");
        svgEl.appendChild(g);
        hex.svgEl_ = g;
    }

    var pp = this.hexCoords(hex.x_, hex.y_);
    var points = "";
    var pointsi = "";
    var cx = (pp[0].x + pp[1].x) / 2;
    var cy = (pp[0].y + pp[4].y) / 2;
    var alpha = 0.9;
    
    for (var i = 0; i < pp.length; i++) {
        points += pp[i].x + " " + pp[i].y + " ";
        pointsi += ((pp[i].x - cx)*alpha + cx) + " " + ((pp[i].y - cy)*alpha + cy) + " ";
    }
    var p = svgEl.ownerDocument.createElementNS(this.svgns_, "polygon");
    var pi = svgEl.ownerDocument.createElementNS(this.svgns_, "polygon");
    var hit = svgEl.ownerDocument.createElementNS(this.svgns_, "polygon");

    p.setAttribute("points", points);
    hit.setAttribute("points", pointsi);
    pi.setAttribute("points", pointsi);
    //HACK: replace with better way to style
    p.setAttribute("class", "hex");
    hit.setAttribute("class", "hex-hitarea");
    pi.setAttribute("class", "hex-inner " + hex.type_);
    g.appendChild(p);
    g.appendChild(pi);

    var self = this;
    hit.onclick = function (evt) { Event.fire(self, "hexclick", [hex, evt]); };
    hit.onmouseover = function (evt) { Event.fire(self, "hexover", [hex, evt]); };
    hit.onmouseout = function (evt) { Event.fire(self, "hexout", [hex, evt]); };

    if(hex.value_ > 0) {
	    
	    var circ = svgEl.ownerDocument.createElementNS(this.svgns_, "circle");
	    circ.setAttribute("cx", cx);
	    circ.setAttribute("cy", cy);
	    circ.setAttribute("r", Math.abs((pp[0].x - pp[1].x) / 3.75));
	    circ.setAttribute("class", "hexlabel-back");
	    g.appendChild(circ);
	
	    var txt = svgEl.ownerDocument.createElementNS(this.svgns_, "text");
	    txt.appendChild(svgEl.ownerDocument.createTextNode(hex.value_));
	    var className = "hexlabel";
	    if(hex.value_ == 6 || hex.value_ == 8) className += " labelemph";
	    
	    txt.setAttribute("class", className);
	    txt.setAttribute("x", (pp[0].x + pp[1].x) / 2 );
	    txt.setAttribute("y", (pp[0].y + pp[4].y) / 2 );
	
	    g.appendChild(txt);
    }
    
    cont = svgEl.ownerDocument.createElementNS(this.svgns_, "g");
    cont.setAttribute("class", "development-container");
    g.appendChild(cont);
    
    for (var i = 0; i < hex.development_.length; i++) {
        this.renderHexDevelopment(hex, hex.development_[i], g);
    }
    
    g.appendChild(hit);
}

BoardView.prototype.highlightBuildableVertex = function(player) {
	//alert("blah");
	for(var i = 0; i < this.board_.vetex_.length; i++) {
		v = this.board_.vetex_[i];
		var buildable = true;
		if(v.development_.length == 0) {
			// more than one vertex away from another city/settlement
			for(var j = 0; buildable && j < v.adjecentVertex_.length; j++) {
				va = v.adjecentVertex_[j];
				if(va.development_.length > 0)
					buildable = false;
			}
			// connected by a road

			if(buildable) {
				var incomingroad = false;
				for(var j = 0; !incomingroad && j < v.edge_.length; j++) {
					ea = v.edge_[j];
					
					for(var k = 0; k < !incomingroad && ea.development_.length; k++) {
						ed = ea.development_[k];
						
						//console.log("edge development: " + ed.type + ", " + ed.player);
						if(ed.model == "road" && ed.player == player)
							incomingroad = true;
					}
				}
				buildable = incomingroad;
			}
		}
		else
			buildable = false;

		if(buildable) {
			//console.log("buildabl!" + v.svgEl_.firstChild.getAttribute("class"));
			var el = null;
			for(el = v.svgEl_.firstChild; el && !/\bvertex\b/.test(el.getAttribute("class")); el = el.nextSibling) {};

			if(el) {
				this._highlight(el);
				//console.log("found one!");
			}
		}
	}
}
BoardView.prototype.highlightBuildableEdge = function(player) {
	for(var i = 0; i < this.board_.edge_.length; i++) {
		var e = this.board_.edge_[i];
		var buildable = false;
		if(e.development_.length == 0) {
			// is this road adjecent to any of the users cities/settlements?
			var adjecentVertexDev = false;
			for(var j = 0; !adjecentVertexDev && j < e.vertex_.length; j++) {
				va = e.vertex_[j];
				for(var k = 0; k < !adjecentVertexDev && va.development_.length; k++) {
					vd = va.development_[k];
					if(vd.player == player) 
						adjecentVertexDev = true;
				}
			}

			// or else look through the adjecent edges for roads...
			if(!adjecentVertexDev) {
				var adjecentRoad = false;
				for(var j = 0; !adjecentRoad && j < e.adjecentEdge_.length; j++) {
					ea = e.adjecentEdge_[j];
					for(var k = 0; !adjecentRoad && k < ea.development_.length; k++) {
						ed = ea.development_[k];
						if(ed.player == player)
							adjecentRoad = true;
					}
				}
			}
			
			buildable = (adjecentVertexDev || adjecentRoad);
		}

		if(buildable) {
			var el = null;
			for(el = e.svgEl_.firstChild; el && !/\bedge\b/.test(el.getAttribute("class")); el = el.nextSibling) {};

			if(el) {
				this._highlight(el);
				//console.log("found a road!");
			}
		}
		
	}
}
BoardView.prototype.clearHighlights = function() {
	for(var i = 0; i < this.board_.vetex_.length; i++) {
		for(var el = this.board_.vetex_[i].svgEl_.firstChild; el; el = el.nextSibling) {
			this._clearHighlight(el);
		}
	}
	for(var i = 0; i < this.board_.edge_.length; i++) {
		for(var el = this.board_.edge_[i].svgEl_.firstChild; el; el = el.nextSibling) {
			this._clearHighlight(el);
		}
	}
}



BoardView.prototype._highlight = function(svgEl) {
	svgEl.setAttribute("class", svgEl.getAttribute("class") + " highlighted");
//	svgEl.style.fill = "rgba(100,255,100,0.3)";
}
BoardView.prototype._clearHighlight = function(svgEl) {
	var cname = svgEl.getAttribute("class");
	cname = cname.replace("highlighted", "");
	svgEl.setAttribute("class", cname);
//	svgEl.style.fill = "inherit";
}


function enableBoardPanZoom(board, boardContainer) {
	var trans = new Transform2d();	
	
	var mousedown_ = false;
	var pageX_ = 0;
	var pageY_ = 0;
	
	var blockerDiv = null;
	
	var mousemove = function(e) {
		if(mousedown_) {
			move(e.pageX, e.pageY);	
			
			e.preventDefault();
			e.stopPropagation();	
		}	
	}
	var mouseout = function(e) {
		if(mousedown_) {
			move(e.pageX, e.pageY);
			stopmove();
			
			mousedown_ = false;
		}	
	}
	var mousedown = function(e) {
		if(e.which == 2) {
			mousedown_ = true;
			startmove(e.pageX, e.pageY);
			
			e.preventDefault();
			e.stopPropagation();
		}	
	}
	
	var stopmove = function() {
		if(blockerDiv) {
			blockerDiv.remove();
			blockerDiv = null;
		}
	}
	var startmove = function(pageX, pageY) {		
		//$(".chat-window").append("<p>" + trans.toString() + "</p>");
		pageX_ = pageX;
		pageY_ = pageY;

		blockerDiv = $("<div style='position:absolute;top:0px;left:0px;height:100%;width:100%;'/>");
		boardContainer.append(blockerDiv);
		
		blockerDiv.mouseup(mouseout);
		blockerDiv.mouseout(mouseout);
		blockerDiv.mousemove(mousemove);
		blockerDiv.mousedown(mousedown);
	}
	var move = function(pageX, pageY) {
		//$(".chat-window").append("<p>px,py " + pageX + "," + pageY + "</p>");
		//$(".chat-window").append("<p>epx,epy " + e.pageX + "," + e.pageY + "</p>");
		
		var x = pageX - pageX_;
		var y = pageY - pageY_;
		

		//var vec = trans.transformFrom([x,y]);
		
		trans.translate(x,y);
		
		board.setAttribute("transform", trans.toSVG());
					
		
		pageX_ = pageX;
		pageY_ = pageY;
	}
	
	var zoom = function(scale, pageX, pageY) {
		var pos = $(board.parentNode.parentNode).position();
		
		var x = pageX - pos.left;
		var y = pageY - pos.top;
					
		
		trans.translate(-x, -y).scale(scale).translate(x,y);
		
		/*
		
		$(".chat-window").append("<p>pos " + pos.left + "," + pos.top + "</p>");
		$(".chat-window").append("<p>e " + e.pageX + "," + e.pageY + "</p>");
		$(".chat-window").append("<p>x,y " + x + "," + y + "</p>");
		$(".chat-window").append("<p>dx,dy " + dx + "," + dy + "</p>");
		$(".chat-window").append("<p>x0,y0 " + vec0[0] + "," + vec0[1] + "</p>");
		
		var vec0 = trans.transformFrom([0,0]);
		var vec1 = trans.transformFrom([x,y]);
		
		var dx = vec1[0] - vec0[0];
		var dy = vec1[1] - vec0[1];
		
		var vec2 = trans.transformFrom([dx, dy]);
		
		trans.translate(vec2[0],vec2[1]);
		*/
		
		board.setAttribute("transform", trans.toSVG());
	}

	var mousewheel = function(e) {
		var delta = 0;
		if(e.detail) {
			delta = -e.detail/3;
		}
		
		if(delta != 0) {
			var scale = Math.pow(1.1, delta);
			
			zoom(scale, e.pageX, e.pageY);
		}
		
		if (e.preventDefault)
	        e.preventDefault();
		e.returnValue = false;
	}
	
	var touchstart = function(e) {
		e.preventDefault();
		if(e.touches.length == 1) {
			startmove(e.touches[0].pageX, e.touches[0].pageY);
		}
	}
	var touchmove = function(e) {
		e.preventDefault();
		if(e.touches.length == 1) {
			move(e.touches[0].pageX, e.touches[0].pageY);
		}
		
	}
	var touchend = function(e) {
		e.preventDefault();
		if(e.touches.length == 1) {
			move(e.touches[0].pageX, e.touches[0].pageY);
			stopmove();
		}
		
	}
	var touchcancel = function(e) {
		
	}
	
	var moztouchstart = function(e) {
		e.preventDefault();
		startmove(e.pageX, e.pageY);
	}
	var moztouchmove = function(e) {
		e.preventDefault();
		move(e.pageX, e.pageY);
	}
	var moztouchend = function(e) {
		e.preventDefault();
		move(e.pageX, e.pageY);
		stopmove();
	}
	
    if(window.addEventListener) 
    	window.addEventListener('DOMMouseScroll', mousewheel, false);
    
    if(document.addEventListener) {
    	document.addEventListener('touchstart', touchstart, false);
    	document.addEventListener('touchmove', touchmove, false);
    	document.addEventListener('touchend', touchend, false);
    	document.addEventListener('touchcancel', touchcancel, false);
    	
    	document.addEventListener('MozTouchDown', moztouchstart, false);
    	document.addEventListener('MozTouchMove', moztouchmove, false);
    	document.addEventListener('MozTouchUp', moztouchend, false);
    }
	    
    window.onmousewheel = mousewheel;
	
    
    boardContainer.mousedown(mousedown);
}

