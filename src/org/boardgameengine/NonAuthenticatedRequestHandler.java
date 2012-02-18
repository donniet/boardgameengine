package org.boardgameengine;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class NonAuthenticatedRequestHandler implements RequestHandler {
	public boolean getAuthenticationRequired() {
		return false;
	}
}
