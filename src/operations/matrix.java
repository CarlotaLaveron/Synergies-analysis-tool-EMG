package operations;

import java.util.Arrays;

public class matrix {

    public static void printMatrix(double[][] m){
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                System.out.print(m[i][j] + "  ");
            }
            System.out.println();
        }

    }

    public static double[][] multiplication(double[][] m1, double[][] m2){
        double[][] product = new double[m1.length][m2[0].length];
        if (m1[0].length == m2.length) {
            for (int i = 0; i < m1.length; i++) {
                for (int j = 0; j < m2[0].length; j++) {
                    for (int k = 0; k < m1[0].length; k++) {
                        product[i][j] += m1[i][k] * m2[k][j];
                    }
                }
            }
        }
        return product;
    }

    public static double [][] transpose (double[][] m){
        int rows = m.length;
        int cols = m[0].length;
        double[][] transpose = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transpose[j][i] = m[i][j];
            }
        }
        return transpose;
    }

    public static double[][] subtraction (double[][] m1, double[][] m2){
        double[][] difference = new double[m1.length][m2[0].length];
        if (m1.length != m2.length || m1[0].length != m2[0].length) {
            throw new IllegalArgumentException("Las matrices deben tener las mismas dimensiones");
        }
        else{
            for (int i = 0; i < m1.length; i++) {
                for (int j = 0; j < m2[0].length; j++) {
                    difference[i][j] = m1[i][j] - m2[i][j];
                }
            }
        }
        return difference;
    }

    public static boolean equal_withinTOL (double[][] m_new, double[][] m){
        int counter = 0;
        int c = m.length * m[0].length;
        double[][] difference = subtraction(m_new, m);

        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                if(difference[i][j] > -0.000001 && difference[i][j] < 0.000001 && counter<c) {
                    counter++;
                }
            }
        }

        if(counter == c){
            return true;
        }else{
            return false;
        }

    }

}
