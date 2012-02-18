package org.boardgameengine;

public abstract class GameServiceRequestHandler extends AuthenticationRequiredRequestHandler {
	public boolean getSignInIfNotAuthenticated() {
		return false;
	}
}
