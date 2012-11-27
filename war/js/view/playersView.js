

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

	Event.addListener(board, "resourcesUpdated", function(players) {
		self.render();
	});
	Event.addListener(board, "tradeResourcesUpdated", function(trade) {
		console.log("tradeResourcesUpdated event handler.");
		self.render();
	})
	Event.addListener(board, "currentPlayerChange", function(currentPlayerIndex) {
		self.setCurrentPlayer(currentPlayerIndex);
	});
};
PlayersView.prototype.setCurrentPlayer = function(currentPlayerIndex) {
	for(var i = 0; i < this.board_.player_.length; i++) {
		var p = this.board_.player_[i];
		if(i == currentPlayerIndex) { 
			p.__el.addClass("current-player");
		}
		else {
			p.__el.removeClass("current-player");
		}
	}
};
PlayersView.prototype.handleResize = function(width, height) {
	console.log("handlig resize: " + width + ", " + height);
};
PlayersView.prototype.render = function() {
	var self = this;
	this.el_.empty();
	
	var tradePlayerMap = new Object();
	var bankTradePlayer = null;
	for(var i = 0; i < this.board_.tradePlayer_.length; i++) {
		var tp = this.board_.tradePlayer_[i];
		if(tp.isBank_) bankTradePlayer = tp;
		else {
			tradePlayerMap[tp.color_] = tp;
		}
	}
	
	var ul = $("<ul/>");
	for(var i = 0; i < this.board_.player_.length; i++) {
		var p = this.board_.player_[i];
		
		var playerDetails = this.board_.gameDetails_.playerMap[p.color_];
		console.log("player color: " + p.color_);
		console.log("player map: " + this.board_.gameDetails_.playerMap);
		
		var li = $("<li class='player-" + i + "' />");
		
		var imgspan = $("<span class='player-image' />");
		
		var img = $("<img width='75' height='75' alt='Player " + p.color_ + " image' />");
		img.attr("src", "http://www.gravatar.com/avatar/" + playerDetails.gameUser.hashedEmail + "?s=75");
		imgspan.append(img);
		li.append(imgspan);
		
		var span = $("<span class='player-name player-" + p.color_ + "'/>");
		span.append(playerDetails.gameUser.nickname);
		span.click(function(p) {
			return function() { Event.fire(self, "playerclick", [p]); };
		}(p));
		li.append(span);
		
		
		
		var resel = $("<div class='resources' />");
		
		var rv = new ResourcesView(resel);
		rv.setPlayer(p);
		Event.fire(p, "load", []);

		Event.addListener(rv, "resourceclick", function(player) { 
			return function(resource) {
				Event.fire(self, "resourceclick", [player, resource]);
			};
		}(p));
		
		li.append(resel);
		
		if(typeof tradePlayerMap[p.color_] != "undefined") {
			var tp = tradePlayerMap[p.color_];
			
			var tradeel = $("<div class='trade-resources' />");
			var tv = new ResourcesView(tradeel);
			tv.setTradePlayer(tp);
			Event.fire(tp, "load", []);

			Event.addListener(tv, "resourceclick", function(player) { 
				return function(resource) {
					Event.fire(self, "traderesourceclick", [player, resource]);
				};
			}(p));
			
			li.append(tradeel);
		}
		
		
		ul.append(li);
		p.__el = li;
	}
	this.el_.append(ul);
	
	this.setCurrentPlayer(this.board_.currentPlayer_);
};