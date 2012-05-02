

function PlayersView(el) {
	this.el_ = el;
	this.board_ = null;
	this.boardLoadListenerId_ = null;
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
		li.append(p.color_);
		ul.append(li);
		
		li.click(function(p) {
			return function() { Event.fire(self, "playerclick", [p]); }
		}(p));
	}
	this.el_.append(ul);
};