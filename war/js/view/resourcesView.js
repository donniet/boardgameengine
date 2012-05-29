
function ResourcesView(element) {
	this.el_ = element;
	this.resources_ = null;
	this.player_ = null;
	this.board_ = null;
	this.selectMode_ = ResourcesView.SelectMode.One;
	this.selectedResources_ = new Array();
}
ResourcesView.SelectMode = {
	"One": 1,
	"Many": 2
}
ResourcesView.prototype.setPlayer = function(player) {
	this.player_ = player;
	
	var self = this;
	Event.addListener(this.player_, "load", function(resources) { self.renderPlayer(); });
}
ResourcesView.prototype.setTradePlayer = function(tradePlayer) {
	this.player_ = tradePlayer;
	var self = this;
	
	Event.addListener(this.player_, "load", function(resources) { self.renderPlayer(); });	
}
ResourcesView.prototype.setResourceArray = function(resourceArray) {
	this.resources_ = resourceArray;
	this.render();
}
ResourcesView.prototype.renderPlayer = function() {	
	this.resources_ = this.player_.getResources();
	this.selectedResources_ = new Array();
	this.render();
}
ResourcesView.prototype.clearSelectedResources = function() {
	for(var i = 0; i < this.selectedResources_.length; i++) {
		var s = this.selectedResources_[i];
		s.span.removeClass("resource-view-resource-selected");
		s.span.__selected = false;
		//delete this.selectedResources_[i];
	}
	this.selectedResources_ = new Array();
	Event.fire(this, "clearselected", []);
}
ResourcesView.prototype.getSelectedResources = function() {
	var obj = new Object();
	var ret = new Array();
	for(var i = 0; i < this.selectedResources_.length; i++) {
		var s = this.selectedResources_[i];
		if(obj[s.resource]) {
			obj[s.resource].amount++;
		}
		else {
			var r = {"resource":s.type_, "amount":1};
			ret.push(r);
			obj[s.type_] = r;
		}
	}
	return ret;
}
ResourcesView.prototype.setSelectMode = function(mode) {
	this.selectMode_ = mode;
	this.clearSelectedResources();
	Event.fire(this, "setselectmode", [mode]);
}
ResourcesView.prototype.handleResourceClick = function(resource, li) {
	console.log("resourceview, resource click: " + resource);
	switch(this.selectMode_) {
	case ResourcesView.SelectMode.One:
		console.log("resourcesview, selectmode: one");
		Event.fire(this, "resourceclick", [resource]);
		break;
	case ResourcesView.SelectMode.Many:
		if(!li.__selected) {
			var obj = {"resource":resource, "li":li};
			this.selectedResources_.push(obj);
			li.addClass("resource-view-resource-selected");
			li.__selected = obj;
		}
		else {
			var sr = new Array();
			
			var i = 0;
			for(; i < this.selectedResources_.length; i++) {
				var s = this.selectedResources_[i];
				if(s.li !== li) {
					sr.push(s);
				} 
				delete this.selectedResources_[i];
			}
			delete this.selectedResources_;
			this.selectedResources_ = sr;
			li.__selected = null;
			li.removeClass("resource-view-resource-selected");
		}
		break;
	default:
		break;
	}
}
ResourcesView.prototype.render = function(resources) {	
	this.el_.empty();
	var self = this;
	
	var ul = $("<ul/>");
	ul.addClass("resource-view");
	for(var i = 0; i < this.resources_.length; i++) {
		var r = this.resources_[i];
		for(var j = 0; j < r.count_; j++) {
			var li = $("<li/>");
			var resource = $("<span/>");
			li.click(function(resource, li) { 
				return function() { 
					self.handleResourceClick(resource, li);
				}	
			}(r.type_, li));
			resource.addClass("resource-view-" + r.type_);
			var textSpan = $("<span/>");
			textSpan.text(r.type_);
			resource.append(textSpan);
			li.append(resource);
			ul.append(li);
		}
	}
	this.el_.append(ul);
}