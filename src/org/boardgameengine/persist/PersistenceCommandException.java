package org.boardgameengine.persist;

public class PersistenceCommandException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public PersistenceCommandException(Throwable e) {
		super(e);
	}

}
