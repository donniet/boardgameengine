

function PlayersView(el) {
	this.el_ = el;
	this.board_ = null;
	this.boardLoadListenerId_ = null;
	this.playerResourcesViews_ = new Array();
}
PlayersView.prototype.setBoard = function(board) {
	if(this.board_) {
		Event.removeListenerById(this.board_, "load", this.boardLoadListenerId_);
		this.board_ = null;
		this.boardLoadListenerId_ = null;		
	}
	this.board_ = board;
	var self = this;
	this.boardLoadListenerId_ = Event.addListener(this.board_, "load", function() {
		self.render();
	});
};
PlayersView.prototype.render = function() {
	var self = this;
	
	var ul = $("<ul/>");
	for(var i = 0; i < this.board_.player_.length; i++) {
		var p = this.board_.player_[i];
		
		var li = $("<li/>");
		var span = $("<span class='player-color player-" + p.color_ + "'/>");
		span.append(p.color_);
		span.click(function(p) {
			return function() { Event.fire(self, "playerclick", [p]); };
		}(p));
		li.append(span);
		
		var img = $("<img width='50' height='50' alt='Player " + p.color_ + " image' />");
		img.attr("src", "http://www.gravatar.com/avatar/" + p.hashedEmail_ + "?s=50");
		li.append(img);
		
		ul.append(li);
		
		var resel = $("<div class='resources' />");
		
		var rv = new ResourcesView(resel);
		rv.setPlayer(p);
		Event.fire(p, "load", []);
		
		li.append(resel);
		
		Event.addListener(rv, "resourceclick", function(player) { 
			return function(resource) {
				Event.fire(self, "resourceclick", [player, resource]);
			};
		}(p));
	}
	this.el_.append(ul);
};