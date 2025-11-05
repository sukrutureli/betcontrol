package com.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class HtmlGenerator {

	private static final ObjectMapper mapper = new ObjectMapper();

	public static void generateHtmlFromJson(String jsonPath, String outputHtmlPath) throws IOException {
		// 🔹 JSON oku
		File jsonFile = new File(jsonPath);
		if (!jsonFile.exists()) {
			System.out.println("⚠️ JSON dosyası bulunamadı: " + jsonFile.getAbsolutePath());
			return;
		}

		List<Map<String, Object>> data = mapper.readValue(jsonFile, List.class);

		// 🔹 HTML başı
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n<html lang='tr'>\n<head>\n").append("<meta charset='UTF-8'>\n")
				.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n")
				.append("<title>📅 Tahminler - BettingSukru</title>\n").append("<style>").append(FILES_STYLE)
				.append("</style>\n</head>\n<body>\n")
				.append("<h1>📅 Tahminler (" + jsonFile.getName().replace(".json", "") + ")</h1>\n")
				.append("<div class='history-container'>\n");

		// 🔹 Sayaçlar
		int wonCount = 0, lostCount = 0, pendingCount = 0;
		int totalPicks = 0;

		// 🔹 Her maç için HTML kartı oluştur
		for (Map<String, Object> match : data) {
			String home = Objects.toString(match.get("homeTeam"), "-");
			String away = Objects.toString(match.get("awayTeam"), "-");
			String score = Objects.toString(match.get("score"), "-");
			Object picksObj = match.get("picks") != null ? match.get("picks") : match.get("pick");

			List<String> picks;
			if (picksObj instanceof List)
				picks = (List<String>) picksObj;
			else
				picks = Collections.singletonList(Objects.toString(picksObj, "-"));

			Map<String, String> statuses = (Map<String, String>) match.get("statuses");

			html.append("<div class='match-card'>").append("<div class='match-header'>").append(home).append(" - ")
					.append(away).append("</div>")
					.append("<div class='match-info'><strong>Skor:</strong> <span class='score'>").append(score)
					.append("</span></div>").append("<div class='picks'>");

			for (String pick : picks) {
				String st = "pending";
				if (statuses != null && statuses.containsKey(pick)) {
					st = statuses.get(pick);
				} else if (match.get("status") != null) {
					st = Objects.toString(match.get("status"), "pending");
				}

				totalPicks++;
				if ("won".equals(st))
					wonCount++;
				else if ("lost".equals(st))
					lostCount++;
				else
					pendingCount++;

				String statusClass = st.equals("won") ? "won" : st.equals("lost") ? "lost" : "pending";
				String statusText = st.equals("won") ? "✅" : st.equals("lost") ? "❌" : "⏳";

				html.append("<div class='pick-line'>").append("<span class='pick-label'>").append(pick)
						.append("</span>").append("<span class='status ").append(statusClass).append("'>")
						.append(statusText).append("</span>").append("</div>");
			}

			html.append("</div></div>\n");
		}

		// 🔹 Başarı oranı
		double successRate = totalPicks > 0 ? (wonCount * 100.0 / (totalPicks - pendingCount)) : 0.0;

		html.append("<div class='stats-box'>📊 <strong>Toplam ").append(totalPicks).append(" tahmin</strong><br>✅ ")
				.append(wonCount).append(" | ❌ ").append(lostCount).append(" | ⏳ ").append(pendingCount)
				.append("<br><span style='color:#28a745;'>Başarı oranı: %").append(String.format("%.1f", successRate))
				.append("</span></div>\n");

		// 🔹 Footer
		html.append("</div>\n<footer>").append("Bu veriler geçmiş tahmin sonuçlarından otomatik oluşturulmuştur.<br>")
				.append("<small>© 2025 BettingSukru</small>").append("</footer>\n</body>\n</html>");

		// 🔹 Dosyaya yaz
		Files.write(Paths.get(outputHtmlPath), html.toString().getBytes());
		System.out.println("✅ HTML üretildi: " + outputHtmlPath);
	}

	private static final String FILES_STYLE = "body {font-family:'Segoe UI',Arial,sans-serif;background:#f4f6fa;color:#222;margin:0;padding:20px;}\n"
			+ "h1 {text-align:center;color:#004d80;margin-bottom:10px;}\n"
			+ ".history-container {max-width:750px;margin:0 auto;display:flex;flex-direction:column;gap:14px;}\n"
			+ ".match-card {background:#fff;border:1px solid #dce3ec;border-radius:10px;padding:14px 16px;box-shadow:0 2px 6px rgba(0,0,0,0.08);}\n"
			+ ".match-header {font-weight:700;color:#003366;font-size:1.05em;margin-bottom:6px;}\n"
			+ ".match-info {font-size:0.9em;color:#555;margin-bottom:10px;}\n"
			+ ".score {font-weight:600;color:#111;}\n"
			+ ".picks {margin-top:8px;display:flex;flex-direction:column;gap:4px;}\n"
			+ ".pick-line {display:flex;align-items:center;justify-content:space-between;background:#f8fafc;padding:6px 10px;border-radius:6px;font-size:0.9em;border:1px solid #e2e6eb;}\n"
			+ ".pick-label {font-weight:600;color:#003366;}\n"
			+ ".status {display:inline-block;padding:3px 8px;border-radius:6px;font-size:0.85em;font-weight:600;color:white;min-width:60px;text-align:center;}\n"
			+ ".won {background:#28a745;} .lost {background:#f75270;} .pending {background:#ffc107;color:#222;}\n"
			+ ".stats-box {text-align:center;margin-top:25px;padding:10px;background:#fff;border:1px solid #dce3ec;border-radius:8px;box-shadow:0 2px 6px rgba(0,0,0,0.08);font-weight:600;color:#003366;}\n"
			+ "footer{text-align:center;font-size:0.85em;color:#777;margin-top:30px;}";

}
