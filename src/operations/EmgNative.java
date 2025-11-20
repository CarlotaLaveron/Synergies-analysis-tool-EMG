package operations;


import java.util.Arrays;

public class EmgNative {
    static {
        try {
            // si ejecutas desde el directorio raíz del proyecto:
            String path = "./filtering/emglib.dll";     // o la ruta absoluta
            System.load(new java.io.File(path).getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }

    }

    public native double[] filterChannel(
            double[] samples,
            int sampleFrequency,
            double rangeSeconds,
            int minEmgFreq,
            int maxEmgFreq,
            boolean highPassOn
    );

    public static double[][] filterAllChannels(double[][] X,
                                         int sampleFrequency,
                                         double rangeSeconds,
                                         int minEmgFreq,
                                         int maxEmgFreq,
                                         boolean highPassOn) {

        if (X == null || X.length == 0) return X;

        int nSamples  = X.length;        // filas
        int nChannels = X[0].length;     // columnas

        double[][] out = new double[nSamples][nChannels];

        for (int ch = 0; ch < nChannels; ch++) {
            // 1) sacar la columna 'ch' como vector 1D
            double[] channel = new double[nSamples];
            for (int i = 0; i < nSamples; i++) {
                channel[i] = X[i][ch];
            }

            EmgNative emg = new EmgNative();
            double[] filtered = emg.filterChannel(
                    channel,
                    sampleFrequency,
                    rangeSeconds,
                    minEmgFreq,
                    maxEmgFreq,
                    highPassOn
            );

            // opcional: sanity check
            if (filtered.length != nSamples) {
                throw new IllegalStateException("Filtered channel length != original length");
            }

            // 3) guardar el resultado de vuelta en la matriz
            for (int i = 0; i < nSamples; i++) {
                out[i][ch] = filtered[i];
            }
        }

        return out;
    }
}
