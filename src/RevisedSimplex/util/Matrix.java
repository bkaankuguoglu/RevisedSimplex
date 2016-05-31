package RevisedSimplex.util;
/**
 * Matrix abstract data structure that holds a two dimensional nxn matrix
 * as a private field and the method implements matrix equation in the form Ax=b.
 * 
 * @author Berk Kaan Kuguoglu
 *
 */
public class Matrix {
	public double A[][];
	private int size;

	public Matrix(int size) {
		A = new double[size][size];
		this.size = size;
	}

/**
 * Computes the matrix equation Ax=b with the x and b vectors given as input.
 * It arbitrarily returns true.
 * 	
 * @param x
 * @param b
 * @return true 
 */
	public boolean solve(double[] x, double[] b) { // Ax=b 
		int swap;
		double max;
		double scale; 
		double temp;

		for (int col = 0; col < size - 1; col++) {
			max = Math.abs(A[col][col]);
			swap = col;
			for (int i = col + 1; i < size; i++)
				if (Math.abs(A[i][col]) > max) {
					max = Math.abs(A[i][col]);
					swap = i;
				}

			if (swap != col) {
				temp = b[swap];
				b[swap] = b[col];
				b[col] = temp;
				for (int i = 0; i < size; i++) {
					temp = A[swap][i];
					A[swap][i] = A[col][i];
					A[col][i] = temp;
				}
			}

			if (A[col][col] != 0)
				for (int row = col + 1; row < size; row++) {
					scale = A[row][col] / A[col][col];
					b[row] -= (scale * b[col]);
					for (int k = col; k < size; k++) {
						A[row][k] -= (scale * A[col][k]);
					}
				}
		}

		for (int col = size - 1; col >= 0; col--) {

			x[col] = b[col];
			for (int row = size - 1; row > col; row--)
				x[col] -= (x[row] * A[col][row]);

			if (A[col][col] != 0)
				x[col] /= A[col][col];
		}
		return true;
	}
}