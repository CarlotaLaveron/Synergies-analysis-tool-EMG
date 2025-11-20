package operators;

public class TwoMatrixes {
    double [][] W;
    double [][] H;

    public TwoMatrixes(double[][] W, double[][] H) {
        this.W = W;
        this.H = H;
    }

    public TwoMatrixes() {

    }

    public double[][] getW() {
        return W;
    }
    public void setW(double[][] W) {
        this.W = W;
    }
    public double[][] getH() {
        return H;
    }
    public void setH(double[][] H) {
        this.H = H;
    }

}
