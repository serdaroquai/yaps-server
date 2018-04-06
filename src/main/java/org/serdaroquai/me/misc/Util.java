package org.serdaroquai.me.misc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.serdaroquai.me.Config.StratumConnection;


public class Util {
	
	private static final CharSequence zeroes = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	private static final String genesisBlockDiff = "00ffff0000000000000000000000000000000000000000000000000000";
	
	public static BigDecimal diffToInteger(String diffEncoded, Algorithm algo) {
		//TODO for now skip algos with multipliers
		    	
    	//note 2 chars in string is 1 byte
    	int numberOfBytes = Integer.valueOf(diffEncoded.substring(0,2), 16);
    	int prefixLength = diffEncoded.length() - 2;
    	int numberOfZeroes= (numberOfBytes * 2) - prefixLength;
    	
    	String diffDecoded = new StringBuilder(numberOfBytes*2).append(diffEncoded.substring(2)).append(zeroes,0,numberOfZeroes).toString();
    	
    	return new BigDecimal(new BigInteger(genesisBlockDiff, 16)).divide(new BigDecimal(new BigInteger(diffDecoded, 16)), 3, RoundingMode.HALF_UP);
	}
	
	public static String cascade(String algo, String tag) {
		return String.format("%s-%s", algo,tag);
	}
	
	public static String getAddressOf(StratumConnection conn) {
		return String.format("%s:%s", conn.getHost(), conn.getPort());
	}
	
	public static int getBlockHeight(String coinbase1) {
		/*
		 * coinbase1 = 020000004a24a05a010000000000000000000000000000000000000000000000000000000000000000ffffffff180304871d044c24a05a08
		 * after ffffff is 18 (script length)
		 * 03 block height byte length ( no need to parse this for the next 150 years for 2^23-1 blocks
		 * 04871d block height in little endian
		 * note that we will reverse 04871d00 (since java uses 4 bytes for Integer)
		 */
		String coinbase = coinbase1.split("ffffffff")[1]; // 180304871d044c24a05a08
		int byteCount = Integer.valueOf(coinbase.substring(2,4),16); //usually 03, for some low block numbers 02
		String heightLittleEndian = coinbase.substring(4, 4 + (byteCount*2)); // (00) 04 87 1d 
		return swapEndian(Integer.valueOf(heightLittleEndian,16), byteCount); // 00 1d 87 04
	}
	
	public static int swapEndian(int i, int byteCount) { 
		// converts (00) 04 87 1d first to 1d 87 04 00 then to 00 1d 87 04
		return ((i<<24) + ((i<<8)&0x00FF0000) + ((i>>8)&0x0000FF00) + (i>>>24)) >> (8 * (4 - byteCount));
	}
	
	public static boolean isEmpty(String string) {
		return StringUtils.isEmpty(string);
	}
	
	public static byte[] applyDSASig(PrivateKey privateKey, String input) {
		Signature dsa;
		byte[] output = new byte[0];
		try {
			dsa = Signature.getInstance("DSA", "BC");
			dsa.initSign(privateKey);
			byte[] strByte = input.getBytes();
			dsa.update(strByte);
			byte[] realSig = dsa.sign();
			output = realSig;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return output;
	}

	public static boolean verifyDSASig(PublicKey publicKey, String data, byte[] signature) {
		try {
			Signature dsaVerify = Signature.getInstance("DSA", "BC");
			dsaVerify.initVerify(publicKey);
			dsaVerify.update(data.getBytes());
			return dsaVerify.verify(signature);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "BC");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

			// Initialize the key generator and generate a KeyPair
			keyGen.initialize(1024, random);
			KeyPair keyPair = keyGen.generateKeyPair();

			return keyPair;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException {
	    byte[] clear = Base64.getDecoder().decode(key64);
	    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
	    KeyFactory fact = KeyFactory.getInstance("DSA");
	    PrivateKey priv = fact.generatePrivate(keySpec);
	    Arrays.fill(clear, (byte) 0);
	    return priv;
	}


	public static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
	    byte[] data = Base64.getDecoder().decode(stored);
	    X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
	    KeyFactory fact = KeyFactory.getInstance("DSA");
	    return fact.generatePublic(spec);
	}

	public static String savePrivateKey(PrivateKey priv) throws GeneralSecurityException {
	    KeyFactory fact = KeyFactory.getInstance("DSA");
	    PKCS8EncodedKeySpec spec = fact.getKeySpec(priv,
	            PKCS8EncodedKeySpec.class);
	    byte[] packed = spec.getEncoded();
	    String key64 = Base64.getEncoder().encodeToString(packed);

	    Arrays.fill(packed, (byte) 0);
	    return key64;
	}


	public static String savePublicKey(PublicKey publ) throws GeneralSecurityException {
	    KeyFactory fact = KeyFactory.getInstance("DSA");
	    X509EncodedKeySpec spec = fact.getKeySpec(publ,
	            X509EncodedKeySpec.class);
	    return Base64.getEncoder().encodeToString(spec.getEncoded());
	}
	
}
