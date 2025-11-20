package GUI;
import operators.MatrixConfig;
import operators.SelectedMuscles;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Sheet;
import org.dhatim.fastexcel.reader.Row;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class save_ecg {

    public static double[][] readExcel(File selectedFile, int n_muscles, MatrixConfig m_config) {
        if (selectedFile == null || n_muscles <= 0) return new double[0][0];

        final int nHeaders = m_config.nHeaders;
        final List<Integer> nIgnoreCols = m_config.nIgnoreCols;

        try (InputStream in = Files.newInputStream(selectedFile.toPath());
             ReadableWorkbook wb = new ReadableWorkbook(in)) {

            Sheet sheet = wb.getFirstSheet();          // sheet 0
            List<double[]> rows = new ArrayList<>();

            try (Stream<Row> stream = sheet.openStream()) {
                final int[] rIdx = {0};
                stream.forEach(r -> {
                    // skip headers
                    if (rIdx[0]++ < nHeaders) return;

                    double[] line = new double[n_muscles];
                    boolean allBlank = true;

                    for (int i = 0; i < n_muscles; i++) {

                        if (nIgnoreCols.contains(i + 1)) {
                            continue;  // Si está en la lista, saltar esta columna
                        }

                        // prefer numeric; fallback to string -> parse
                        BigDecimal num = r.getCellAsNumber(i).orElse(null);
                        double val;
                        if (num != null) {
                            val = num.doubleValue();
                            allBlank = false;
                        } else {
                            String s = r.getCellAsString(i).orElse("");
                            val = parseDoubleSafe(s);
                            if (!s.trim().isEmpty()) allBlank = false;
                        }
                        line[i] = val;
                    }
                    if (!allBlank) rows.add(line);
                });
            }

            int y = rows.size();
            double[][] matrix = new double[y][n_muscles];
            for (int j = 0; j < y; j++) {       // tiempo
                double[] line = rows.get(j);    // valores de ese instante
                for (int i = 0; i < n_muscles; i++) {  // canal
                    matrix[j][i] = line[i];
                }
            }
            return matrix;


        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return null;
    }

    private static double parseDoubleSafe(String s) {
        if (s == null) return 0.0;
        s = s.trim();
        if (s.isEmpty()) return 0.0;

        // normalize hard spaces
        String norm = s.replace("\u00A0", "").replace("\u202F", "");

        // remove thousands separators (comma or dot) only when between digit groups of 3
        norm = norm.replaceAll("(?<=\\d)[,\\.](?=\\d{3}(\\D|$))", "");

        // if there is a comma decimal and no dot decimal, convert comma→dot
        if (norm.indexOf(',') >= 0 && norm.indexOf('.') < 0) norm = norm.replace(',', '.');

        try { return Double.parseDouble(norm); } catch (Exception ex) { return 0.0; }
    }

    public static double[][] leerCSV(File selectedFile, int n_muscles, MatrixConfig m_config,
                                     String delimiter, String delim_decimal) {

        if (selectedFile == null || n_muscles <= 0) return new double[0][0];
        int n_headers = m_config.nHeaders;
        List<Integer> n_ignoreCols = m_config.nIgnoreCols; // 1-based

        List<String> lines = new ArrayList<>();
        try (BufferedReader buffer = Files.newBufferedReader(selectedFile.toPath(), StandardCharsets.UTF_8)) {
            String temp;
            while ((temp = buffer.readLine()) != null) {
                lines.add(temp);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (lines.isEmpty() || lines.size() <= n_headers)
            return new double[0][0];

        final String splitRegex = toDelimiterRegex(delimiter);

        // líneas de datos (sin cabeceras)
        List<String> data = new ArrayList<>(lines.subList(n_headers, lines.size()));
        data.removeIf(l -> {
            if (l == null) return true;
            String t = l.trim();
            if (t.isEmpty()) return true;
            String noSpaces = t.replaceAll("\\s+", "");
            return noSpaces.matches("^[,;]+$");
        });

        if (data.isEmpty()) return new double[0][0];

        // usamos la primera línea de datos para saber cuántas columnas hay
        String firstLine = data.get(0)
                .replace("\uFEFF", " ")
                .replace("\u00A0", " ")
                .replace("\u202F", " ");
        String[] firstTokens = firstLine.split(splitRegex, -1);
        int totalCols = firstTokens.length;

        // 1) Construimos el mapa de columnas para los músculos (0-based)
        List<Integer> muscleCols = new ArrayList<>();
        for (int col0 = 0; col0 < totalCols && muscleCols.size() < n_muscles; col0++) {
            int col1 = col0 + 1; // 1-based para comparar con n_ignoreCols
            if (n_ignoreCols.contains(col1)) {
                continue; // ignorada
            }
            muscleCols.add(col0); // esta columna será un músculo
        }

        if (muscleCols.size() < n_muscles) {
            throw new IllegalStateException(
                    "Not enough non-ignored columns: needed " + n_muscles +
                            ", found " + muscleCols.size() + " (totalCols=" + totalCols +
                            ", ignore=" + n_ignoreCols + ")"
            );
        }

        int y = data.size(); // filas de datos
        double[][] matrix = new double[y][n_muscles];

        // 2) Leemos los datos usando el mapa de columnas
        for (int j = 0; j < y; j++) {
            String line = data.get(j)
                    .replace("\uFEFF", " ")
                    .replace("\u00A0", " ")
                    .replace("\u202F", " ");
            String[] tokens = line.split(splitRegex, -1);

            for (int i = 0; i < n_muscles; i++) {
                int colIndex = muscleCols.get(i); // columna real en tokens[]
                if (colIndex >= tokens.length) {
                    matrix[j][i] = 0.0;
                    continue;
                }
                String tok = tokens[colIndex].trim();
                matrix[j][i] = parseNumberWithDecimal(tok, delim_decimal);
            }
        }

        return matrix;
    }


    private static String toDelimiterRegex(String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) return Pattern.quote(",");
        String d = delimiter.trim();
        if (d.equalsIgnoreCase("Tab"))   return "\\t+";
        if (d.equalsIgnoreCase("Space")) return "\\s+";
        return Pattern.quote(d);
    }

    private static double parseNumberWithDecimal(String s, String decimalSep) {
        if (s == null) return 0.0;
        s = s.trim();
        if (s.isEmpty()) return 0.0;

        boolean commaDecimal = (decimalSep != null && decimalSep.equals(","));

        // normaliza espacios duros
        s = s.replace("\u00A0", "").replace("\u202F", "");

        // elimina separadores de miles sólo cuando están entre grupos de 3 dígitos
        if (commaDecimal) {
            // miles con punto: 1.234,56 -> quita SÓLO los puntos de miles
            s = s.replaceAll("(?<=\\d)\\.(?=\\d{3}(\\D|$))", "");
            // decimal con coma -> punto
            s = s.replace(',', '.');
        } else {
            // miles con coma: 1,234.56 -> quita SÓLO las comas de miles
            s = s.replaceAll("(?<=\\d),(?=\\d{3}(\\D|$))", "");
            // decimal con punto ya está bien
        }

        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0; // o Double.NaN si prefieres detectar errores
        }
    }

    public static double[][] readEmt(File file, SelectedMuscles muscles, MatrixConfig cfg, String decimalDelimiter) throws IOException {
        if (file == null) return new double[0][0];

        List<double[]> data = new ArrayList<>();
        int nHeaders = cfg.nHeaders;
        Set<Integer> ignoreCols = (cfg.nIgnoreCols != null)
                ? new HashSet<>(cfg.nIgnoreCols)
                : Collections.emptySet();

        boolean decimalIsComma = ",".equals(decimalDelimiter);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum <= nHeaders) continue; // ignorar cabeceras

                // Normalizar espacios raros
                line = line.replaceAll("[\\u00A0\\u2007\\u202F\\u2060\\u3000]", " ").trim();
                // Convertir separadores no númericos a espacios
                line = line.replaceAll("[^0-9eE,\\.\\-+]+", " ").trim();
                line = line.trim();
                if (line.isEmpty()) continue;

                // Dividir por cualquier tipo de espacio o tabulador
                String[] parts = line.split("\\s+");

                List<Double> values = new ArrayList<>();
                for (int i = 0; i < parts.length; i++) {
                    int col = i + 1;
                    if (ignoreCols.contains(col)) continue;

                    String token = parts[i];
                    if (decimalIsComma) token = token.replace(',', '.');

                    try {
                        values.add(Double.parseDouble(token));
                    } catch (NumberFormatException e) {
                        // Ignorar texto (por ejemplo "Frame" o "Time")
                    }
                }

                if (values.isEmpty()) continue;

                int nMuscles = muscles.getNMuscles();
                if (values.size() != nMuscles) {
                    System.err.println("⚠️ Línea " + lineNum + ": " + values.size() +
                            " columnas útiles; se esperaban " + nMuscles);
                }

                double[] row = new double[values.size()];
                for (int i = 0; i < values.size(); i++) row[i] = values.get(i);
                data.add(row);
            }
        }

        // Convertir lista en matriz
        double[][] matrix = new double[data.size()][];
        for (int i = 0; i < data.size(); i++) matrix[i] = data.get(i);

        return matrix;
    }

}
