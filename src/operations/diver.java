package operations;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import operators.TwoMatrixes;
import operators.SVD;

//OPCIONES ERAN JAMA, COMMON MATHS Y EJML, EJML ES LA MEJOR
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.decomposition.svd.SvdImplicitQrDecompose_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;

import javax.swing.*;
import java.awt.*;
import javafx.scene.paint.Color;



public class diver {
    //TODO: BUSCAR JUSTIFICACION PARA LOS VALORES
    private static final int DEFAULT_MAX_ITERS = 10000;
    private static final double DEFAULT_TOL = 1e-3;
    private static final int Kmax = 8;
    private static final int DEFAULT_MIN_ITERS = 15;
    private static final int Kmin = 1;

    public static TwoMatrixes default_diver(double[][] X) {
        return diver(X, DEFAULT_MAX_ITERS, DEFAULT_TOL);
    }

    public TwoMatrixes customizable_diver(double[][] X, int maxIters, double tol) {
        return diver(X, maxIters, tol);
    }

    public static TwoMatrixes diver(double[][] X, int maxIters, double tol){
        //matrix.printMatrix(X);
        final double eps = 1e-12;
        double prevSSE = Double.POSITIVE_INFINITY;

        int m = X.length;
        int T = X[0].length;
        int Kcap = Math.min(Kmax, T);
        System.out.println("Kcap: " + Kcap);
        System.out.println("Kmax: " + Kmax);
        System.out.println("Min: " + T);
        int k = selectK(X, Kcap);

        TwoMatrixes default_hw = new TwoMatrixes();
        default_hw = nndsvdInit(X, k);
        double [][] H = default_hw.getH();
        double [][] W = default_hw.getW();
        for (int it = 0; it < maxIters; it++){
            // --- update W ---
            double[][] XHt  = matrix.multiplication(X, matrix.transpose(H));
            double[][] WH   = matrix.multiplication(W, H);
            double[][] WHHt = matrix.multiplication(WH, matrix.transpose(H));
            for (int i = 0; i < W.length; i++)
                for (int j = 0; j < W[0].length; j++)
                    W[i][j] *= XHt[i][j] / (WHHt[i][j] + eps);

            // --- update H ---
            double[][] Wt   = matrix.transpose(W);
            double[][] WtX  = matrix.multiplication(Wt, X);
            double[][] WtW  = matrix.multiplication(Wt, W);
            double[][] WtWH = matrix.multiplication(WtW, H);
            for (int i = 0; i < H.length; i++)
                for (int j = 0; j < H[0].length; j++)
                    H[i][j] *= WtX[i][j] / (WtWH[i][j] + eps);

            WH = matrix.multiplication(W, H);
            double sse = frobSSE(X, WH);

            if ((prevSSE - sse) / (prevSSE + eps) < tol && it > DEFAULT_MIN_ITERS) break;
            prevSSE = sse;
            System.out.println(it);
        }

        TwoMatrixes result = new TwoMatrixes();
        result.setW(W);
        result.setH(H);
        return result;
    }

    static TwoMatrixes nndsvdInit(double[][] X, int k) {
        int m = X.length, n = X[0].length;
        System.out.println("-------dEBUG SVD-------");
        System.out.printf("X shape: %d x %d%n", m, n);
        System.out.printf("k pedido: %d%n", k);

        // 1) truncated SVD rank k
        SVD svd = svdTruncate(X, k);// we provide this helper below
        double[][] U = svd.getU();
        double[][] Vt = svd.getVt();
        double[] S = svd.getS();

        int rU = (U.length > 0 ? U[0].length : 0);   // columnas de U
        int rVt = Vt.length;                         // filas de Vt
        int rS = S.length;
        System.out.printf("U shape: %d x %d%n", U.length, rU);
        System.out.printf("Vt shape: %d x %d%n", Vt.length, (Vt.length>0?Vt[0].length:0));
        System.out.printf("S length: %d%n", rS);

        double[][] W = new double[m][k];
        double[][] H = new double[k][n];

        // 2) first component: absolute values
        double sigma0 = Math.sqrt(S[0]);
        for (int i = 0; i < m; i++)
            W[i][0] = sigma0 * Math.abs(U[i][0]);
        for (int j = 0; j < n; j++)
            H[0][j] = sigma0 * Math.abs(Vt[0][j]);

        // 3) remaining components
        for (int c = 1; c < k; c++) {

            double[] u = column(U, c);
            double[] v = row(Vt, c);

            // split into positive / negative parts
            double[] uPos = positive(u), uNeg = negative(u);
            double[] vPos = positive(v), vNeg = negative(v);

            double uPosNorm = norm(uPos), vPosNorm = norm(vPos);
            double uNegNorm = norm(uNeg), vNegNorm = norm(vNeg);

            // choose the part (pos or neg) with higher energy
            if (uPosNorm * vPosNorm >= uNegNorm * vNegNorm) {
                assignComponent(W, H, c, uPos, vPos, S[c]);
            } else {
                assignComponent(W, H, c, uNeg, vNeg, S[c]);
            }
        }

        // 4) NNDSVDa: replace zeros with tiny random values
        double eps = 1e-3;
        fillZerosWithRandom(W, eps);
        fillZerosWithRandom(H, eps);

        return new TwoMatrixes(W, H);
    }

    static final long MAX_CELLS = 100_000_000;

    static SVD svdTruncate(double[][] X, int k) throws IllegalStateException {
        int m = X.length, n = X[0].length;

        long cells = (long) m * n;
        if (cells > MAX_CELLS) {
            throw new IllegalArgumentException(
                    "Matrix too large for SVD (" + m + " x " + n + " = " + cells +
                            " elements). Reduce file size or number of muscles.");
        }

        long free = Runtime.getRuntime().maxMemory();   // JVM max
        long required = (long) m * n * 8;              // bytes

        if (required > free * 0.7) { // usa 70% como límite seguro
            throw new IllegalStateException(
                    "Not enough memory for SVD.\nNeeded ≈ " + (required/1e6) + " MB, available ≈ " + (free/1e6) + " MB.\n " +
                            "Matrix too large for SVD (" + m + " x " + n + " = " + cells + "elements). " +
                            "Reduce file size or number of muscles."
            );
        }

        DMatrixRMaj A = new DMatrixRMaj(X);

        // computeU=true, computeV=true, compact=true  ← important
        SvdImplicitQrDecompose_DDRM svd =
                (SvdImplicitQrDecompose_DDRM) DecompositionFactory_DDRM.svd(m, n, true, true, true);
        if (!svd.decompose(A)) throw new RuntimeException("SVD failed");

        DMatrixRMaj Uc  = svd.getU(null, false); // m×r (r=min(m,n)=5)
        DMatrixRMaj VtC = svd.getV(null, true);  // r×n (5×5)
        double[] Sfull  = svd.getSingularValues();

        int r = Math.min(k, Sfull.length);
        double[][] U  = new double[m][r];
        double[][] Vt = new double[r][n];
        double[]   S  = new double[r];

        for (int j = 0; j < r; j++) {
            S[j] = Sfull[j];
            for (int i = 0; i < m; i++) U[i][j]   = Uc.get(i, j);
            for (int i = 0; i < n; i++) Vt[j][i]  = VtC.get(j, i);
        }
        return new SVD(U, S, Vt);
    }


    // --- take column of matrix ---
    static double[] column(double[][] A, int c) {
        double[] v = new double[A.length];
        for (int i = 0; i < A.length; i++) v[i] = A[i][c];
        return v;
    }

    // --- take row of matrix ---
    static double[] row(double[][] A, int r) {
        double[] v = new double[A[0].length];
        for (int j = 0; j < A[0].length; j++) v[j] = A[r][j];
        return v;
    }

    // --- positive and negative parts ---
    static double[] positive(double[] x) {
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) y[i] = Math.max(0, x[i]);
        return y;
    }
    static double[] negative(double[] x) {
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) y[i] = Math.max(0, -x[i]);
        return y;
    }

    // --- L2 norm ---
    static double norm(double[] x) {
        double s = 0;
        for (double v : x) s += v*v;
        return Math.sqrt(s);
    }

    // --- assign one component of W and H ---
    static void assignComponent(double[][] W, double[][] H,
                                int c, double[] u, double[] v, double s) {
        double scale = Math.sqrt(s * norm(u) * norm(v));
        if (scale == 0) scale = 1e-12;
        for (int i = 0; i < W.length; i++) W[i][c] = u[i] / norm(u) * scale;
        for (int j = 0; j < H[0].length; j++) H[c][j] = v[j] / norm(v) * scale;
    }

    // --- replace zeros with tiny random ---
    static void fillZerosWithRandom(double[][] A, double eps) {
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < A.length; i++)
            for (int j = 0; j < A[0].length; j++)
                if (A[i][j] == 0) A[i][j] = eps * r.nextDouble();
    }

    public static TwoMatrixes diverHW(double[][] X, double[][] W, double[][] H, int maxIters, double tol){
        final double eps = 1e-12;
        double prevSSE = Double.POSITIVE_INFINITY;
        for (int it = 0; it < maxIters; it++){
            //update W
            double[][] XHt  = matrix.multiplication(X, matrix.transpose(H));
            double[][] WH   = matrix.multiplication(W, H);
            double[][] WHHt = matrix.multiplication(WH, matrix.transpose(H));
            for (int i = 0;i < W.length; i++)
                for (int j = 0; j < W[0].length; j++)
                    W[i][j] *= XHt[i][j] / (WHHt[i][j] + eps);

            //update H
            double[][] Wt   = matrix.transpose(W);
            double[][] WtX  = matrix.multiplication(Wt, X);
            double[][] WtW  = matrix.multiplication(Wt, W);
            double[][] WtWH = matrix.multiplication(WtW, H);
            for (int i = 0; i < H.length; i++)
                for (int j=0; j<H[0].length; j++)
                    H[i][j] *= WtX[i][j] / (WtWH[i][j] + eps);

            double sse = frobSSE(X, WH);
            if (((prevSSE - sse) / (prevSSE + eps)) < tol) break;  // mejora relativa
            prevSSE = sse;

        }

        TwoMatrixes result = new TwoMatrixes();
        //matrix.printMatrix(W);
        //matrix.printMatrix(H);
        result.setW(W);
        result.setH(H);
        //matrix.printMatrix(result.getW());
        //matrix.printMatrix(result.getH());
        return result;
    }

    public static int selectK(double[][] X, int Kcap) {// try ranks 1..12
        final int repeats = 8;
        final int maxIters = 2000;
        final double tol = 1e-8;    // convergence tolerance
        final double dvThreshold = 0.01; // stop when VAF gain < 1%
        final double eps = 1e-12;
        double bestSSE_k = Double.POSITIVE_INFINITY;

        final int mRows = X.length;
        final int T = X[0].length;

        double prevVAF = 0.0;
        final double denom = frobNorm2(X) + eps;
        final int nK = Kcap - Kmin + 1;
        double[] vafByK = new double[nK];


        for (int k = Kmin; k <= Kcap; k++) {

            for (int r = 0; r < repeats; r++) {
                java.util.Random rng = new java.util.Random(System.nanoTime() + r + 31L*k);

                double[][] W0 = randPlus(mRows, k, rng);
                double[][] H0 = randPlus(k, T, rng);

                TwoMatrixes d = diverHW(X, W0, H0, maxIters, tol);
                double[][] X_prime = matrix.multiplication(d.getW(), d.getH());
                double sse = frobSSE(X, X_prime);

                if (sse < bestSSE_k) bestSSE_k = sse;
            }

            double vaf = 1.0 - bestSSE_k / denom;
            vafByK[k - Kmin] = vaf;
            double gain = (k == Kmin) ? vaf : (vaf - prevVAF);

            //System.out.printf("k=%d  VAF=%.4f  gain=%.4f%n", k, vaf, gain);

            if (k > Kmin && gain < dvThreshold) {
                final int kOptFinal = k-1;       // para usar dentro de la lambda
                final double[] vafByKFinal = vafByK;

                javax.swing.SwingUtilities.invokeLater(() -> {
                    JFrame frame = new JFrame("VAF vs Number of Synergies");
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                    JFXPanel fxPanel = createVafChartPanel(vafByKFinal, Kmin, kOptFinal);
                    fxPanel.setPreferredSize(new Dimension(600, 450));
                    frame.getContentPane().add(fxPanel);

                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);

                });

                return kOptFinal;

            }
            prevVAF = vaf;
        }

        return Kcap;
    }

    public static JFXPanel createVafChartPanel(double[] vafByK, int kMin, int kOpt) {
        JFXPanel fxPanel = new JFXPanel();

        Platform.runLater(() -> {
            NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel("Number of synergies (k)");

            NumberAxis yAxis = new NumberAxis(60, 100, 5);
            yAxis.setLabel("VAF (%)");

            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle("VAF vs Number of Synergies (k_opt = " + kOpt + ")");

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("VAF");

            // ===== SOLO HASTA EL ÚLTIMO VALOR COMPUTADO =====
            int lastIdx = vafByK.length - 1;
            // retrocede mientras haya ceros al final (no calculados)
            while (lastIdx > 0 && vafByK[lastIdx] == 0.0) {
                lastIdx--;
            }

            for (int i = 0; i <= lastIdx; i++) {
                int k = kMin + i;
                double vaf = vafByK[i] * 100.0;
                series.getData().add(new XYChart.Data<>(k, vaf));
            }
            // ================================================

            lineChart.getData().add(series);

            // fondo blanco
            lineChart.setStyle("-fx-background-color: white;");

            Scene scene = new Scene(lineChart, 800, 600);
            scene.setFill(Color.WHITE);  // fondo de toda la escena en blanco
            fxPanel.setScene(scene);

            // dejar también blanco solo el área de la gráfica (sin gris)
            Platform.runLater(() -> {
                Node plotBg = lineChart.lookup(".chart-plot-background");
                if (plotBg != null) {
                    plotBg.setStyle("-fx-background-color: white;");
                }
            });
        });

        return fxPanel;
    }



    private static double[][] randPlus(int r, int c, java.util.Random rng) {
        double[][] A = new double[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                A[i][j] = Math.max(1e-6, rng.nextDouble());
        return A;
    }
    private static double frobSSE(double[][] A, double[][] B) {
        int r = A.length, c = A[0].length; double s = 0;
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++) { double d = A[i][j] - B[i][j]; s += d*d; }
        return s;
    }
    private static double frobNorm2(double[][] A) {
        int r = A.length, c = A[0].length; double s = 0;
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++) s += A[i][j]*A[i][j];
        return s;
    }
}

