package RevisedSimplex.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import RevisedSimplex.util.RevisedSimplex;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The page of the GUI where the RevisedSimplex where the problem is stepped and 
 * iterated through, displayed, and solved.
 * 
 * @author Berk Kaan Kuguoglu
 */
public class RevisedSimplexGUI extends Application{
	RevisedSimplex revisedSimplex;
	double[] varCoefficients;
	
	//GUI elements to globally update
	VBox Pi;
	VBox yB;
	VBox cB;
	RadioButton[] reducedCosts;
	Label[] reducedCostLabels;
	VBox xMatrixBox;
	Label objectiveValueLabel;
	int solveStatus = 0;
	GridPane bMatrix;
	VBox messageBox;

	/**
	 * Constructor for storing all relevant data to the RevisedSimplex to solve
	 * 
	 * @param revisedSimplex The RevisedSimplex problem to solve.
	 * @param varCoefficients The variable coefficients.
	 */
	public RevisedSimplexGUI(RevisedSimplex revisedSimplex, double[] varCoefficients) {
		this.revisedSimplex  = revisedSimplex;
		this.varCoefficients = varCoefficients;
	}


	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setTop(makeHeader());
		root.setLeft(makeLeftPane());
		root.setRight(makeRightPane(primaryStage));
		root.setPadding(new Insets(25));
		root.setStyle("-fx-border-color: grey; -fx-border-width: 5px;"); 
		
		primaryStage.setScene(new Scene(root, 	270 + (50*revisedSimplex.numVariables) + (60*revisedSimplex.numConstraints), 445 + (35*revisedSimplex.numConstraints))); //used to resize the Stage to accommodate for the number of variables and constraints.
		primaryStage.getIcons().add(loadIcon());
		primaryStage.setTitle("Revised Simplex");
		primaryStage.show();
		
	}

	/**
	 * Creates the left pane of the GUI, which is used for displaying the objective, 
	 * the constraint matrix, the X matrix, the reduced costs, and the current objective value.
	 * 
	 * @return node the created node
	 */
	private Node makeLeftPane() {
		VBox root = new VBox(10);
		root.setPadding(new Insets(20));
		root.setStyle("-fx-border-color: grey; -fx-border-width: 2px"); 
		
		VBox overview = new VBox(8);
		Label objective = new Label("Objective: " + (revisedSimplex.isMinimize? "Min":"Max"));
		//unicode codes for subscripts 1-22
		String[] subscripts = {"\u2081","\u2082","\u2083","\u2084","\u2085","\u2086","\u2087", "\u2088", "\u2089", "\u2081\u2080", "\u2081\u2081", "\u2081\u2082", "\u2081\u2083", "\u2081\u2084", "\u2081\u2085", "\u2081\u2086", "\u2081\u2087", "\u2081\u2088", "\u2081\u2089", "\u2082\u2080", "\u2082\u2081", "\u2082\u2082"};
		String problemString = "";
		for(int i = 0; i < varCoefficients.length; ++i){
			problemString += " " + (varCoefficients[i]<0? "- ": (i==0? " ":"+ ") ) + Math.abs(varCoefficients[i]) + "X" + subscripts[i];
		}
		Label problem = new Label(problemString);
		
		Label preprocessedObjective = new Label("Initial Objective Function: ");
		String objectiveString = "";
		for(int i =0; i < revisedSimplex.numVariables; ++i){
			objectiveString += " " + (revisedSimplex.cost[i]<0? "- ": (i==0? " ":"+ ") ) + Math.abs(revisedSimplex.cost[i]) + "X" + subscripts[i];
		}
		Label objectiveLabel = new Label(objectiveString);
		
		overview.getChildren().addAll(objective, problem, preprocessedObjective, objectiveLabel);
		overview.setPadding(new Insets(9));
		overview.setStyle("-fx-border-color: black; -fx-border-width: 1px"); 
		
		
		HBox constraintEquations = new HBox();
		
		VBox constraintMatrixBox = new VBox();
		Label constraintLabel = new Label("Constraint Matrix: (A)");
		GridPane constraintMatrix = new GridPane();
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			for(int j = 0; j < revisedSimplex.numVariables; ++j){
				constraintMatrix.add(new Label("" + revisedSimplex.A[i][j] + "\t"), j, i);
			}
		}
		constraintMatrixBox.getChildren().addAll(constraintLabel, constraintMatrix);
		constraintMatrixBox.setStyle("-fx-border-color: black; -fx-border-width: 1px; -fx-border-style: hidden solid hidden hidden"); 
		
		VBox rhs = new VBox();
		rhs.getChildren().add(new Label(" RHS:")); 
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			rhs.getChildren().add(new Label(" " + revisedSimplex.b[i]));
		}
		
		constraintEquations.getChildren().addAll(constraintMatrixBox, rhs);
		constraintEquations.setPadding(new Insets(9));
		constraintEquations.setStyle("-fx-border-color: black; -fx-border-width: 1px"); 
		
		GridPane reducedCostPane = new GridPane();
		reducedCosts = new RadioButton[revisedSimplex.numVariables];
		reducedCostLabels = new Label[revisedSimplex.numVariables+1];
		ToggleGroup reducedCostsToggle = new ToggleGroup();
		for(int i = 0; i < revisedSimplex.numVariables; ++i){
			reducedCosts[i] = new RadioButton("\t");
			reducedCosts[i].setToggleGroup(reducedCostsToggle);
			reducedCosts[i].setDisable(true);
			reducedCostPane.add(new Label("X" + subscripts[i]), i, 0);
			reducedCostPane.add(reducedCosts[i], i, 1);
			
		}
		for(int i = 0; i < revisedSimplex.numNonbasic; ++i){
			reducedCostLabels[revisedSimplex.NonBasicVariables[i]] = new Label("" + revisedSimplex.reducedCost[i]);
			reducedCostPane.add(reducedCostLabels[revisedSimplex.NonBasicVariables[i]], revisedSimplex.NonBasicVariables[i], 3);
		}
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			reducedCostLabels[revisedSimplex.BasicVariables[i]] = new Label("Basic");
			reducedCostPane.add(reducedCostLabels[revisedSimplex.BasicVariables[i]], revisedSimplex.BasicVariables[i], 3);
		}
		
		reducedCostPane.setPadding(new Insets(4));
		reducedCostPane.setStyle("-fx-border-color: grey; -fx-border-width: 1px");
		
		xMatrixBox = new VBox();
		xMatrixBox.getChildren().add(new Label("X:"));
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			xMatrixBox.getChildren().add(new Label("  X" + subscripts[revisedSimplex.BasicVariables[i]] + " = " + revisedSimplex.x[i] + "\t"));
			
		}
		xMatrixBox.setPadding(new Insets(9));
		xMatrixBox.setStyle("-fx-border-color: black; -fx-border-width: 1px"); 
		
		objectiveValueLabel = new Label("Current Objective Value: ");
		root.getChildren().addAll(overview, constraintEquations, xMatrixBox, reducedCostPane, objectiveValueLabel);
		root.setPadding(new Insets(20));
		
		return root;
	}


	/**
	 * Created the right pane of the GUI, which is used for displaying the B matrix, yB, cB, and Pi, 
	 * along with the scrolling pane of messages and buttons for controlling the process of 
	 * stepping and/or iterating through the revised simplex.
	 * 
	 * @param primaryStage The primaryStage of the application
	 * @return the created node
	 */
	private Node makeRightPane(Stage primaryStage) {
		VBox root = new VBox(10);
		root.setAlignment(Pos.CENTER_RIGHT);
		root.setPadding(new Insets(20));
		root.setStyle("-fx-border-color: grey; -fx-border-width: 2px"); 
		
		VBox bMatrixBox = new VBox(3);
		bMatrix = new GridPane();
		Label bLabel = new Label("B Matrix");
		bLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px; -fx-border-style: hidden hidden solid hidden;"); 
		bMatrixBox.getChildren().addAll(bLabel, bMatrix);
		Label[][] bMatrixLabels = new Label[revisedSimplex.numConstraints][revisedSimplex.numConstraints];
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			for (int j = 0; j < revisedSimplex.numConstraints; j++) {
				bMatrixLabels[i][j] = new Label(""+0);
				bMatrixLabels[i][j].setPrefSize(60, 20);
				bMatrix.add(bMatrixLabels[i][j], i, j);
			}
		}
		bMatrixBox.setPadding(new Insets(4));
		bMatrixBox.setStyle("-fx-border-color: grey; -fx-border-width: 1px"); 
		
		HBox moreBoxes = new HBox();//contains the three boxes for yB, cB, and Pi
		
		yB = new VBox();
		Label yBLabel = new Label("yB");
		yB.getChildren().add(yBLabel);
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			yB.getChildren().add(new Label("0"));
		}
		yB.setPadding(new Insets(4));
		yB.setStyle("-fx-border-color: grey; -fx-border-width: 1px"); 

		cB = new VBox();
		Label cBLabel = new Label("cB");
		cB.getChildren().add(cBLabel);
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			cB.getChildren().add(new Label("0"));
		}
		cB.setPadding(new Insets(4));
		cB.setStyle("-fx-border-color: grey; -fx-border-width: 1px"); 
		
		Pi = new VBox();
		Pi.getChildren().add(new Label("Pi"));
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			Pi.getChildren().add(new Label("0"));
		}
		Pi.setPadding(new Insets(4));
		Pi.setStyle("-fx-border-color: grey; -fx-border-width: 1px"); 
		
		yB.setPrefWidth(50);
		cB.setPrefWidth(50);
		Pi.setPrefWidth(50);
		
		moreBoxes.getChildren().addAll(yB, cB, Pi);
		moreBoxes.setPrefWidth(200);
		moreBoxes.setPadding(new Insets(8));
		moreBoxes.setStyle("-fx-border-color: grey; -fx-border-width: 2px"); 
		moreBoxes.setAlignment(Pos.CENTER);
		moreBoxes.setSpacing(30);
		
		ScrollPane messages = new ScrollPane();
		messageBox = new VBox();
		messageBox.getChildren().add(new Label("Ready!"));
		messages.setContent(messageBox);
		messageBox.heightProperty().addListener(new ChangeListener<Number>() { // scrolls the message pane down every time a new message is added so as to always display the most recently added message
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				messages.setVvalue(1);
			}
		});
		
		//unicode codes for subscripts between 1 and 22
		String[] subscripts = {"\u2081","\u2082","\u2083","\u2084","\u2085","\u2086",
								"\u2087", "\u2088", "\u2089", "\u2081\u2080", "\u2081\u2081",
								"\u2081\u2082", "\u2081\u2083", "\u2081\u2084", "\u2081\u2085", 
								"\u2081\u2086", "\u2081\u2087", "\u2081\u2088", "\u2081\u2089", 
								"\u2082\u2080", "\u2082\u2081", "\u2082\u2082"};
		VBox controls = new VBox(10);
		Button step = new Button("Next");
		Button iterate = new Button("Full Iteration");
		
		step.setOnAction((event) -> { //lambda expression for controlling what happens when the step button is clicked on
			solveStatus = revisedSimplex.iterateOneStep();
			iterate.setDisable(true);
			switch(revisedSimplex.currentStep){
				case 1:
					for(int i = 0; i < revisedSimplex.numConstraints; ++i){
						for(int j = 0; j < revisedSimplex.numConstraints; ++j){
							((Label)getNodeByCoordinate(bMatrix, i, j)).setText("" + revisedSimplex.A[j][revisedSimplex.BasicVariables[i]] + "\t");
						}
					}
					messageBox.getChildren().add(new Label("Updated the B Matrix."));
					break;
					
				case 2:
					Pi.getChildren().clear();
					Pi.getChildren().add(new Label("Pi"));
					for(int i = 0; i < revisedSimplex.numConstraints; ++i){
						Pi.getChildren().add(new Label("" + revisedSimplex.pi[i])); // add decimal format?
					}
					messageBox.getChildren().add(new Label("Updated B^T pi = cB."));
					break;
					
				case 3:
					for(int i = 0; i < revisedSimplex.numNonbasic; ++i){
						reducedCostLabels[revisedSimplex.NonBasicVariables[i]].setText("" + revisedSimplex.reducedCost[i]);
					}
					for(int i = 0; i < revisedSimplex.numConstraints; ++i){
						reducedCostLabels[revisedSimplex.BasicVariables[i]].setText("Basic");
					}
					
					messageBox.getChildren().add(new Label("Reduced costs are calculated \nCost(reduced) = ci−(cB)(B^-1)A.\nTest for Optimality."));
					break; 
				case 4:// user selects entering variable thing
					messageBox.getChildren().add(new Label("The solution is not optimal!\nInsert the entering Variable"));
					for(int i = 0; i <revisedSimplex.numNonbasic; ++i){
						reducedCosts[revisedSimplex.NonBasicVariables[i]].setDisable(!(revisedSimplex.reducedCost[i] < 0));
					}
					reducedCosts[revisedSimplex.NonBasicVariables[revisedSimplex.enteringVar]].setSelected(true);
					
					for(int i = 0; i < revisedSimplex.numConstraints; ++i){
						reducedCosts[revisedSimplex.BasicVariables[i]].setSelected(false);
						reducedCosts[revisedSimplex.BasicVariables[i]].setDisable(true);
					}
					break;
					
				case 5:
					for(int i = 0; i < revisedSimplex.numNonbasic; ++i){
						if(reducedCosts[i].isSelected()){
							revisedSimplex.enteringVar = i;
						}
						reducedCosts[i].setDisable(true);
					}
					messageBox.getChildren().add(new Label("The entering variable is x" + subscripts[revisedSimplex.NonBasicVariables[revisedSimplex.enteringVar]] ));
					break;
					
				case 6:
					yB.getChildren().clear();
					yB.getChildren().add(new Label("yB"));
					for(int i = 0; i < revisedSimplex.numConstraints; ++i){
						yB.getChildren().add(new Label("" + revisedSimplex.yB[i]));
					}
					messageBox.getChildren().add(new Label("yB is updated.\nNow, Test for unboundedness"));
					break;

				case 7:
					messageBox.getChildren().add(new Label("The problem is unbounded"));
					break;
				case 8:
					messageBox.getChildren().add(new Label("The  problem is bounded.\n The method goes on."));
					break;
					
				case 9:
					messageBox.getChildren().add(new Label("Min Ratio Test"));
					int index;
					for(int i = 0; i < revisedSimplex.numConstraints; ++i){
						index = revisedSimplex.BasicVariables[i];
						if(revisedSimplex.yB[i] > 0 && revisedSimplex.x[i]/revisedSimplex.yB[i] == revisedSimplex.MinRatio){
							reducedCosts[index].setSelected(true);
							reducedCosts[index].setText("" + revisedSimplex.MinRatio);
						} else {
							reducedCosts[index].setSelected(false);
							reducedCosts[index].setText("" + revisedSimplex.MinRatio);
						}
					}
					
					index = revisedSimplex.BasicVariables[revisedSimplex.leavingVar];
					reducedCosts[index].setSelected(true);
					
					for(int i = 0; i < revisedSimplex.numNonbasic; ++i){
						index = revisedSimplex.NonBasicVariables[i];
						reducedCosts[index].setSelected(false);
						reducedCosts[index].setDisable(true);
						reducedCosts[index].setText("\t");
						
					}
					
					messageBox.getChildren().add(new Label("There are multiple variables have the min ratio.\nPick the leaving variable."));
					break;
					
				case 10:
					for(int i = 0; i < revisedSimplex.numConstraints; ++i){
						if(reducedCosts[i].isSelected()){
							revisedSimplex.leavingVar = i;
						}
						reducedCosts[i].setSelected(false);
						reducedCosts[i].setDisable(true);
					}
					messageBox.getChildren().add(new Label("The leaving variable is X" + subscripts[revisedSimplex.BasicVariables[revisedSimplex.leavingVar]]));
					break;
					
				case 11:
					messageBox.getChildren().add(new Label("The minumum ratio test indicates X" + subscripts[revisedSimplex.BasicVariables[revisedSimplex.leavingVar]]+"\nshould leave the basis."));
					break;
					
				case 12:
					messageBox.getChildren().add(new Label("Solution is found.\nxB and the objective value Z are updated."));
					break;
					
				case 0:
					objectiveValueLabel.setText("Current Objective Value Z = : " + revisedSimplex.calculateObjective());
					
					xMatrixBox.getChildren().clear();
					xMatrixBox.getChildren().add(new Label("X:"));
					cB.getChildren().clear();
					cB.getChildren().add(new Label("cB:"));
					for(int i = 0; i <revisedSimplex.numConstraints; ++i){
						xMatrixBox.getChildren().add(new Label("X" + subscripts[revisedSimplex.BasicVariables[i]] + " = " + revisedSimplex.x[i]));
						cB.getChildren().add(new Label("" + revisedSimplex.cost[revisedSimplex.BasicVariables[i]]));
					}
					
					messageBox.getChildren().add(new Label("The basis xB and the objection value Z are updated."));
					break;
			}//end switch
			
			if(revisedSimplex.currentStep == 0){
				iterate.setDisable(false);
			}
			if(solveStatus == 3){ // unbounded
				messageBox.getChildren().add(new Label("The problem is unbounded."));
				step.setDisable(true);
				iterate.setDisable(true);
			} else if (solveStatus == 1){ // if optimal
				if(revisedSimplex.ArtificialAdded == false){
					messageBox.getChildren().add(new Label("The problem is solved!"));
					updateAllPanels();
					iterate.setDisable(true);
					step.setDisable(true);
				} else {
					if(revisedSimplex.calculateObjective() == 0){
						revisedSimplex.eliminateArtificials();
						messageBox.getChildren().add(new Label("Artificial variables are being eliminated"));
						Stage newStage = new Stage();
						RevisedSimplexGUI phaseTwo = new RevisedSimplexGUI(revisedSimplex, this.varCoefficients);
						phaseTwo.start(newStage);
						newStage.setTitle("Revised Simplex - Phase Two");
						primaryStage.close();
					} else {
						messageBox.getChildren().add(new Label("The problem is infeasible."));
						iterate.setDisable(true);
						step.setDisable(true);
					}
				}
			}
			
		});
		iterate.setOnAction((event) -> { //lambda expression for controlling what happens when the iterate button is clicked on
			messageBox.getChildren().add(new Label("Keep iterating."));
			int solveStatus = revisedSimplex.iterate();
			updateAllPanels();
			
			if(solveStatus == 3){ // unbounded
				messageBox.getChildren().add(new Label("The problem is unbounded."));
				step.setDisable(true);
				iterate.setDisable(true);
			} else if (solveStatus == 1){ // optimal
				if(revisedSimplex.ArtificialAdded == false){
					messageBox.getChildren().add(new Label("The problem is solved!"));
					updateAllPanels();
					iterate.setDisable(true);
					step.setDisable(true);
				} else {
					if(revisedSimplex.calculateObjective() == 0){
						revisedSimplex.eliminateArtificials();
						messageBox.getChildren().add(new Label("Artificial variables are being eliminated"));
						Stage newStage = new Stage();
						RevisedSimplexGUI phaseTwo = new RevisedSimplexGUI(revisedSimplex, this.varCoefficients);
						phaseTwo.start(newStage);
						newStage.setTitle("Revised Simplex - Phase Two");
						primaryStage.close();
					} else {
						messageBox.getChildren().add(new Label("The problem is infeasible."));
						iterate.setDisable(true);
						step.setDisable(true);
					}
				}
			}
		});
		
		step.setPrefWidth(150);
		iterate.setPrefWidth(150);
		
		Button quit = new Button("Quit");
		quit.setPrefWidth(150);
		quit.setOnAction((event) -> {
			primaryStage.close(); 
		});
		controls.getChildren().addAll(step, iterate, quit);
		controls.setPadding(new Insets(4));
		controls.setStyle("-fx-border-color: grey; -fx-border-width: 1px"); 
		
		root.getChildren().addAll(bMatrixBox, moreBoxes, messages, controls);
		
		return root;
	}

	/**
	 * Updates the entire display to show the current state of all the displayable data.
	 * 
	 */
	private void updateAllPanels() {
		String[] subscripts = {"\u2081","\u2082","\u2083","\u2084","\u2085","\u2086","\u2087", "\u2088", "\u2089", "\u2081\u2080", "\u2081\u2081", "\u2081\u2082", "\u2081\u2083", "\u2081\u2084", "\u2081\u2085", "\u2081\u2086", "\u2081\u2087", "\u2081\u2088", "\u2081\u2089", "\u2082\u2080", "\u2082\u2081", "\u2082\u2082"};
		
		for(int i = 0; i < revisedSimplex.numNonbasic; ++i){
			reducedCostLabels[revisedSimplex.NonBasicVariables[i]].setText("" + revisedSimplex.reducedCost[i]);
		}
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			reducedCostLabels[revisedSimplex.BasicVariables[i]].setText("Basic");
		}
		
		xMatrixBox.getChildren().clear();
		xMatrixBox.getChildren().add(new Label("X:"));
		for(int i = 0; i <revisedSimplex.numConstraints; ++i){
			xMatrixBox.getChildren().add(new Label("X" + subscripts[revisedSimplex.BasicVariables[i]] + " = " + revisedSimplex.x[i]));
		}
		
		Pi.getChildren().clear();
		Pi.getChildren().add(new Label("Pi"));
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			Pi.getChildren().add(new Label("" + revisedSimplex.pi[i])); 
		}
		
		cB.getChildren().clear();
		cB.getChildren().add(new Label("cB:"));
		for(int i = 0; i <revisedSimplex.numConstraints; ++i){
			cB.getChildren().add(new Label("" + revisedSimplex.cost[revisedSimplex.BasicVariables[i]]));
		}
		
		yB.getChildren().clear();
		yB.getChildren().add(new Label("yB"));
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			yB.getChildren().add(new Label("" + revisedSimplex.yB[i]));
		}
		
		for(int i = 0; i < revisedSimplex.numConstraints; ++i){
			for(int j = 0; j < revisedSimplex.numConstraints; ++j){
				((Label)getNodeByCoordinate(bMatrix, i, j)).setText("" + revisedSimplex.A[j][revisedSimplex.BasicVariables[i]] + "\t");
			}
		}
		
		objectiveValueLabel.setText("Current Objective Value: " + revisedSimplex.calculateObjective());
	}

	/**
	 * Creates the header for the application.
	 * 
	 * @return the created node.
	 */
	private Node makeHeader() {
		VBox header =new VBox(10);
		Label title = new Label("Revised Simplex Solver");
		title.setScaleX(3);
		title.setScaleY(3);
		title.setPadding(new Insets(10));
		header.getChildren().addAll(title);
		header.setAlignment(Pos.CENTER);
		header.setPadding(new Insets(0, 0, 20, 0));
		return header;
	}
	
	/**
	 * Utility function to locate a node in a GridPane by a particular pair of coordinates
	 * 
	 * @param x X coordinate of desired Node
	 * @param y Y coordinate of desired Node
	 * @return Returns the desired Node
	 */
	private Node getNodeByCoordinate(GridPane gridpane, int x, int y){ //there has to be a better way to do this
		Node temp = null;
		for(Node node : gridpane.getChildren()){ 
			int nodeX = GridPane.getColumnIndex(node);
			int nodeY = GridPane.getRowIndex(node);
			if(x == nodeX && y == nodeY){
				temp = node;
				break;
			}
		}
		return temp;
	}

	/**
	 * Loads the icon used to represent the application.
	 * 
	 * @return The specified icon.
	 */
	
	private Image loadIcon(){
		String separator  = File.separator; //for multiplatform support
		File iconFile     = new File("src" + separator + "RevisedSimplex" + separator + "gui" + separator + "icon.png");
		InputStream input = null;
		Image icon        = null;
		try{
			input = new FileInputStream(iconFile);
			icon  = new Image(input);
			input.close();
		}catch(FileNotFoundException e){
			
		}catch(IOException e){
			
		}
		return icon;
	}
}
