package com.control;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.*;
import java.util.*;

/**
 * Nesine canlı skor sayfalarından (futbol ve basketbol) bitmiş maç skorlarını
 * çeker. - 00:00–06:00 arası "Dün" sekmesine otomatik geçer - Bitmiş maçları
 * .board varlığına göre tespit eder - Headless, incognito, cache disable
 * modunda çalışır
 */
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
				"--window-size=1920,1080", "--disable-blink-features=AutomationControlled", "--disable-cache",
				"--incognito");

		driver = new ChromeDriver(options);
		driver2 = new ChromeDriver(options);
		wait = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	// =============================================================
	// ⚽ FUTBOL: Bitmiş maç skorlarını çek
	// =============================================================
	public Map<String, String> fetchFinishedScores() {
		Map<String, String> scores = new HashMap<>();
		try {
			String url = "https://www.nesine.com/iddaa/canli-skor/futbol";
			driver.get(url);
			waitForPageLoad(driver, 10);
			Thread.sleep(1000);
			clickYesterdayTabIfNeeded(driver);
			Thread.sleep(1500);

			wait.until(ExpectedConditions
					.presenceOfAllElementsLocatedBy(By.cssSelector("li.match-not-play .teams-score-content")));

			List<WebElement> matches = driver.findElements(By.cssSelector("li.match-not-play"));
			System.out.println("Toplam maç: " + matches.size());

			for (WebElement match : matches) {
				try {
					// Bitmiş maç: board var mı?
					if (match.findElements(By.cssSelector(".board .home-score")).isEmpty())
						continue;

					String home = safeText(match.findElement(By.cssSelector(".home-team span[aria-hidden='true']")),
							driver);
					String away = safeText(match.findElement(By.cssSelector(".away-team span[aria-hidden='true']")),
							driver);

					WebElement board = match.findElement(By.cssSelector(".board"));
					String homeScore = safeText(board.findElement(By.cssSelector(".home-score")), driver);
					String awayScore = safeText(board.findElement(By.cssSelector(".away-score")), driver);
					String score = homeScore + "-" + awayScore;

					scores.put(home + " - " + away, score);
					System.out.println("✅ " + home + " - " + away + " → " + score);

				} catch (Exception e) {
					System.out.println("⚠️ Futbol maçında hata: " + e.getMessage());
				}
			}

			System.out.println("⚽ Bitmiş maç sayısı: " + scores.size());

		} catch (Exception e) {
			System.out.println("fetchFinishedScores hata: " + e.getMessage());
		}
		return scores;
	}

	// =============================================================
	// 🏀 BASKETBOL: Bitmiş maç skorlarını çek
	// =============================================================
	public Map<String, String> fetchFinishedScoresBasket() {
		Map<String, String> scores = new HashMap<>();
		try {
			String url = "https://www.nesine.com/iddaa/canli-skor/basketbol";
			driver2.get(url);
			waitForPageLoad(driver2, 10);
			Thread.sleep(1000);
			clickYesterdayTabIfNeeded(driver2);
			Thread.sleep(1500);

			wait.until(ExpectedConditions
					.presenceOfAllElementsLocatedBy(By.cssSelector("li.match-not-play .teams-score-content")));

			List<WebElement> matches = driver2.findElements(By.cssSelector("li.match-not-play"));
			System.out.println("Toplam maç (basketbol): " + matches.size());

			for (WebElement match : matches) {
				try {
					if (match.findElements(By.cssSelector(".board .home-score")).isEmpty())
						continue;

					String home = safeText(match.findElement(By.cssSelector(".home-team span[aria-hidden='true']")),
							driver2);
					String away = safeText(match.findElement(By.cssSelector(".away-team span[aria-hidden='true']")),
							driver2);

					WebElement board = match.findElement(By.cssSelector(".board"));
					String homeScore = safeText(board.findElement(By.cssSelector(".home-score")), driver2);
					String awayScore = safeText(board.findElement(By.cssSelector(".away-score")), driver2);
					String score = homeScore + "-" + awayScore;

					scores.put(home + " - " + away, score);
					System.out.println("🏀 " + home + " - " + away + " → " + score);

				} catch (Exception e) {
					System.out.println("⚠️ Basketbol maçında hata: " + e.getMessage());
				}
			}

			System.out.println("🏀 Bitmiş basket maç sayısı: " + scores.size());

		} catch (Exception e) {
			System.out.println("fetchFinishedScoresBasket hata: " + e.getMessage());
		}
		return scores;
	}

	// =============================================================
	// ⏪ Gece 00:00–06:00 arası "Dün" sekmesine geç
	// =============================================================
	private void clickYesterdayTabIfNeeded(WebDriver driver) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			JavascriptExecutor js = (JavascriptExecutor) driver;

			wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".live-result-menu")));
			Thread.sleep(1000);

			LocalTime now = LocalTime.now(ZoneId.of("Europe/Istanbul"));
			if (now.isAfter(LocalTime.MIDNIGHT) && now.isBefore(LocalTime.of(6, 0))) {

				List<WebElement> tabs = driver
						.findElements(By.xpath("//span[contains(@class,'menu-item') and contains(@class,'tab')]"));
				WebElement yesterdayTab = null;

				for (int i = 0; i < tabs.size(); i++) {
					if (tabs.get(i).getText().contains("Bugün") && i > 0) {
						yesterdayTab = tabs.get(i - 1);
						break;
					}
				}

				if (yesterdayTab != null) {
					js.executeScript("arguments[0].classList.remove('disabled');", yesterdayTab);
					js.executeScript("arguments[0].scrollIntoView({block:'center'});", yesterdayTab);
					Thread.sleep(1000);
					js.executeScript("arguments[0].click();", yesterdayTab);
					Thread.sleep(1500);
					System.out.println("⏪ Dün sekmesine geçildi.");
				} else {
					System.out.println("⚠️ Dün sekmesi bulunamadı.");
				}

			} else {
				System.out.println("📅 Şu an bugün sekmesi aktif, geçiş yapılmadı.");
			}

		} catch (Exception e) {
			System.out.println("⚠️ Dün sekmesine geçilemedi: " + e.getMessage());
		}
	}

	// =============================================================
	// 🧹 Yardımcı metotlar
	// =============================================================

	public void close() {
		try {
			driver.quit();
			driver2.quit();
		} catch (Exception ignore) {
		}
	}

	private String safeText(WebElement el, WebDriver driver) {
		try {
			String text = el.getAttribute("textContent");
			if (text == null || text.trim().isEmpty())
				text = el.getText();
			return text == null ? "-" : text.trim();
		} catch (Exception e) {
			try {
				return ((JavascriptExecutor) driver)
						.executeScript("return arguments[0].innerText || arguments[0].textContent;", el).toString()
						.trim();
			} catch (Exception inner) {
				return "-";
			}
		}
	}

	public void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
		new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
				.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
						.equals("complete"));
	}
}
