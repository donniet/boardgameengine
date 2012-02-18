package org.boardgameengine;

public abstract class AuthenticationRequiredRequestHandler implements RequestHandler {
	public boolean getAuthenticationRequired() {
		return true;
	}
}
