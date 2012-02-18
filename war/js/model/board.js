

function Board(token, services) {
	this.modelElements_ = new Object();
	this.players_ = new Array();
	this.userToken_ = token;
	
	this.datamodelUrl_ = services["datamodel-xml-url"];
	this.eventUrl_ = services["event-xml-url"];
	
	this.poly_ = new Array();
	this.edge_ = new Array();
	this.vertex_ = new Array();
	this.dice_ = new Array();
	
	this.player_ = new Array();
	
}

Board.prototype.loadXML = function(xml) {
	
}