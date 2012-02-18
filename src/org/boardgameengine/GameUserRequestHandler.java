package org.boardgameengine;

public abstract class GameUserRequestHandler extends AuthenticationRequiredRequestHandler {
	public boolean getSignInIfNotAuthenticated() {
		return true;
	}

}
