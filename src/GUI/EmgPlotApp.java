package GUI;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class EmgPlotApp {

    // Para asegurarnos de inicializar JavaFX solo una vez
    private static boolean fxInitialized = false;

    private static void initFX() {
        if (!fxInitialized) {
            // JFXPanel inicializa el toolkit de JavaFX en una app Swing
            new JFXPanel();
            Platform.setImplicitExit(false);
            fxInitialized = true;
            System.out.println("JavaFX inicializado");
        }
    }

    /**
     * Abre una NUEVA ventana Swing que contiene un panel JavaFX
     * con dos gráficos (antes y después).
     *
     * Las matrices son matrix[muestra][canal] (columnas = canales).
     */
    public static void show(double[][] emgBefore, double[][] emgAfter) {

        // Seguridad básica por si te llega algo raro
        if (emgBefore == null || emgBefore.length == 0 ||
                emgAfter == null  || emgAfter.length == 0) {
            System.out.println("Matrices vacías, no se plotea");
            return;
        }

        System.out.println("show() llamado. Muestras: " + emgBefore.length +
                "  Canales: " + emgBefore[0].length);

        // 1) Inicializar JavaFX si hace falta
        initFX();

        // 2) Crear la ventana Swing en el hilo de Swing
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("EMG - Comparación antes/después");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(1000, 600);
            frame.setLocationRelativeTo(null);

            // 3) Crear el JFXPanel: contenedor JavaFX dentro de Swing
            JFXPanel fxPanel = new JFXPanel();
            frame.add(fxPanel);
            frame.setVisible(true);

            // 4) Crear la escena JavaFX dentro del JFXPanel
            Platform.runLater(() -> {
                System.out.println("Platform.runLater ejecutándose (JavaFX OK)");

                // --- Gráfico ANTES ---
                NumberAxis xAxisBefore = new NumberAxis();
                NumberAxis yAxisBefore = new NumberAxis();
                xAxisBefore.setLabel("Time");
                yAxisBefore.setLabel("Amplitude EMG");

                LineChart<Number, Number> chartBefore =
                        new LineChart<>(xAxisBefore, yAxisBefore);
                chartBefore.setTitle("EMG - Before processing");
                chartBefore.setCreateSymbols(false);

                // --- Gráfico DESPUÉS ---
                NumberAxis xAxisAfter = new NumberAxis();
                NumberAxis yAxisAfter = new NumberAxis();
                xAxisAfter.setLabel("Time");
                yAxisAfter.setLabel("Amplitude EMG");

                LineChart<Number, Number> chartAfter =
                        new LineChart<>(xAxisAfter, yAxisAfter);
                chartAfter.setTitle("EMG - After processing");
                chartAfter.setCreateSymbols(false);

                // Añadir los datos (columnas = canales)
                addMatrixToChart(emgBefore, chartBefore, "Canal");
                addMatrixToChart(emgAfter, chartAfter, "Canal");

                // Pestañas Antes / Después
                TabPane tabPane = new TabPane();

                Tab tabBefore = new Tab("Antes", chartBefore);
                tabBefore.setClosable(false);

                Tab tabAfter = new Tab("Después", chartAfter);
                tabAfter.setClosable(false);

                tabPane.getTabs().addAll(tabBefore, tabAfter);
                tabPane.setStyle("-fx-background-color: white;");


                // Crear escena y asignarla al JFXPanel
                Scene scene = new Scene(tabPane);
                fxPanel.setScene(scene);
            });
        });
    }

    /**
     * matrix[muestra][canal]
     * Cada columna (canal) se convierte en una serie del gráfico.
     */
    private static void addMatrixToChart(double[][] matrix,
                                         LineChart<Number, Number> chart,
                                         String baseSeriesName) {
        if (matrix == null || matrix.length == 0) return;

        int numMuestras = matrix.length;
        int numCanales  = matrix[0].length;

        System.out.println("Añadiendo datos al gráfico: " +
                numMuestras + " muestras, " + numCanales + " canales");

        for (int canal = 0; canal < numCanales; canal++) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(baseSeriesName + " " + (canal + 1));

            for (int muestra = 0; muestra < numMuestras; muestra++) {
                double valor = matrix[muestra][canal]; // columnas = canales
                series.getData().add(new XYChart.Data<>(muestra, valor));
            }

            chart.getData().add(series);
        }
    }
}
