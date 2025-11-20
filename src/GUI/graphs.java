package GUI;

import javafx.application.*;
import javafx.collections.FXCollections;
import javafx.embed.swing.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import operations.matrix;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class graphs {
    private static final JFXPanel view = new JFXPanel();   // <-- único componente que expondremos
    public JComponent getView() {                   // <-- para añadirlo a tu Swing
        return view;
    }

    /** Build a 3-column grid and place N charts in it */
    public static void mountGridCharts(List<Chart> charts) {
        Platform.setImplicitExit(false);

        Platform.runLater(() -> {
            GridPane grid = new GridPane();
            grid.setHgap(16);
            grid.setVgap(16);
            grid.setPadding(new Insets(16));

            ColumnConstraints c1 = new ColumnConstraints();
            ColumnConstraints c2 = new ColumnConstraints();
            c1.setPercentWidth(50); c1.setHgrow(Priority.ALWAYS);
            c2.setPercentWidth(50); c2.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().addAll(c1, c2);

            //List<Chart> charts = graphs.nmfCharts(W, H, muscles);
            for (int i = 0; i < charts.size(); i++) {
                int col = i % 2, row = i / 2;
                grid.add(createCellFixed(charts.get(i), 420), col, row);
            }

            ScrollPane scroll = new ScrollPane(grid);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);
            scroll.setPannable(true);

            view.setScene(new Scene(scroll));
        });

    }

    /** One responsive cell per chart: fixed 3 columns, controlled aspect ratio */
    private static StackPane createCellFixed(Chart chart, double heightPx) {
        StackPane cell = new StackPane(chart);

        // la celda se estira a lo ancho (50% de la fila), alto fijo
        cell.setMinSize(0, heightPx);
        cell.setPrefSize(Region.USE_COMPUTED_SIZE, heightPx);
        cell.setMaxSize(Double.MAX_VALUE, heightPx);

        // el chart rellena la celda
        chart.setMinSize(0, 0);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        chart.prefWidthProperty().bind(cell.widthProperty());
        chart.prefHeightProperty().bind(cell.heightProperty());

        cell.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10 10 2 10; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        return cell;
    }

    public static List<Chart> nmfCharts(double[][] W, double[][] H, List<String> muscleLabels) {
        List<Chart> list = new ArrayList<>();

        // W: T x k  (tiempo x sinergias)
        // H: k x m  (sinergias x músculos)

        int T  = W.length;        // nº de muestras/tiempo = 51
        int kW = W[0].length;     // nº de sinergias según W
        int kH = H.length;        // nº de sinergias según H
        int m  = H[0].length;     // nº de músculos/canales = 5

        // Comprobaciones de forma
        if (kW != kH) {
            throw new IllegalArgumentException(
                    "Dimensiones incompatibles: W.cols=" + kW + " != H.rows=" + kH
            );
        }
        int k = kW;

        // Comprobaciones de ragged
        for (int t = 0; t < T; t++) {
            if (W[t].length != k) {
                throw new IllegalArgumentException("W es ragged en fila " + t);
            }
        }
        for (int s = 0; s < k; s++) {
            if (H[s].length != m) {
                throw new IllegalArgumentException("H es ragged en fila " + s);
            }
        }

        // Labels: uno por músculo
        if (muscleLabels != null && muscleLabels.size() != m) {
            throw new IllegalArgumentException("muscleLabels.size()=" + muscleLabels.size() +
                    " pero H tiene " + m + " columnas (músculos).");
        }

        for (int s = 0; s < k; s++) {
            // --- Barras: pesos de la sinergia s en cada músculo -> H[s, :] ---
            double[] weights = new double[m];
            for (int i = 0; i < m; i++) {
                weights[i] = H[s][i];      // fila s (sinergia), col i (músculo)
            }
            list.add(barChartH("Synergy " + (s + 1), muscleLabels, weights));

            // --- Línea: activación temporal de la sinergia s -> W[:, s] ---
            double[] activation = new double[T];
            for (int t = 0; t < T; t++) {
                activation[t] = W[t][s];   // fila t (tiempo), col s (sinergia)
            }
            list.add(lineChartW("Activation " + (s + 1), activation));
        }

        return list;
    }



    public static BarChart<String, Number> barChartH(String title, List<String> labels, double[] values) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Muscle");
        xAxis.setCategories(FXCollections.observableArrayList(labels));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Weight");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setPadding(new Insets(0));


        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < labels.size(); i++) {
            series.getData().add(new XYChart.Data<>(labels.get(i), values[i]));
        }
        xAxis.setCategories(FXCollections.observableArrayList(labels));
        chart.getData().add(series);
        chart.lookupAll(".default-color0.chart-bar")
                .forEach(n -> n.setStyle("-fx-bar-fill: #8ea7d1;"));

        Platform.runLater(() -> {
            // 1) sin padding extra dentro del chart
            Node content = chart.lookup(".chart-content");
            if (content instanceof Region r) r.setPadding(new Insets(0));

            // 2) restaurar estilos por si antes se aplicó algo
            Node axisLine = xAxis.lookup(".axis-line");
            if (axisLine != null) axisLine.setStyle(""); // no mover la línea

            Node axisLabel = xAxis.lookup(".axis-label");
            if (axisLabel instanceof Region xr) xr.setPadding(new Insets(8, 0, 0, 0)); // mismo “feeling” que Time

            Node xAxisNode = xAxis.lookup(".axis");
            if (xAxisNode != null) xAxisNode.setStyle("-fx-tick-label-gap: 2; -fx-tick-length: 3; -fx-font-size: 11px;");

            Node yAxisNode = yAxis.lookup(".axis");
            if (yAxisNode != null) yAxisNode.setStyle("-fx-tick-label-gap: 4; -fx-tick-length: 3; -fx-font-size: 11px;");

            // 3) para que el CategoryAxis no pida tanta altura como con texto horizontal
            xAxis.setTickLabelRotation(35); // 30–45° según te guste
            // si aún quieres menos altura, reduce un poco la fuente:
            // xAxis.lookup(".axis").setStyle("-fx-font-size: 10px;");

            // 4) mantener el plot limpio, como en el LineChart
            Node plot = chart.lookup(".chart-plot-background");
            if (plot != null) plot.setStyle("-fx-background-insets: 0; -fx-padding: 0;");
        });


        return chart;
    }

    public static LineChart<Number, Number> lineChartW(String title, double[] values) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Activation");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int t = 0; t < values.length; t++) {
            series.getData().add(new XYChart.Data<>(t, values[t]));
        }

        chart.getData().add(series);
        Platform.runLater(() ->
                chart.lookup(".chart-series-line").setStyle("-fx-stroke: #8ea7d1;")
        );
        return chart;
    }
}