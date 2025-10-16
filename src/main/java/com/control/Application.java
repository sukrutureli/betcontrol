package com.control;

import java.util.Map;

public class Application {
	public static void main(String[] args) {
		MatchScraper scraper = null;

		try {
			scraper = new MatchScraper();
			
			Map<String, String> updatedScores = scraper.fetchFinishedScores();
			
			System.out.println("----- Güncellenen Skorlar (" + updatedScores.size() + ") -----");
			for (Map.Entry<String, String> entry : updatedScores.entrySet()) {
			    System.out.println(entry.getKey() + " → " + entry.getValue());
			}
			System.out.println("--------------------------------------------");
			
			PredictionUpdater.updateFromGithub(updatedScores, "");
			
			//Basketbol
			scraper.getDriver().manage().deleteAllCookies();
			scraper.getDriver().navigate().to("about:blank");
			Thread.sleep(500);
			
			Map<String, String> updatedScoresBasketbol = scraper.fetchFinishedScoresBasket();
			
			System.out.println("----- Güncellenen Skorlar (" + updatedScoresBasketbol.size() + ") -----");
			for (Map.Entry<String, String> entry : updatedScoresBasketbol.entrySet()) {
			    System.out.println(entry.getKey() + " → " + entry.getValue());
			}
			System.out.println("--------------------------------------------");
			
			PredictionUpdater.updateFromGithub(updatedScoresBasketbol, "basketbol-");

		} catch (Exception e) {
			System.out.println("GENEL HATA: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (scraper != null) {
				scraper.close();
			}
		}
	}
}
