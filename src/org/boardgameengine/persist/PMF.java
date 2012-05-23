package org.boardgameengine.persist;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

public final class PMF {
    private static final PersistenceManagerFactory pmfInstance =
        JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private PMF() {}

    public static PersistenceManagerFactory getInstance() {
        return pmfInstance;
    }
    
    public static Object executeCommandInTransaction(PersistenceCommand command) throws PersistenceCommandException {
    	PersistenceManager pm = getInstance().getPersistenceManager();
    	Transaction tx = pm.currentTransaction();
    	
    	Object ret = null;
    	
    	try {
    		tx.begin();
    		
    		ret = command.exec(pm);
    		
    		tx.commit();
    	}
    	catch(Exception e) {
    		throw new PersistenceCommandException(e);
    	}
    	finally {
    		if(tx.isActive()) {
    			tx.rollback();
    		}
    		pm.close();
    	}
    	
    	return ret;
    }
}