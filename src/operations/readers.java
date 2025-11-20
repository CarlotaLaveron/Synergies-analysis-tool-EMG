package operations;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;



public class readers {
    private static final String PLACEHOLDER = "Select muscle...";
    private static List<String> musclestouse =  new ArrayList<>();

    public static List<String> getSelectedMuscles(List<JComboBox<String>> muscles) {
        List<String> out = new ArrayList<>();
        for (JComboBox<String> cb : muscles){
            String s = (String) cb.getSelectedItem();
            if (s != null && !PLACEHOLDER.equals(s)) out.add(s);
        }
        musclestouse = out;
        return out;
    }

    public static double[][] getMatrixFromFile(String ruta) {
        File file = new File(ruta);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String firstDataLine = null;

            // 1) Localiza la primera línea no vacía ni comentario
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//"))
                    continue;
                firstDataLine = line;
                break;
            }
            if (firstDataLine == null)
                throw new IOException("El archivo no contiene datos legibles.");

            // 2) Autodetecta delimitador
            char delim = detectDelimiter(firstDataLine);

            // 3) Parseo en streaming
            List<double[]> rows = new ArrayList<>();
            int expectedCols = -1;

            // Reparsea la primera línea
            {
                String[] tokens = splitKeepingWhitespaceOrDelim(firstDataLine, delim);
                tokens = normalizeDecimals(tokens, delim);
                double[] row = parseRow(tokens);
                expectedCols = row.length;
                rows.add(row);
            }

            // Resto de líneas
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//"))
                    continue;

                String[] tokens = splitKeepingWhitespaceOrDelim(line, delim);
                tokens = normalizeDecimals(tokens, delim);
                double[] row = parseRow(tokens);

                if (row.length != expectedCols) {
                    throw new IllegalArgumentException(
                            "Inconsistencia en columnas: esperadas " + expectedCols +
                                    ", encontradas " + row.length + ". Línea: \"" + line + "\""
                    );
                }
                rows.add(row);
            }

            // 4) Convierte List<double[]> a matriz double[][]
            int y = rows.size();        // número de filas (líneas)
            int x = expectedCols;       // número de columnas por línea

            // Mantengo tu convención: matrix[col][row]
            double[][] matrix = new double[x][y];
            for (int j = 0; j < y; j++) {
                double[] r = rows.get(j);
                for (int i = 0; i < x; i++) {
                    matrix[i][j] = r[i];
                }
            }
            return matrix;

        } catch (IOException e) {
            System.err.println("Error de E/S leyendo " + ruta + ": " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("No se pudo convertir un valor numérico: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Error en formato de archivo: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

// --- Helpers ---

    // Detecta ',' ';' '\t' o (si no hay nada de eso) usa espacios en blanco
    private static char detectDelimiter(String line) {
        int commas = count(line, ',');
        int semis  = count(line, ';');
        int tabs   = count(line, '\t');

        if (commas >= semis && commas >= tabs && commas > 0) return ',';
        if (semis  >= commas && semis  >= tabs && semis  > 0) return ';';
        if (tabs   >= commas && tabs   >= semis && tabs   > 0) return '\t';
        // si no hay delimitadores típicos, asumimos espacios
        return ' '; // se interpretará como split por \\s+
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    // Divide la línea según el delimitador detectado.
// Si delim == ' ', usa split por espacios en blanco (uno o más).
    private static String[] splitKeepingWhitespaceOrDelim(String line, char delim) {
        if (delim == ' ') {
            return line.trim().split("\\s+");
        } else if (delim == '\t') {
            return line.split("\t", -1);
        } else {
            // coma o punto y coma (permitimos campos vacíos con -1)
            return line.split("\\" + delim, -1);
        }
    }

    // Normaliza decimales con coma si el delimitador es ';' (CSV europeo típico).
// Si el delimitador es ',', NO tocamos las comas (son separadores).
    private static String[] normalizeDecimals(String[] tokens, char delim) {
        if (delim == ';') {
            String[] out = new String[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                // reemplaza coma decimal por punto
                out[i] = tokens[i].trim().replace(',', '.');
            }
            return out;
        } else {
            String[] out = new String[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                out[i] = tokens[i].trim();
            }
            return out;
        }
    }

    private static double[] parseRow(String[] tokens) {
        double[] row = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].isEmpty())
                throw new NumberFormatException("Campo vacío en la columna " + i);
            row[i] = Double.parseDouble(tokens[i]);
        }
        return row;
    }

}
