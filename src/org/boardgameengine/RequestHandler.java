package org.boardgameengine;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestHandler {
	public void handle(HttpServletRequest req, HttpServletResponse resp, Matcher matches) throws IOException;
	public boolean getAuthenticationRequired();
	public boolean getSignInIfNotAuthenticated();
}
