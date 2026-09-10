package com.example.myapplication.Utility;

import com.example.myapplication.Player;
import com.example.myapplication.StatDefinition;
import com.example.myapplication.Model.Constants;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Esporta la lista giocatori con tutte le statistiche in un file .xlsx
 * (stesso layout dello script Python esporta_teammaker_excel.py).
 *
 * Colonne: NOME, COGNOME, SOPRANNOME, ATTIVO, TOTALESTATS, poi una colonna per
 * ogni stat del catalogo (nell'ordine attuale). TOTALESTATS = somma dei valori
 * di tutte le stat del giocatore (bonus escluso, per coerenza con lo script Python).
 */
public class PlayerExportUtility {

    /** Header colorato in blu scuro con testo bianco, come nel Python. */
    private static final String HEADER_FILL = "1F4E78";
    private static final String HEADER_FONT_COLOR = "FFFFFF";

    public static File exportToXlsx(File outputFile) throws IOException {
        List<StatDefinition> statsDefs = StatsUtility.getDefinitions();

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             Workbook wb = new Workbook(fos, "TeamMaker", "1.0")) {

            Worksheet ws = wb.newWorksheet("Giocatori");

            int lastFixedCol = 4; // TOTALESTATS (colonna E)
            int firstStatCol = 5; // colonna F
            int totalCols = firstStatCol + statsDefs.size();

            // ---- HEADER ----
            String[] fixedHeaders = {"NOME", "COGNOME", "SOPRANNOME", "ATTIVO", "TOTALESTATS"};
            for (int i = 0; i < fixedHeaders.length; i++) {
                ws.value(0, i, fixedHeaders[i]);
            }
            for (int i = 0; i < statsDefs.size(); i++) {
                ws.value(0, firstStatCol + i, statsDefs.get(i).label);
            }

            // Stile header: blu scuro, testo bianco, bold, centrato, wrap
            ws.range(0, 0, 0, totalCols - 1).style()
                    .bold()
                    .fillColor(HEADER_FILL)
                    .fontColor(HEADER_FONT_COLOR)
                    .horizontalAlignment("center")
                    .verticalAlignment("center")
                    .wrapText(true)
                    .set();

            // ---- RIGHE GIOCATORI ----
            int rowIndex = 1;
            for (Player player : Constants.players) {
                if (player == null) continue;

                ws.value(rowIndex, 0, str(player.name));
                ws.value(rowIndex, 1, str(player.surname));
                ws.value(rowIndex, 2, str(player.nickname));
                ws.value(rowIndex, 3, player.isActive ? "SI" : "NO");

                double totale = 0;
                for (int i = 0; i < statsDefs.size(); i++) {
                    StatDefinition def = statsDefs.get(i);
                    double value = player.stats != null ? player.stats.get(def.key) : 0;
                    writeNumber(ws, rowIndex, firstStatCol + i, value);
                    totale += value;
                }
                writeNumber(ws, rowIndex, lastFixedCol, totale);
                rowIndex++;
            }

            int lastRow = Math.max(rowIndex - 1, 1);

            // ---- LARGHEZZE COLONNE ----
            ws.width(0, 18); // Nome
            ws.width(1, 20); // Cognome
            ws.width(2, 22); // Soprannome
            ws.width(3, 10); // Attivo
            ws.width(4, 14); // TotaleStats
            for (int i = 0; i < statsDefs.size(); i++) {
                ws.width(firstStatCol + i, 16);
            }

            // Freeze pane su F2: prima riga + prime 5 colonne fisse
            ws.freezePane(firstStatCol, 1);

            // Autofilter su tutta la tabella (frecce a tendina su ogni intestazione)
            if (lastRow >= 1) {
                ws.setAutoFilter(0, 0, lastRow, totalCols - 1);
            }
        }

        return outputFile;
    }

    private static String str(String s) { return s != null ? s : ""; }

    /** Scrive un numero senza .0 inutile (3.0 -> 3, 1.5 -> 1.5) come nel Python. */
    private static void writeNumber(Worksheet ws, int row, int col, double value) {
        if (value == Math.rint(value)) {
            ws.value(row, col, (long) value);
        } else {
            ws.value(row, col, value);
        }
    }
}
