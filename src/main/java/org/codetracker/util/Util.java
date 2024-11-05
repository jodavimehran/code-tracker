package org.codetracker.util;

import gr.uom.java.xmi.UMLAnnotation;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Util {
	private static boolean enableCache;
	private static Map<String, String> shaCache = new HashMap<>();
    private Util() {
    }

    public static String annotationsToString(List<UMLAnnotation> umlAnnotations) {
        return umlAnnotations != null && !umlAnnotations.isEmpty()
                ? String.format("[%s]", umlAnnotations.stream().map(UMLAnnotation::toString).sorted().collect(Collectors.joining(";")))
                : "";
    }

    public static void enableSHACache() {
    	enableCache = true;
    }

    public static void disableSHACache() {
    	enableCache = false;
    }

    public static void clearSHACache() {
    	shaCache.clear();
    }

    public static String getSHA512(String input) {
    	if(shaCache.containsKey(input))
    		return shaCache.get(input);
    	String toReturn = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(input.getBytes(StandardCharsets.UTF_8));
            toReturn = String.format("%0128x", new BigInteger(1, digest.digest()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(enableCache)
        	shaCache.put(input, toReturn);
        return toReturn;
    }
}
