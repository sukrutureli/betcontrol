package com.control;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchScraper {

	private WebDriver driver;
	private JavascriptExecutor js;
	private WebDriverWait wait;

	public MatchScraper() {
		setupDriver();
	}

	private void setupDriver() {
		System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
				"--window-size=1920,1080", "--disable-blink-features=AutomationControlled");
		driver = new ChromeDriver(options);
		js = (JavascriptExecutor) driver;
		wait = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	// =============================================================
	// CANLI SKOR (BİTMİŞ MAÇLAR) ÇEK
	// =============================================================
	public Map<String, String> fetchFinishedScores(String type) {
		Map<String, String> scores = new HashMap<>();
		try {
			String url = "https://www.nesine.com/iddaa/canli-skor/" + type;
			driver.get(url);
			clickYesterdayTabIfNeeded(driver);
			wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.match-not-play")));

			Thread.sleep(2000); // Dinamik skor tablosunun yüklenmesi için kısa bekleme

			List<WebElement> allMatches = driver.findElements(By.cssSelector("li.match-not-play"));
			List<WebElement> finishedMatches = new ArrayList<>();

			for (WebElement match : allMatches) {
				try {
					WebElement statusEl = match.findElement(By.cssSelector(".statusLive.status"));
					String status = statusEl.getAttribute("class");
					if (status.contains("finished")) {
						finishedMatches.add(match);
					}
				} catch (Exception ignore) {
				}
			}

			System.out.println("Bitmiş maç sayısı: " + finishedMatches.size());

			for (WebElement match : finishedMatches) {
				try {
					String home = safeText(match.findElement(By.cssSelector(".home-team span")));
					String away = safeText(match.findElement(By.cssSelector(".away-team span")));

					// Skor
					WebElement scoreBoard = match.findElement(By.cssSelector(".board"));
					String homeScore = safeText(scoreBoard.findElement(By.cssSelector(".home-score")));
					String awayScore = safeText(scoreBoard.findElement(By.cssSelector(".away-score")));
					String score = homeScore + "-" + awayScore;

					String key = home + " - " + away;
					scores.put(key, score);
					System.out.println("✅ " + key + " → " + score);

				} catch (Exception e) {
					System.out.println("⚠️ Tekil maç çekilemedi: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("fetchFinishedScores hata: " + e.getMessage());
		}
		return scores;
	}

	/**
	 * Gece 00:00'dan sonra çalıştırıldığında "dün" sekmesine tıklar.
	 * Diğer zamanlarda hiçbir şey yapmaz.
	 */
	private void clickYesterdayTabIfNeeded(WebDriver driver) {
	    try {
	        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

	        // Menü tamamen yüklensin
	        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".live-result-menu")));
	        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(.,'Bugün')]")));

	        WebElement todayTab = driver.findElement(By.xpath("//span[contains(.,'Bugün')]"));

	        // İstanbul saatine göre 00:00-06:00 arası "dün" sekmesini seç
	        LocalTime now = LocalTime.now(ZoneId.of("Europe/Istanbul"));
	        if (now.isAfter(LocalTime.MIDNIGHT) && now.isBefore(LocalTime.of(6, 0))) {
	            WebElement previousTab = todayTab.findElement(
	                    By.xpath("preceding-sibling::span[contains(@class,'tab')][1]")
	            );

	            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", previousTab);
	            new WebDriverWait(driver, Duration.ofSeconds(5))
	                    .until(ExpectedConditions.elementToBeClickable(previousTab));

	            previousTab.click();
	            Thread.sleep(2000); // Sayfa verilerini güncellemesi için bekle

	            System.out.println("⏪ Dün sekmesine geçildi.");
	        } else {
	            System.out.println("📅 Bugün sekmesi aktif, değişiklik yapılmadı.");
	        }

	    } catch (Exception e) {
	        System.out.println("⚠️ Dün sekmesi seçilemedi: " + e.getMessage());
	    }
	}
	
	public void close() {
		try {
			driver.quit();
		} catch (Exception ignore) {
		}
	}

	private String safeText(WebElement el) {
		try {
			return el.getText().trim();
		} catch (Exception e) {
			return "-";
		}
	}
}
