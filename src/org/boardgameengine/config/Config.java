package org.boardgameengine.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.boardgameengine.GameServlet;

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
	private String encryptionAlgorithm_ = null;
	private byte[] privateKey_ = new byte[] {0x0};
	private byte[] initializationVector_ = new byte[] {0x0};
	private String gameEngineNamespace_ = "";
	private String scxmlNamespace_ = "";
	private int maxScriptRuntime_ = 1;
	private String datamodeltransformresource_ = "";
	private String datamodeltransformplayeridparam_ = "";
	private int stateHistorySize_ = 100;
	
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
			encryptionAlgorithm_ = props_.getProperty("ecryptionAlgorithm");
			privateKey_ = props_.getProperty("privateKey").getBytes(encoding_);
			initializationVector_ = props_.getProperty("initializationVector").getBytes(encoding_);
			
			gameEngineNamespace_ = props_.getProperty("gameenginenamespace");
			scxmlNamespace_ = props_.getProperty("scxmlnamespace");
			
			maxScriptRuntime_ = Integer.parseInt(props_.getProperty("maxScriptRuntime"));
			
			datamodeltransformresource_ = props_.getProperty("datamodeltransformresource");
			datamodeltransformplayeridparam_ = props_.getProperty("datamodeltransformplayeridparam");
			
			stateHistorySize_ = Integer.parseInt(props_.getProperty("stateHistorySize"));
			
			random_ = new SecureRandom();
			//dateFormat_.setCalendar(new GregorianCalendar());
		}
		catch(IOException e) {
			//TODO: do something better here...
			e.printStackTrace();
		}
	}
	
	public InputStream getDataModelTransformStream() {
		return Config.class.getResourceAsStream(datamodeltransformresource_);
	}
	public String getDataModelTransformPlayerIdParam() {
		return datamodeltransformplayeridparam_;
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
	public byte[] getPrivateKey() {
		return privateKey_;
	}
	public String getEncryptionAlgorithm() {
		return encryptionAlgorithm_;
	}
	public byte[] getInitializationVector() {
		return initializationVector_;
	}
	public String getGameEngineNamespace() { 
		return gameEngineNamespace_;
	}
	public String getSCXMLNamespace() { 
		return scxmlNamespace_;
	}
	public int getStateHistorySize() {
		return stateHistorySize_;
	}
	private static String hex(byte[] array) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length; ++i) {
		sb.append(Integer.toHexString((array[i]
				& 0xFF) | 0x100).substring(1,3));       
		}
		return sb.toString();
	}
	public String getMD5Hash (String message) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return hex (md.digest(message.getBytes("CP1252")));
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	/*
	public String getMD5Hash(String str) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] out = digest.digest(str.getBytes(getEncoding()));
			BigInteger outInt = new BigInteger(out); 
			
			return outInt.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return null;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	*/
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

	public int getMaxScriptRuntime() {
		return maxScriptRuntime_;
	}
	
	public String encryptString(String plain) {
		SecretKeySpec key = new SecretKeySpec(getPrivateKey(), getEncryptionAlgorithm());
		String ret = null;
				
		try {
			byte[] input = plain.getBytes(getEncoding());
			
			Cipher cipher = Cipher.getInstance(getEncryptionAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] encrypted = new byte[cipher.getOutputSize(input.length)];
			int enc_len = cipher.update(input, 0, input.length, encrypted, 0);
			enc_len += cipher.doFinal(encrypted, enc_len);
			
			ret = Base64.encodeBase64String(encrypted);
			
		} 
		catch (NoSuchAlgorithmException e) {} 
		catch (NoSuchPaddingException e) {} 
		catch (InvalidKeyException e) {
			e.printStackTrace();
		} 
		catch (ShortBufferException e) {} 
		catch (UnsupportedEncodingException e) {} 
		catch (IllegalBlockSizeException e) {} 
		catch (BadPaddingException e) {}
		
		return ret;
		
	}
	public String decryptString(String crypt) {
		SecretKeySpec key = new SecretKeySpec(getPrivateKey(), getEncryptionAlgorithm());
		String ret = null;
		
		byte[] input = Base64.decodeBase64(crypt);
		
		try {
			Cipher cipher = Cipher.getInstance(getEncryptionAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, key);
			
			byte[] decrypted = new byte[cipher.getOutputSize(input.length)];
			int dec_len = cipher.update(input, 0, input.length, decrypted, 0);
			dec_len += cipher.doFinal(decrypted, dec_len);
			
			ret = new String(decrypted, getEncoding());			
		} 
		catch (NoSuchAlgorithmException e) {} 
		catch (NoSuchPaddingException e) {} 
		catch (InvalidKeyException e) {} 
		catch (ShortBufferException e) {} 
		catch (UnsupportedEncodingException e) {} 
		catch (IllegalBlockSizeException e) {} 
		catch (BadPaddingException e) {}
		
		return ret;
	}
}
