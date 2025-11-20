package GUI;
import javafx.scene.chart.Chart;
import operators.MatrixConfig;
import operators.SelectedMuscles;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class asistencias {

    public static class RoundedButton extends JButton {
        private int radius = 15;

        public RoundedButton(String text) {
            super(text);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setForeground(Color.BLACK);
            setFont(new Font("Century Gothic", Font.BOLD, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Colores de fondo según el estado
            Color bg = getModel().isPressed() ? new Color(200, 210, 230)
                    : getModel().isRollover() ? new Color(235, 240, 250)
                    : getBackground();

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            // Borde
            //g2.setColor(new Color(150, 150, 150));
            g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static MatrixConfig askHeaderRows(Component parent) {
        JSpinner spHeaders = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        JTextField spCols  = new JTextField();
        spCols.setPreferredSize(new Dimension(120, 26));
        spCols.setToolTipText("Ej.: 1,2,5 (índices 1-based)");

        JLabel hint = new JLabel("<html><i>Ej.: <b><span style='color:#0077cc;'>1,2,5</span></b> — no spaces, index 1-based</i></html>");
        hint.setForeground(Color.GRAY);


        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        // Fila 0
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 1;
        panel.add(new JLabel("Number of header rows:"), gc);
        gc.gridx = 1; gc.gridy = 0;
        panel.add(spHeaders, gc);

        // Fila 1
        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Columns to ignore (comma separated):"), gc);
        gc.gridx = 1; gc.gridy = 1;
        panel.add(spCols, gc);

        // Fila 2 (el hint ocupa las dos columnas)
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2;
        panel.add(hint, gc);


        String[] options = { "Accept and continue", "No headers", "Go back" };

        while (true) {
            int choice = JOptionPane.showOptionDialog(
                    parent, panel, "File structure options",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]
            );

            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) { // Go back / cerrar
                return null;
            }

            int nHeaders = (choice == 1) ? 0 : (Integer) spHeaders.getValue(); // "No headers" => 0
            String text = spCols.getText().trim();

            try {
                // Parsear solo DESPUÉS de que el usuario elija
                List<Integer> ignoreCols = parseIgnoreCols(text); // puede ser vacío => no ignorar nada
                return new MatrixConfig(nHeaders, ignoreCols, true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent,
                        "Formato de columnas inválido.\nUsa números separados por comas, p. ej.: 1,2,5",
                        "Invalid input", JOptionPane.ERROR_MESSAGE);
                // vuelve a mostrar el diálogo (loop)
            }
        }
    }

    private static List<Integer> parseIgnoreCols(String text) throws NumberFormatException {
        if (text.isEmpty()) return Collections.emptyList();

        List<Integer> list = new ArrayList<>();
        for (String token : text.split(",")) {
            String s = token.trim();
            if (s.isEmpty()) continue;
            int v = Integer.parseInt(s); // throws NumberFormatException if not a number
            if (v <= 0) throw new NumberFormatException("Indices must be 1-based");
            list.add(v);
        }
        return list;
    }


    public static double[][] readFile(File f, MatrixConfig matrixConfig, SelectedMuscles muscles, String selectedDelim, String selectedDecimal){
        try{
            if (f.getName().toLowerCase().endsWith(".txt") || f.getName().toLowerCase().endsWith(".csv")) {
                return save_ecg.leerCSV(f, muscles.getNMuscles(), matrixConfig, selectedDelim, selectedDecimal);
            } else if (f.getName().toLowerCase().endsWith(".xlsx")) {
                return save_ecg.readExcel(f, muscles.getNMuscles(), matrixConfig);
            } else if (f.getName().toLowerCase().endsWith(".emt")) {
                return save_ecg.readEmt(f, muscles, matrixConfig, selectedDecimal);
            }
        }catch (Exception e){
            matrixConfig.setAccepted(false);
            JOptionPane.showMessageDialog(
                    null,
                    "Could not read the file.\nMake sure the configuration aligns with the file structure.",
                    "Read error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return null;
    }

    public static SelectedMuscles saveMuscles(List<JComboBox<String>> muscleCombos) {
        SelectedMuscles muscles = new SelectedMuscles();
        for (int i = 0; i < muscleCombos.size(); i++) {
            JComboBox<String> cb = muscleCombos.get(i);
            Object sel = cb.getSelectedItem();

            if (sel != null && !sel.toString().equals("Select muscle...")) {
                // índice empezando en 1
                muscles.add(sel.toString(), i + 1);
            }
        }
        //System.out.println(muscles.toString());
        return muscles;
    }

    static double frobNormSq(double[][] A) {
        double s = 0;
        for (double[] row : A) for (double v : row) s += v * v;
        return s;
    }

    static double[][] sub(double[][] A, double[][] B) {
        int m = A.length, n = A[0].length;
        if (B.length != m || B[0].length != n) throw new IllegalArgumentException("sub: shape mismatch");
        double[][] C = new double[m][n];
        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) C[i][j] = A[i][j] - B[i][j];
        return C;
    }

    static double[] frobNormSqRows(double[][] A) {
        int m = A.length, n = A[0].length;
        double[] out = new double[m];
        for (int i = 0; i < m; i++) {
            double s = 0; for (int j = 0; j < n; j++) s += A[i][j]*A[i][j];
            out[i] = s;
        }
        return out;
    }

    public static void saveGraphsWithFileDialog(List<Chart> charts,
                                                double[][] W, double[][] H, List<String> muscles,
                                                int widthPx, int heightPx) {
        if (charts == null || charts.isEmpty()) {
            System.err.println("No hay charts generados todavía.");
            return;
        }

        FileDialog fd = new FileDialog((Frame) null, "Guardar gráficos como ZIP", FileDialog.SAVE);
        fd.setFile("nmf_charts.zip");
        fd.setVisible(true);

        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) {
            System.out.println("Guardado cancelado por el usuario.");
            return;
        }
        if (!file.toLowerCase().endsWith(".zip")) file += ".zip";

        File zipFile = new File(dir, file);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {

            // 2.1) Escribir las imágenes (exacto a como ya lo hacías)
            for (int i = 0; i < charts.size(); i++) {
                Chart chart = charts.get(i);
                int synergy = (i / 2) + 1;
                boolean isSpatial = (i % 2 == 0);
                String name = "synergy" + synergy + "_" +
                        (isSpatial ? "spatialActivation" : "temporalActivation") + ".png";

                byte[] png = snapshotChartToPngBytes(chart, widthPx, heightPx, javafx.scene.paint.Color.WHITE);

                zos.putNextEntry(new ZipEntry(name));
                zos.write(png);
                zos.closeEntry();
            }

            // 2.2) Escribir matrices en un TXT dentro del ZIP
            if (W != null || H != null) {
                String txt = buildMatricesTxt(W, H, muscles);
                zos.putNextEntry(new ZipEntry("matrices.txt"));
                zos.write(txt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            System.out.println("ZIP guardado en: " + zipFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildMatricesTxt(double[][] W, double[][] H, List<String> muscles) {
        StringBuilder sb = new StringBuilder();
        java.text.DecimalFormat df = new java.text.DecimalFormat(
                "0.000000", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));

        // 1) Lista de músculos (si viene)
        if (muscles != null && !muscles.isEmpty()) {
            sb.append("Muscles:\n");
            for (String m : muscles) {
                sb.append("- ").append(m).append('\n');
            }
            sb.append("\n\n");
        }

        if (W != null && W.length > 0) {
            int m = W.length;
            int k = W[0].length;
            sb.append("H - Temporal Activations (time × synergies) =")
                    .append(m).append(" × ").append(k).append("\n");

            for (double[] row : W) {
                for (double v : row) {
                    sb.append(String.format("%10.6f", v)); // ancho fijo = 10, decimales = 6
                }
                sb.append('\n');
            }
            sb.append("\n\n");
        }

        // Matriz H (activaciones temporales)
        if (H != null && H.length > 0) {
            int k = H.length;
            int t = H[0].length;
            sb.append("W - Spatial Synergies (synergies × muscles) = ")
                    .append(k).append(" × ").append(t).append("\n");

            for (double[] row : H) {
                for (double v : row) {
                    sb.append(String.format("%10.6f", v));
                }
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }


    /** Snapshot del Chart a PNG (Scene offscreen + CSS + layout). */
    private static byte[] snapshotChartToPngBytes(Chart chart, int widthPx, int heightPx,
                                                  javafx.scene.paint.Color bg)
            throws InterruptedException, IOException {

        final byte[][] out = new byte[1][];
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        javafx.application.Platform.runLater(() -> {
            try {
                // Asegura CSS + layout del propio chart en su sitio actual
                chart.applyCss();
                chart.layout();

                // Medidas actuales del chart en pantalla (si son 0, usa pref)
                double cw = chart.getWidth();
                double ch = chart.getHeight();
                if (cw <= 0 || ch <= 0) {
                    cw = Math.max(1, chart.prefWidth(-1));
                    ch = Math.max(1, chart.prefHeight(-1));
                }

                // Escala para obtener la resolución deseada
                double sx = widthPx  / cw;
                double sy = heightPx / ch;

                javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                params.setFill(bg);
                params.setTransform(javafx.scene.transform.Transform.scale(sx, sy));

                javafx.scene.image.WritableImage wi =
                        new javafx.scene.image.WritableImage(widthPx, heightPx);

                // Snapshot in-place (no reparenta)
                javafx.scene.image.WritableImage snap = chart.snapshot(params, wi);

                java.awt.image.BufferedImage bi =
                        javafx.embed.swing.SwingFXUtils.fromFXImage(snap, null);

                try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                    javax.imageio.ImageIO.write(bi, "png", baos);
                    out[0] = baos.toByteArray();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        return out[0];
    }


}