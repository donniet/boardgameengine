package org.boardgameengine.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;

import flexjson.transformer.DateTransformer;

public class Config {
	private static Config instance_ = null;
	
	private Properties props_ = null;
	private String dateFormatString_ = null;
	private DateFormat dateFormat_ = null;
	private DateTransformer dateTransformer_ = null;
	private Random random_ = null;
	private String encoding_ = null;
	private String digestAlgorithm_ = null;
	private String gameEngineNamespace_ = "";
	private String scxmlNamespace_ = "";
	
	public static Config getInstance() {
		if(instance_ == null) {
			instance_ = new Config();
		}
		return instance_;
	}
	
	private Config() {
		props_ = new Properties();
		 
		try {
			props_.load(Config.class.getResourceAsStream("/app.properties"));
			
			dateFormatString_ = props_.getProperty("dateFormat");
			dateFormat_ = new SimpleDateFormat(dateFormatString_);
			dateTransformer_ = new DateTransformer(dateFormatString_);
			
			encoding_ = props_.getProperty("encoding");
			digestAlgorithm_ = props_.getProperty("digestAlgorithm");
			
			gameEngineNamespace_ = props_.getProperty("gameenginenamespace");
			scxmlNamespace_ = props_.getProperty("scxmlnamespace");
			
			random_ = new SecureRandom();
			//dateFormat_.setCalendar(new GregorianCalendar());
		}
		catch(IOException e) {
			//TODO: do something better here...
			e.printStackTrace();
		}
	}
	
	public DateFormat getDateFormat() {
		return dateFormat_;
	}
	public String getDateFormatString() {
		return dateFormatString_;
	}
	public DateTransformer getDateTransfomer() {
		return dateTransformer_;
	}
	public Random getRandom() {
		return random_;
	}
	public String getEncoding() {
		return encoding_;
	}
	public String getDigestAlgorithm() {
		return digestAlgorithm_;
	}
	public String getGameEngineNamespace() { 
		return gameEngineNamespace_;
	}
	public String getSCXMLNamespace() { 
		return scxmlNamespace_;
	}
	public String getHash(String str, byte[] salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance(getDigestAlgorithm());
			digest.reset();
			digest.update(salt);
			byte[] out = digest.digest(str.getBytes(getEncoding()));
			
			return Base64.encodeBase64URLSafeString(out);
		}
		catch(UnsupportedEncodingException e) {
			//TODO: add logging
			return null;
		}
		catch(NoSuchAlgorithmException e) {
			//TODO: add logging
			return null;
		}
	}
	
}
