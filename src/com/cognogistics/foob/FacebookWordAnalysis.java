package com.cognogistics.foob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import facebook4j.Category;
import facebook4j.Comment;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.PagableList;
import facebook4j.Post;
import facebook4j.Reading;
import facebook4j.ResponseList;
import facebook4j.auth.AccessToken;

public class FacebookWordAnalysis {

	/**
	 * A Facebook client app that counts frequencies of words and phrases in a group's posts.
	 * 
	 * Get an access token from:
	 * 		https://developers.facebook.com/tools/explorer
	 * 
	 * 
	 * @param args
	 * @throws FacebookException 
	 */
	
	public static final String groupIdGreatLakesRM 		= "106675549490547";
	private static final String myTokenString			= "EAACEdEose0cBAEQcETZBfm7VvQfcDipkZCHczpGZCj0eVOjOM5gc72dgZAGo46UjbYsoPpnXl3BS7iV1xKuDW0JJ2KV5TLI6LvPxZBChNtpML0ZCyZBWzQ0aZCN1IgqVgjn91JGjYv6jxgoR1G38bKAjZAvo01FfcXjrCt7sNbkFWc5YJFObH1sZCjoBMin6fuYePcA0Q3g6ZAhrgZDZD";
	private static final int maxMessages				= 500;
	private static final int phraseWordsMax				= 6;
	private static final int topN 						= 50;
	
	private static ArrayList<TreeMap<String, Integer>> wordCounts 	= new ArrayList<TreeMap<String, Integer>>();
	
	public static void main(String[] args) {

		// Initialize word counting maps
		for (int xx=0; xx<phraseWordsMax; xx++) {
			TreeMap<String, Integer> initmap = new TreeMap<String, Integer>();
			wordCounts.add(initmap);
		}
		
		// Set up connection to FB
		Facebook facebook = new FacebookFactory().getInstance();			    // Generate FB instance.
	    facebook.setOAuthAppId("", "");											// Use default values for OAuth application id.

	    AccessToken token = new AccessToken(myTokenString);					    // Set access token
	    facebook.setOAuthAccessToken(token);

	    // Get the group feed
	    ResponseList<Post> feed = null;
	    boolean success = false;
	    try {
	    	feed = facebook.getFeed(groupIdGreatLakesRM,new Reading().limit(maxMessages));
	    	success = true;
	    } catch (FacebookException ex) {
	    	System.err.println("Error getting response... " + ex.getErrorMessage());
	    }

        // Process feed posts
	    if ( success ) {

	    	for (int i = 0; i < feed.size(); i++) {

		    	// Get post.
	            Post post = feed.get(i);

	            // Get parts of post we care about.
	            String postId 		= post.getId();
	            String postText 	= post.getMessage();

	            if ( postText != null ) {

	            	String postTextLine	= postText.replaceAll("[\\r\\n]+", "\\$");
		            Date postDate 		= post.getUpdatedTime();
		            String postDateText	= (postDate == null) ? "" : postDate.toString();
		            Category postFrom 	= post.getFrom();
		            String postUser		= (postFrom == null ) ? "" : postFrom.getName();

		            // Print date,text of post
		            //System.out.println(String.format("%s,%s,%s,%s",postId,postDateText,nonull(postUser),postTextLine));
		
		            // Add garbage to the hash mill...
		            Dissect(postTextLine);
	            }
	        }
	    }
	    
	    // Analyze the data
	    AnalyzeData();
	    
	}
	
	// Analyze Data
	private static void AnalyzeData() {

		TreeMap<String, Integer> phraseMap;
		NavigableSet<Map.Entry<String,Integer>> phrasesSorted;
		
		for (int phraseWordCt=0; phraseWordCt<phraseWordsMax; phraseWordCt++) {
			System.out.println(String.format("Top %d-word phrases...", phraseWordCt+1));
			phraseMap = wordCounts.get(phraseWordCt);
			phrasesSorted = entriesSortedByValues(phraseMap);
			int outCt = 0;
			for (Map.Entry<String,Integer> pair: phrasesSorted) {
				if ( !isMiscWord(pair.getKey())) {
					outCt += 1;
					if ( outCt >= topN ) break;
					int wordCt = Integer.valueOf(pair.getValue());
		            System.out.println(String.format("%05d %s", wordCt, pair.getKey()));
				}
	        }
		}
		
	}
	
	
	// Dissect text into phrases and count them up.
	private static void Dissect(String text) {

		String textline = text.replaceAll("[^a-zA-Z]"," ").replaceAll("  +", " ").toLowerCase();
		String words[] = textline.split(" ");
		
		for (int wordIx=0; wordIx<words.length; wordIx++ ) {
			String phraseText = "";
			for (int phraseWordIx=0; phraseWordIx<phraseWordsMax && wordIx+phraseWordIx<words.length; phraseWordIx++) {
				phraseText += ((phraseWordIx != 0) ? " " : "") + words[wordIx+phraseWordIx];
				phraseAdd(phraseWordIx,phraseText);
			}
		}
		
		System.out.println(textline);
		
	}
	
	private static void phraseAdd(int wordCt, String phraseText) {
		TreeMap<String, Integer> phraseMap = wordCounts.get(wordCt);
		if ( !phraseMap.containsKey(phraseText)) {
			phraseMap.put(phraseText,1);
		} else {
			phraseMap.put(phraseText, phraseMap.get(phraseText) + 1);
		}
	}
	
	private static <K,V extends Comparable<? super V>>
	NavigableSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
	    NavigableSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
	        new Comparator<Map.Entry<K,V>>() {
	            @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
	                int res = e2.getValue().compareTo(e1.getValue());
	                return res != 0 ? res : 
	                	String.valueOf(e2.getKey()).compareTo(String.valueOf(e1.getKey()));
	            }
	        }
	    );
	    sortedEntries.addAll(map.entrySet());
	    return sortedEntries;
	}
	
	private static String nonull(String text) {
		return text == null ? "" : text;
	}
	
	private static boolean isMiscWord(String text) {
		boolean matched = String.valueOf(".a.to.and.of.this.is.it.the.s.so.but.or.are.t.m.at.if.any.are.one.ve.been.was.be.an.has.in.on.for.with.").contains("."+text+".");
		return matched;
	}
	
	
	
	

}		