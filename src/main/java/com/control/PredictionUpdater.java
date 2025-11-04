package com.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PredictionUpdater {

	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * GitHub Pages üzerindeki JSON'u indirir, skorları günceller, güncel
	 * versiyonunu "data/2025-10-16-updated.json" olarak kaydeder.
	 */
	public static void updateFromGithub(Map<String, String> updatedScores, String prefix) throws IOException {
		// 🔹 dünün tarihini bul
		String day = "";

		LocalTime now = LocalTime.now(ZoneId.of("Europe/Istanbul"));
		if (now.isAfter(LocalTime.MIDNIGHT) && now.isBefore(LocalTime.of(6, 0))) {
			day = LocalDate.now(ZoneId.of("Europe/Istanbul")).minusDays(1)
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		} else {
			day = LocalDate.now(ZoneId.of("Europe/Istanbul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		}

		// 🔹 GitHub Pages URL'si
		String url = "https://sukrutureli.github.io/bettingsukru/data/" + prefix + day + ".json";
		System.out.println("📥 JSON indiriliyor: " + url);

		// 🔹 JSON’u indir
		List<PredictionData> predictions;
		try (InputStream in = new URL(url).openStream()) {
			predictions = mapper.readerForListOf(PredictionData.class).readValue(in);
		} catch (Exception e) {
			System.out.println("�?� JSON indirilemedi: " + e.getMessage());
			return;
		}

		// 🔹 Güncelle
//		for (PredictionData p : predictions) {
//			String key = (p.getHomeTeam() + " - " + p.getAwayTeam()).trim();
//			System.out.println(key);
//			if (updatedScores.containsKey(key)) {
//				String score = updatedScores.get(key);
//				p.setScore(score);
//				if (prefix.equals("")) {
//					evaluatePredictions(p, score, "Futbol");
//				} else if (prefix.equals("basketbol-")) {
//					evaluatePredictions(p, score, "Basketbol");
//				}
//
//			}
//		}

		for (PredictionData p : predictions) {
			String home = p.getHomeTeam();
			String away = p.getAwayTeam();
			String matchedKey = null;

			int count = 0;
			// 🔹 1️⃣ Önce tam eşleşme kontrolü
			for (String key : updatedScores.keySet()) {
				String[] parts = key.split(" - ");
				if (parts.length == 2) {
					String homeKey = parts[0];
					String awayKey = parts[1];
					
					if (home.equals(homeKey) && away.equals(awayKey)) {
	      matchedKey = key;
	count++;
}
				}
			}

			if (matchedKey != null && count == 1) {
				String score = updatedScores.get(matchedKey);
				p.setScore(score);
				if (prefix.equals("")) {
					evaluatePredictions(p, score, "Futbol");
				} else if (prefix.equals("basketbol-")) {
					evaluatePredictions(p, score, "Basketbol");
				}
			} else {
				System.out.println("⚠️ Eşleşme bulunamadı: " + p.getHomeTeam() + " - " + p.getAwayTeam());
			}
		}

		// 🔹 Lokale kaydet (örnek: data/2025-10-15-updated.json)
		File outDir = new File("public/data");
		if (!outDir.exists())
			outDir.mkdirs();

		File outFile = new File(outDir, prefix + day + ".json");
		mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, predictions);

		System.out.println("✅ Güncellenmiş dosya: " + outFile.getAbsolutePath());
	}

	/**
	 * Skora göre "won/lost/pending" durumu belirler
	 */
	private static void evaluatePredictions(PredictionData p, String score, String type) {
		try {
			String[] parts = score.split("-");
			int home = Integer.parseInt(parts[0].trim());
			int away = Integer.parseInt(parts[1].trim());

			for (String pick : p.getPicks()) {
				String result = "";
				if (type.equals("Futbol")) {
					result = evaluatePick(pick, home, away);
				} else if (type.equals("Basketbol")) {
					result = evaluatePickBasketbol(pick, home, away);
				}
				p.getStatuses().put(pick, result);
			}

		} catch (Exception e) {
			System.err.println("⚠�? Skor formatı hatalı: " + score);
		}
	}

	private static String evaluatePick(String pick, int home, int away) {
		if (pick.contains("MS1"))
			return home > away ? "won" : "lost";
		if (pick.contains("MS2"))
			return away > home ? "won" : "lost";
		if (pick.contains("MSX"))
			return away == home ? "won" : "lost";

		if (pick.toLowerCase().contains("üst"))
			return (home + away) > 2.5 ? "won" : "lost";
		if (pick.toLowerCase().contains("alt"))
			return (home + away) < 2.5 ? "won" : "lost";

		if (pick.toLowerCase().contains("var"))
			return (home > 0 && away > 0) ? "won" : "lost";
		if (pick.toLowerCase().contains("yok"))
			return (home == 0 || away == 0) ? "won" : "lost";

		return "pending";
	}

	private static String evaluatePickBasketbol(String pick, int home, int away) {
		String[] splitPick = pick.split(" ");
		Double barem = null;
		if (Character.isDigit(pick.charAt(0))) {
			barem = Double.valueOf(splitPick[0].replace(",", "."));
		}

		if (pick.contains("MS1"))
			return home > away ? "won" : "lost";
		if (pick.contains("MS2"))
			return away > home ? "won" : "lost";

		if (pick.toLowerCase().contains("üst"))
			return (home + away) > barem ? "won" : "lost";
		if (pick.toLowerCase().contains("alt"))
			return (home + away) < barem ? "won" : "lost";

		return "pending";
	}
}

