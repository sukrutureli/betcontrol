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
	private WebDriver driver2;
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
		driver2 = new ChromeDriver(options);
		wait = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	// =============================================================
	// CANLI SKOR (BİTMİŞ MAÇLAR) ÇEK
	// =============================================================
	public Map<String, String> fetchFinishedScores() {
		Map<String, String> scores = new HashMap<>();
		try {
			String url = "https://www.nesine.com/iddaa/canli-skor/futbol";
			driver.get(url);
			waitForPageLoad(driver, 10);
			Thread.sleep(1000); // ekstra nefes payı
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
	
	public Map<String, String> fetchFinishedScoresBasket() {
	    Map<String, String> scores = new HashMap<>();
	    try {
	        String url = "https://www.nesine.com/iddaa/canli-skor/basketbol";
	        driver2.get(url);
	        waitForPageLoad(driver2, 10);
	        Thread.sleep(1000); // ekstra nefes payı
	        clickYesterdayTabIfNeeded(driver2);

	        WebDriverWait wait = new WebDriverWait(driver2, Duration.ofSeconds(15));
	        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".match-list.basketbol")));

	        Thread.sleep(2000); // Dinamik içerik yüklenmesi için kısa bekleme

	        // Tüm bitmiş maçları bul
	        List<WebElement> finishedMatches = driver2.findElements(
	                By.cssSelector("li.match-not-play.unliveData .statusLive.status.finished")
	        );

	        System.out.println("Bitmiş maç sayısı (basketbol): " + finishedMatches.size());

	        for (WebElement status : finishedMatches) {
	            try {
	                // Ana <li> elementine geri dön
	                WebElement match = status.findElement(By.xpath("./ancestor::li[contains(@class,'match-not-play')]"));

	                String home = safeText(match.findElement(By.cssSelector(".home-team span[aria-hidden='true']")));
	                String away = safeText(match.findElement(By.cssSelector(".away-team span[aria-hidden='true']")));

	                // Skor
	                WebElement board = match.findElement(By.cssSelector(".board"));
	                String homeScore = safeText(board.findElement(By.cssSelector(".home-score")));
	                String awayScore = safeText(board.findElement(By.cssSelector(".away-score")));
	                String score = homeScore + "-" + awayScore;

	                String key = home + " - " + away;
	                scores.put(key, score);
	                System.out.println("🏀 " + key + " → " + score);

	            } catch (Exception e) {
	                System.out.println("⚠️ Basketbol maçında hata: " + e.getMessage());
	            }
	        }

	    } catch (Exception e) {
	        System.out.println("fetchFinishedScoresBasket hata: " + e.getMessage());
	    }
	    return scores;
	}

	private void clickYesterdayTabIfNeeded(WebDriver driver) {
	    try {
	        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
	        JavascriptExecutor js = (JavascriptExecutor) driver;

	        // Menü yüklenene kadar bekle
	        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".live-result-menu")));
	        Thread.sleep(1000);

	        // "Bugün" sekmesini bul
	        WebElement todayTab = driver.findElement(By.xpath("//span[contains(.,'Bugün')]"));

	        // İstanbul saatine göre kontrol
	        LocalTime now = LocalTime.now(ZoneId.of("Europe/Istanbul"));
	        if (now.isAfter(LocalTime.MIDNIGHT) && now.isBefore(LocalTime.of(6, 0))) {

	            // "Bugün" sekmesinden önce gelen sekmeleri bul
	            List<WebElement> allTabs = driver.findElements(By.xpath("//span[contains(@class,'menu-item') and contains(@class,'tab')]"));
	            WebElement yesterdayTab = null;

	            for (int i = 0; i < allTabs.size(); i++) {
	                if (allTabs.get(i).getText().contains("Bugün") && i > 0) {
	                    yesterdayTab = allTabs.get(i - 1);
	                    break;
	                }
	            }

	            if (yesterdayTab != null) {
	                // "disabled" class'ı varsa kaldır
	                js.executeScript("arguments[0].classList.remove('disabled');", yesterdayTab);

	                // Overlay veya sabit menü varsa görünene kadar kaydır
	                js.executeScript("arguments[0].scrollIntoView({block:'center'});", yesterdayTab);
	                Thread.sleep(1000);

	                // Tıklama işlemini doğrudan JS ile yap
	                js.executeScript("arguments[0].click();", yesterdayTab);

	                // Menü değişimini bekle
	                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[style*='height:30px']")));
	                Thread.sleep(2000);

	                System.out.println("⏪ Dün sekmesine başarıyla geçildi (JS ile tıklama).");
	            } else {
	                System.out.println("⚠️ Dün sekmesi bulunamadı (muhtemelen tek sekme aktif).");
	            }

	        } else {
	            System.out.println("📅 Şu an bugün sekmesi kullanılabilir, geçiş yapılmadı.");
	        }

	    } catch (Exception e) {
	        System.out.println("⚠️ Dün sekmesi seçilemedi (force click denemesi): " + e.getMessage());
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
	
	public void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
	    new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds)).until(
	        webDriver -> ((JavascriptExecutor) webDriver)
	            .executeScript("return document.readyState").equals("complete"));
	}
	
	public WebDriver getDriver() {
		return driver;
	}

}
