package operators;

import java.util.List;

public class MatrixConfig {
    public int nHeaders;
    public List<Integer> nIgnoreCols;
    public boolean accepted;


    public MatrixConfig(int nHeaders, List<Integer>  nIgnoreCols) {
        this.nHeaders = nHeaders;
        this.nIgnoreCols = nIgnoreCols;
    }

    public MatrixConfig(int nHeaders, List<Integer>  nIgnoreCols, boolean a) {
        this.nHeaders = nHeaders;
        this.nIgnoreCols = nIgnoreCols;
        this.accepted = a;
    }

    public int getnHeaders() {
        return nHeaders;
    }

    public void setnHeaders(int nHeaders) {
        this.nHeaders = nHeaders;
    }

    public List<Integer>  getnIgnoreCols() {
        return nIgnoreCols;
    }

    public void setnIgnoreCols(List<Integer>  nIgnoreCols) {
        this.nIgnoreCols = nIgnoreCols;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
