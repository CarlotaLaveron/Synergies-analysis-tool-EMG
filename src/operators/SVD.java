package operators;

public class SVD {
    double[][] U, Vt;
    double[] S;

    public SVD(double[][] U, double[] S, double[][] Vt) {
        this.U = U;
        this.S = S;
        this.Vt = Vt;
    }


    public double[][] getU() {
        return U;
    }

    public void setU(double[][] u) {
        U = u;
    }

    public double[][] getVt() {
        return Vt;
    }

    public void setVt(double[][] vt) {
        Vt = vt;
    }

    public double[] getS() {
        return S;
    }

    public void setS(double[] s) {
        S = s;
    }
}
