
function DiceView(diceElement) {
	this.diceElement_ = diceElement;
	this.board_ = null;
	this.diceRolledListenerId_ = null;
}
DiceView.prototype.setBoard = function(board) {
	if(this.board_) {
		Event.removeListenerById(this.board_, "diceRolled", this.diceRolledListenerId_);
		this.board_ = null;
		this.diceRolledListenerId_ = null;		
	}
	this.board_ = board;
	var self = this;
	this.diceRolledListenerId_ = Event.addListener(this.board_, "diceRolled", function() {
		self.render();
	});
}
DiceView.prototype.render = function() {
	console.log("rendering dice.");
	this.diceElement_.empty();
	
	var dice = this.board_.getDice();
	
	if(!dice || dice.length == 0) 
		dice = [6,6];
	
	var self = this;
	
	for(var i = 0; dice && i < dice.length; i++) {
		var d = dice[i];
		//console.log("current die: " + d);
		var dieEl = $('<img src="/i/Dice-' + d + '.svg" alt="' + d + '" width="50" height="50"/>');
		dieEl.click(function() {
			console.log("dice view click handler");
			Event.fire(self, "click", []);
		});
		//console.log("die img: " + dieEl)
		this.diceElement_.append(dieEl);
	}
}