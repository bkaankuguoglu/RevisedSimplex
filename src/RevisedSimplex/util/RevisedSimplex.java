package RevisedSimplex.util;

/**
 * Implementation of the method revised simplex and all the corresponding data structures related
 * to it. It works seamlessly with the GUI package of the project and handles the back-end operations.
 * In the implementation, I've used some sources as a reference, or a starting point, to my project, 
 * these are:
 * 
 *  - "Introduction to Operations Research", 10th Edition", Hillier & Lieberman
 *  - "A Java Library of Graph Algorithms and Optimization", T. Lau 
 *  - "Numerical Methods, Algorithms and Tools in C#", Dos Passos
 *  - http://www.neos-guide.org/content/simplex-method
 *  
 *  Furthermore, I've utilized some open source codes from these sources. With all due respect, 
 *  I hereby refer to these sources and I claim I have no intention to commit a plagiarism. 
 *  In fact, I've contributed to their previous works when I found it necessary. 
 * 
 */
import java.lang.Math;

public class RevisedSimplex {
	public int numVariables;
	public int numConstraints;
	public int numNonbasic;
	public int numConstraintsTaken = 0;
	public int currentStep = 0;
	public int numIterations = 0;
	public int numArtificials;

	public double[] reducedCost;
	public double[] cost;
	public double[] x;
	public double[] pi;
	public double[] yB;

	public double MinRatio;
	public int NumMinRatio;

	public Matrix Bt;
	public Matrix B;

	public double[] costOfBasicVars;
	public double objectiveValue = 0;

	public double[][] A;
	public double[] b;
	public int[] constraintType;
	public int[] BasicVariables;
	public int[] NonBasicVariables;
	public int[] varType;
	public double[] colOfA;

	int[] permanentBasis;

	public int enteringVar;
	public int leavingVar;

	public boolean isMinimize;
	public boolean ArtificialAdded = false;

	/*
	 * We're going to use these variables for the 2nd phase.
	 */
	public boolean oldOptimizationType;
	double originalCost[];

	/**
	 * Constructor for Revised Simplex class. It acts as a starter to the method
	 * by initializing the variables needed during the method. It takes
	 * numOfVars and numOfConstraints as inputs.
	 * 
	 * @param numOfVars
	 * @param numOfConstraints
	 */
	public RevisedSimplex(int numOfVars, int numOfConstraints) {
		numVariables = numOfVars;
		numConstraints = numOfConstraints;
		numArtificials = 0;

		BasicVariables = new int[numOfConstraints];
		NonBasicVariables = new int[numOfVars + 2 * numOfConstraints];
		originalCost = new double[numOfVars + 2 * numOfConstraints];
		costOfBasicVars = new double[numOfConstraints];
		reducedCost = new double[numOfVars + 2 * numOfConstraints];
		cost = new double[numOfVars + 2 * numOfConstraints];

		constraintType = new int[numOfConstraints];
		varType = new int[numOfVars + 2 * numOfConstraints];

		A = new double[numOfConstraints][numOfVars + 3 * numOfConstraints];
		b = new double[numOfConstraints];
		Bt = new Matrix(numOfConstraints);
		B = new Matrix(numOfConstraints);
		yB = new double[numOfConstraints];
		x = new double[numOfConstraints];
		pi = new double[numOfConstraints];
		colOfA = new double[numOfConstraints];

	}

	/**
	 * Main method that computes the method step by step. It s composed of 12
	 * different cases from 0-11 and each one of them has their own particular
	 * conditions, such as updating the matrices used throughout the method and
	 * doing optimality checks. It updates the matrix B as long as the iteration
	 * goes on.
	 * 
	 * @return currentStep that is going to be implemented in the next iteration
	 */

	public int iterateOneStep() {
		switch (currentStep) {
		case 0: // update the B matrix
			numIterations++;
			this.makeBt();
			this.makeB();
			currentStep = 1;
			return 0; // continue iterating

		case 1: // update pi
			Bt.solve(pi, costOfBasicVars);
			currentStep = 2;
			return 0; // continue iterating

		case 2: // calculate reduced costs
			this.calculateReducedCosts();
			currentStep = 3;
			return 0; // continue iterating

		case 3: // optimality check
			if (this.optimalityCheck()) {
				currentStep = 7;
				return 1; // the solution is optimal
			} else {
				currentStep = 4;
				this.enteringVariable();
				return 0; // continue iterating
			}

			/*
			 * User is able to change the sequence of the iteration by changing
			 * the entering variable. It iterates normally if none action is
			 * taken by the user.
			 */
		case 4:
			currentStep = 5;
			return 0; // continue iterating

		case 5: // update yB
			for (int i = 0; i < numConstraints; i++)
				colOfA[i] = A[i][NonBasicVariables[enteringVar]];
			// yB = (B^-1)(Ai)
			B.solve(yB, colOfA);
			currentStep = 6;
			return 0; // continue iterating

		case 6: // unboundedness test
			if (this.testUnboundedness()) {
				currentStep = 7;
				return 3; // the z value is unbounded
			} else
				currentStep = 8;
			return 0; // continue iterating

		case 7: // the optimal solution is found
			objectiveValue = this.calculateObjective();
			return 1; // optimal

		case 8: // decision for leaving variable
			leavingVariable();
			if (NumMinRatio == 1)
				currentStep = 11;
			else // if there is a tie for leaving variable, then the decision is
					// made by user.
				currentStep = 9;
			return 0; // continue iterating

		case 9: // user's choice is taken
			currentStep = 10;
			return 0; // continue iterating

		case 10: // arbitrary step that only prints the leaving variable on the
					// message box
			currentStep = 11;
			return 0; // continue iterating

		case 11: // the solution is updated
			this.updateSolution();
			objectiveValue = this.calculateObjective();
			currentStep = 0;
			return 0; // continue iterating
		}
		return 0;
	}

	/**
	 * Similar to iterate one step method, it also iterates but with full
	 * iteration. Instead of any case statements, it does all the compilation at
	 * once.
	 * 
	 * @return
	 */

	public int iterate() {
		numIterations++;
		this.makeBt();

		// update Pi (Bt*Pi=cB)

		Bt.solve(pi, costOfBasicVars);

		// calculate reduced costs
		this.calculateReducedCosts();

		// optimality check

		if (!this.optimalityCheck()) {
			this.enteringVariable(); // if not found yet, choose entering
										// variable.
		} else {
			objectiveValue = this.calculateObjective();
			return 1; // if optimal solution is found
		}

		// update B
		this.makeB();

		// update yB
		for (int i = 0; i < numConstraints; i++)
			colOfA[i] = A[i][NonBasicVariables[enteringVar]];

		// B = yB * (column of a matrix corresponds to the entering variable)
		B.solve(yB, colOfA);

		// unboundedness test

		if (!this.testUnboundedness()) {
			this.leavingVariable();
			this.updateSolution();
			return 0; // continue iterating
		} else
			return 3; // the LP is unbounded.

	}

	/**
	 * At each iteration, our B matrix and right hand side values changes, so
	 * does the value of the objective function. It simply does z = (cB*xB)
	 * 
	 * @return value value of the objective function
	 */

	public double calculateObjective() {
		double value = 0;
		if (isMinimize == true)
			for (int i = 0; i < numConstraints; i++)
				value += (x[i] * cost[BasicVariables[i]]);
		else
			for (int i = 0; i < numConstraints; i++)
				value -= (x[i] * cost[BasicVariables[i]]);

		return value;
	}

	/**
	 * Decides the variable to be taken out from the basis by finding using the
	 * minimum ratio test.
	 * 
	 */
	private void leavingVariable() {
		double rat;
		int minIndex = -1;

		NumMinRatio = 0;

		for (int i = 0; i < numConstraints; i++) {
			if (yB[i] > 0) {
				rat = x[i] / yB[i];
				if (NumMinRatio == 0) {
					MinRatio = rat;
					minIndex = i;
					NumMinRatio = 1;
				} else if (rat < MinRatio) {
					MinRatio = rat;
					minIndex = i;
					NumMinRatio = 1;
				} else if (rat == MinRatio)
					NumMinRatio++;
			}
		}

		leavingVar = minIndex;

	}

	/**
	 * Updates the solution vector by swapping entering and leaving variables.
	 * It also takes the number of artificial variables into account.
	 */
	private void updateSolution() {
		int temp;

		// Xi = Xi_old - cB*B^-1
		for (int i = 0; i < numConstraints; i++)
			x[i] -= (MinRatio * yB[i]);

		x[leavingVar] = MinRatio;

		if (varType[BasicVariables[leavingVar]] == 2) // artificial
			numArtificials--;
		if (varType[NonBasicVariables[enteringVar]] == 2) // artificial
			numArtificials++;

		temp = BasicVariables[leavingVar];
		BasicVariables[leavingVar] = NonBasicVariables[enteringVar];
		NonBasicVariables[enteringVar] = temp;

	}

	/**
	 * Calculates reduced costs vector, (c - cB * B^-1 * A)
	 * 
	 */
	private void calculateReducedCosts() {
		for (int i = 0; i < numNonbasic; i++) {
			for (int j = 0; j < numConstraints; j++)
				colOfA[j] = A[j][NonBasicVariables[i]];

			reducedCost[i] = cost[NonBasicVariables[i]] - this.Dot(pi, colOfA, numConstraints);
		}
	}

	/**
	 * Even though every non basic variable that has negative c - cB*B^-1*A
	 * value (reduced cost) will do fine. However, it's written in a way that it
	 * chooses the one with the lowest reduced cost.
	 */
	private void enteringVariable() {
		int minIndex = 0;
		double minValue = 999999;

		for (int i = 0; i < numNonbasic; i++)
			if (reducedCost[i] < 0 && reducedCost[i] < minValue) {
				minValue = reducedCost[i];
				minIndex = i;
			}

		enteringVar = minIndex;

	}

	/**
	 * Check if the given element i is in the basis.
	 * 
	 * @param i
	 * @param BasisSize
	 * @param basis
	 * @return
	 */
	private boolean isBasic(int i, int BasisSize, int[] basis) {
		for (int j = 0; j < BasisSize; j++) {
			if (basis[j] == i)
				return true;
		}
		return false;
	}

	/**
	 * If yB > 0 for all basic variables, the lp is unbounded. The method checks
	 * whether or not the problem is bounded.
	 * 
	 * @return
	 */
	private boolean testUnboundedness() {
		for (int i = 0; i < numConstraints; i++)
			if (yB[i] > 0) {
				return false;
			}
		return true;
	}

	/**
	 * Optimality check for the current basic feasible solution.
	 * 
	 * @return isOptimal boolean value for optimality
	 */
	private boolean optimalityCheck() {
		boolean isOptimal = true;

		for (int i = 0; i < numNonbasic; i++)
			if (reducedCost[i] < 0) {
				isOptimal = false;
				return isOptimal;
			}
		return isOptimal;
	}

	/**
	 * Updates Bt Matrix
	 */

	private void makeBt() {
		for (int i = 0; i < numConstraints; i++) {
			costOfBasicVars[i] = cost[BasicVariables[i]];
			for (int j = 0; j < numConstraints; j++)
				Bt.A[i][j] = A[j][BasicVariables[i]];
		}
	}

	/**
	 * Updates B Matrix
	 */
	private void makeB() {
		for (int i = 0; i < numConstraints; i++)
			for (int j = 0; j < numConstraints; j++)
				B.A[i][j] = A[i][BasicVariables[j]];
	}

	/**
	 * Takes an array of double values as coefficients for the constraint
	 * equation to be added, a double value of right hand side and the type of
	 * the equation.
	 * 
	 * @param coefficients
	 * @param rhs
	 * @param type
	 */
	public void addConstraint(double[] coefficients, double rhs, int type) {
		for (int i = 0; i < numVariables; i++) {
			A[numConstraintsTaken][i] = coefficients[i];
		}
		x[numConstraintsTaken] = rhs;
		b[numConstraintsTaken] = rhs;
		constraintType[numConstraintsTaken] = type;
		numConstraintsTaken++;
	}

	/**
	 * Decision for the objective function. Max or Min.
	 * 
	 * @param coefficients
	 * @param isMinimize
	 */
	public void optimizationType(double[] coefficients, boolean isMinimize) {
		for (int i = 0; i < numVariables; i++)
			cost[i] = coefficients[i];
		this.isMinimize = isMinimize;
	}

	/**
	 * It initialize the revised simplex method by creating the augmented matrix
	 * and corresponding slack and artificial variables.
	 * 
	 * @param numberOfVariables
	 * @param numberOfConstraints
	 * @return
	 */
	public boolean initialize(int numberOfVariables, int numberOfConstraints) {
		int lastCol;
		int next;
		int[] ConstraintVariable = new int[numberOfConstraints];

		int slack;
		int artificial;

		oldOptimizationType = isMinimize;
		lastCol = numberOfVariables;

		/*
		 * It is required to multiply all coefficients in the objective function
		 * with -1 to make sure that it can optimize the maximization problem.
		 */

		if (!isMinimize)
			for (int i = 0; i < lastCol; i++)
				cost[i] *= -1;

		for (int i = 0; i < lastCol; i++)
			NonBasicVariables[i] = i;

		next = lastCol;

		for (int i = 0; i < numberOfConstraints; i++)
			switch (constraintType[i]) {
			case 0: // less than
				cost[lastCol] = 0;
				A[i][lastCol] = 1;
				ConstraintVariable[i] = lastCol;
				varType[lastCol] = 1; // slack or surplus
				lastCol++;
				break;

			case 1: // greater than
				cost[lastCol] = 0;
				A[i][lastCol] = -1;
				ConstraintVariable[i] = lastCol;
				varType[lastCol] = 1; // slack or surplus
				lastCol++;
				break;
			case 2: // equal to
			}

		/*
		 * Adding artificial variables when it's needed. Also updates the basis.
		 */
		for (int i = 0; i < numberOfConstraints; i++)
			switch (constraintType[i]) {

			case 0: // less than
				// slack variables added to the basis.
				if (b[i] >= 0) {
					BasicVariables[i] = ConstraintVariable[i];
					x[i] = b[i];
				} else { /* b[i] < 0 */
					// artificial variables added to the basis.
					A[i][lastCol] = -1;
					x[i] = -b[i];
					varType[lastCol] = 2; // artificial
					ArtificialAdded = true;
					BasicVariables[i] = lastCol;

					slack = ConstraintVariable[i];
					NonBasicVariables[next] = slack;

					next++;
					lastCol++;
					numArtificials++;
				}
				break;

			case 1: // greater than
				if (b[i] > 0) {
					x[i] = b[i];
					varType[lastCol] = 2; // artificial
					ArtificialAdded = true;
					A[i][lastCol] = 1;
					BasicVariables[i] = lastCol;
					artificial = ConstraintVariable[i];
					NonBasicVariables[next] = artificial;
					next++;
					lastCol++;
					numArtificials++;
				} else {
					BasicVariables[i] = ConstraintVariable[i];
					x[i] = -b[i];
				}
				break;

			case 2: // equal to
				if (b[i] >= 0) {
					x[i] = b[i];
					A[i][lastCol] = 1;

				} else {
					x[i] = -b[i];
					A[i][lastCol] = -1;

				}

				varType[lastCol] = 2; // artificial
				ArtificialAdded = true;
				BasicVariables[i] = lastCol;
				lastCol++;
				numArtificials++;
				break;

			}

		numNonbasic = lastCol - numConstraints;
		numVariables = lastCol;

		if (numArtificials > 0)
			this.calculateInitialCosts();

		return true;
	}

	/**
	 * Calculates the costs related to the given problem and, after eliminating
	 * artificial variables computes the costs for the phase one.
	 */
	private void calculateInitialCosts() {
		double newCoefficients[] = new double[numVariables];
		for (int i = 0; i < numVariables; i++) {
			originalCost[i] = cost[i];
			if (varType[i] == 2) // artificial
				newCoefficients[i] = 1;
			else
				newCoefficients[i] = 0;
		}
		this.optimizationType(newCoefficients, true); // isMinimize = true
	}

	/**
	 * Multiplication method for two vectors and it simply multiplies two
	 * vectors with size of "n"
	 * 
	 * @param row
	 * @param col
	 * @param n
	 * @return
	 */
	private double Dot(double[] row, double[] col, int n) {
		double result = 0;
		for (int i = 0; i < n; i++)
			result += row[i] * col[i];
		return result;
	}

	/**
	 * Clear every data being computed so far and sets each field to its default
	 * value.
	 * 
	 * @param numberOfVariables
	 * @param numberOfConstraints
	 */
	public void clear(int numberOfVariables, int numberOfConstraints) {
		for (int i = 0; i < numConstraints; i++)
			for (int j = 0; j < numVariables; j++)
				A[i][j] = 0;

		numConstraintsTaken = 0;
		numVariables = numberOfVariables;
		objectiveValue = 0;
		currentStep = 0;
		ArtificialAdded = false;
		numArtificials = 0;
	}

	/**
	 * Matrix procedure. More information about the process can be found in the
	 * following articles:
	 * 
	 * Why QR factorization? :
	 * http://www.sciencedirect.com/science/article/pii/0898122185901907
	 * 
	 * Other sources:
	 * http://e-collection.library.ethz.ch/eserv/eth:47547/eth-47547-01.pdf
	 * https://arxiv.org/pdf/1408.3518.pdf
	 * http://www.caam.rice.edu/~timwar/NUDG/Jars/Jama/QRDecomposition.java
	 * 
	 * @param basisSize
	 *            number of elements in the particular part of the basis
	 * @return
	 */

	private int AugmentBasis(int basisSize) {
		int high;
		int low;
		int temp;

		double[][] newBMatrix;
		double NormVector;
		double v1;
		double v2;
		double dtemp;
		double[] tempVector;

		int rows;
		int cols;

		rows = numConstraints;
		cols = numVariables;

		// Defensive programming approach to check for possible errors.

		if (basisSize < 0 || rows <= 0 || cols <= 0) {
			System.out.println("Ooops, are you sure you have taken any LP course?");
			return 1;
		}

		if (basisSize > rows) {
			System.out.println("There are more elements in the basis than the number of rows!");
			System.out.println("Basis Size : " + basisSize + " rows : " + rows);
			return 1;
		}

		if (rows > cols) {
			System.out.println("The number of rows is greater than the number of columns");
			System.out.println("rows : " + rows + " columns : " + cols);
			return 1;
		}

		for (int i = 0; i < basisSize; i++) {
			if (BasicVariables[i] < 0 || BasicVariables[i] >= cols) {
				System.out.println("Basis element[" + i + "]= " + BasicVariables[i] + " is not in the range");
				return 1;
			}
		}

		if (basisSize == rows) {
			System.out.println("Basis already has the right number of elements");
			return 0;
		}

		newBMatrix = new double[rows][cols];
		permanentBasis = new int[cols];

		/*
		 * Now we are going to transfer columns of A to the new B Matrix by
		 * holding the data of the current basis in the first location. The rest
		 * will be placed starting from cols-basisSize.
		 */

		low = 0;
		high = cols - 1;

		for (int i = 0; i < cols; i++) {
			if (isBasic(i, basisSize, BasicVariables)) {
				for (int j = 0; j < rows; j++)
					newBMatrix[j][low] = A[j][i];
				permanentBasis[low] = i;
				low++;
			} else {
				for (int j = 0; j < rows; j++)
					newBMatrix[j][high] = A[j][i];
				permanentBasis[high] = i;
				high--;
			}
		}

		tempVector = new double[rows];

		// QR factorization

		for (int i = 0; i < basisSize; i++) {

			NormVector = 0;
			for (int j = i; j < rows; j++)
				NormVector += newBMatrix[j][i] * newBMatrix[j][i];
			NormVector = (double) Math.sqrt(NormVector);

			for (int j = i; j < rows; j++)
				tempVector[j] = newBMatrix[j][i];

			if (tempVector[i] < 0) {
				newBMatrix[i][i] = NormVector;
				tempVector[i] -= NormVector;
			} else {
				newBMatrix[i][i] = -NormVector;
				tempVector[i] += NormVector;
			}

			// set the present column to 0
			for (int j = i + 1; j < rows; j++)
				newBMatrix[j][i] = 0;

			v1 = 0;
			for (int j = i; j < rows; j++)
				v1 += tempVector[j] * tempVector[j];

			v1 = 2 / v1;

			for (int k = i + 1; k < cols; k++) {
				v2 = 0;
				for (int j = i; j < rows; j++)
					v2 += tempVector[j] * newBMatrix[j][k];
				v2 *= v1;
				for (int j = i; j < rows; j++)
					newBMatrix[j][k] -= v2 * tempVector[j];
			}
		}

		/*
		 * By choosing the largest pivot element in each row, we will form our
		 * new basis for the next phase.
		 */

		for (int i = basisSize; i < rows; i++) {

			// the greatest pivot in the row i defines the column
			v1 = Math.abs(newBMatrix[i][i]);
			high = i;
			for (int j = i + 1; j < cols; j++) {
				if (Math.abs(newBMatrix[i][j]) > v1) {
					v1 = Math.abs(newBMatrix[i][j]);
					high = j;
				}
			}
			// swap column high with column i
			temp = permanentBasis[i];
			permanentBasis[i] = permanentBasis[high];
			permanentBasis[high] = temp;

			for (int j = 0; j < rows; j++) {
				dtemp = newBMatrix[j][i];
				newBMatrix[j][i] = newBMatrix[j][high];
				newBMatrix[j][high] = dtemp;
			}

			if (i < rows - 1) {
				NormVector = 0;
				for (int j = i; j < rows; j++)
					NormVector += newBMatrix[j][i] * newBMatrix[j][i];

				NormVector = (double) Math.sqrt(NormVector);

				for (int j = i; j < rows; j++)
					tempVector[j] = newBMatrix[j][i];

				if (tempVector[i] < 0) {
					newBMatrix[i][i] = NormVector;
					tempVector[i] -= NormVector;
				} else {
					newBMatrix[i][i] = -NormVector;
					tempVector[i] += NormVector;
				}

				for (int j = i + 1; j < rows; j++)
					newBMatrix[j][i] = 0;

				v1 = 0;
				for (int j = i; j < rows; j++)
					v1 += tempVector[j] * tempVector[j];
				v1 = 2 / v1;

				for (int k = i + 1; k < cols; k++) {
					v2 = 0;
					for (int j = i; j < rows; j++)
						v2 += tempVector[j] * newBMatrix[j][k];
					v2 *= v1;
					for (int j = i; j < rows; j++)
						newBMatrix[j][k] -= v2 * tempVector[j];
				}
			}
		}

		return 0;
	}

	/**
	 * Eliminates artificial variables after the augmented matrix is created.
	 */
	public void eliminateArtificials() {
		int basicIndex = 0;
		int nonbasicIndex = 0;
		int artificialCount = 0;
		int basicArtificialsCount = 0;
		double[] temp = new double[numVariables]; // for swap
		int[] varType = new int[numVariables]; // 1 = basic / 2 = non-basic

		for (int i = 0; i < numNonbasic; i++)
			varType[NonBasicVariables[i]] = 2; // non basic

		for (int i = 0; i < numConstraints; i++) {
			temp[BasicVariables[i]] = x[i];
			varType[BasicVariables[i]] = 1; // basic
		}

		for (int i = 0; i < numVariables; i++)
			if (varType[i] != 2) { // if not artificial
				switch (varType[i]) {
				case 1: // basic
					BasicVariables[basicIndex] = i;
					x[basicIndex] = temp[i];
					basicIndex++;
					break;
				case 2: // non-basic
					NonBasicVariables[nonbasicIndex] = i;
					nonbasicIndex++;
					break;
				default:
					System.out.println("There are only two types of variables: basic or non-basic");
				}
			} else
				artificialCount++;

		for (int i = 0; i < numVariables; i++)
			if (varType[i] == 2) { // artificial
				switch (varType[i]) {
				case 1: // basic
					basicArtificialsCount++;
					BasicVariables[basicIndex] = i;
					x[basicIndex] = temp[i];
					basicIndex++;
					break;
				case 2:// non basic
					NonBasicVariables[nonbasicIndex] = i;
					nonbasicIndex++;
					break;
				default:
					System.out.println("There are only two types of variables: basic or non-basic");
				}
			}

		// artificial variable test

		if (basicArtificialsCount > 0) {
			AugmentBasis(numConstraints - basicArtificialsCount);

			// update everything

			for (int i = 0; i < numVariables; i++)
				varType[i] = 2; // the rest is non-basic type

			for (int i = 0; i < numConstraints; i++)
				BasicVariables[i] = permanentBasis[i];

			for (int i = 0; i < numConstraints; i++)
				varType[BasicVariables[i]] = 1; // basic

			basicIndex = 0;
			nonbasicIndex = 0;

			for (int i = 0; i < numVariables; i++)
				switch (varType[i]) {
				case 1: // basic
					if (varType[i] == 2) { // artificial
						System.out.println("Some artificial variables are still here to eliminate");
					}
					BasicVariables[basicIndex] = i;
					x[basicIndex] = temp[i];
					basicIndex++;
					break;
				case 2: // non basic
					NonBasicVariables[nonbasicIndex] = i;
					nonbasicIndex++;
					break;
				default:
					System.out.println("There are only two types of variables: basic or non-basic");
				}
		}

		// now the all the artificial variables are eliminated, so the phase 2
		// can start.
		this.optimizationType(originalCost, oldOptimizationType);
		this.isMinimize = oldOptimizationType;
		ArtificialAdded = false;
		numNonbasic -= artificialCount;
		numVariables -= artificialCount;
		currentStep = 0;

	}

}
