package RevisedSimplex.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParsePosition;

import RevisedSimplex.util.RevisedSimplex;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextFormatter;

/**
 * The page of the GUI where the number of variables and constraints is specified, then entered.
 * 
 * @author Berk Kaan Kuguoglu
 */
public class DataEntryScreen extends Application{
	//arbitrary defaults
	boolean isMinimize = true;
	int numOfVars = 2;
	int numOfConstraints = 2;
	
	TextField[]   varCoefficientFields;
	TextField[][] conCoefficientFields;
	TextField[]   rhs;
	int[]         constraintType;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane root = new BorderPane();
		BorderPane topFrame = new BorderPane();
		
		String cssBordering = "-fx-border-color:gray ; \n" //#090a0c
							+ "-fx-border-width:7.5";
	    Image img = loadImage();
	    ImageView imgView = new ImageView(img);
		topFrame.setCenter(imgView);
		topFrame.setStyle(cssBordering);
	    root.setTop(topFrame);

		root.setCenter(makeBody(primaryStage));
		
		Scene scene = new Scene(root, 720, 560);
		primaryStage.setScene(scene);
		primaryStage.getIcons().add(loadIcon());
		primaryStage.setTitle("Revised Simplex Solver by Berk Kaan Kuguoglu");
		
		
		
	
		
		primaryStage.show();
	}

	/**
	 * Creates the main body panel of the application and populates it with elements used in specifying the revised simplex variables and constraints
	 * 
	 * @param The primaryStage of the application
	 * @return The main body panel of the application
	 */
	private Node makeBody(Stage primaryStage) {
		VBox root = new VBox(20);
		
		HBox objective = new HBox(5);
		Label objctiveLabel = new Label("Objective: ");
		RadioButton min = new RadioButton("Minimize");
		min.setOnAction((event) -> {
			isMinimize = true;
		});
		RadioButton max = new RadioButton("Maximize");
		max.setOnAction((event) -> {
			isMinimize = false;
		});
		ToggleGroup objectiveToggle = new ToggleGroup();
		min.setToggleGroup(objectiveToggle);
		max.setToggleGroup(objectiveToggle);
		objectiveToggle.selectToggle(min); //default to minimize
		objective.setPadding(new Insets(4));
		objective.setStyle("-fx-border-color: black; -fx-border-width: 1px; -fx-border-style: hidden hidden solid hidden;"); 
		objective.getChildren().addAll(objctiveLabel, min, max);
		
		VBox varsAndConstraints = new VBox(5);
		
		HBox varNumBox = new HBox();
		Label varNumLabel = new Label("Number of Variables:\t");
		varNumLabel.setPadding(new Insets(3));
		ChoiceBox<String> varNumSelector = new ChoiceBox<String>();
		varNumSelector.getItems().addAll("2", "3", "4", "5", "6");
		varNumSelector.getSelectionModel().select(numOfVars - 2);
		varNumBox.getChildren().addAll(varNumLabel, varNumSelector);

		HBox conNumBox = new HBox();
		Label conNumLabel = new Label("Number of Constraints:\t");
		conNumLabel.setPadding(new Insets(3));
		ChoiceBox<String> conNumSelector = new ChoiceBox<String>();
		conNumSelector.getItems().addAll("2", "3", "4", "5", "6");
		conNumSelector.getSelectionModel().select(numOfConstraints - 2);
		conNumBox.getChildren().addAll(conNumLabel, conNumSelector);
		varsAndConstraints.setPadding(new Insets(8));
		varsAndConstraints.setStyle("-fx-border-color: grey; -fx-border-width: 2px"); 
		varsAndConstraints.getChildren().addAll(varNumBox, conNumBox);
		
		BorderPane dataEntryPane = new BorderPane();
		dataEntryPane.setCenter(makeDataEntryPane());
		varNumSelector.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {  //lambda expression for the dynamic resizing of the variables
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				numOfVars = newValue.intValue()+2; //newValue.intValue is the index of the newly selected item
				dataEntryPane.setCenter(makeDataEntryPane());
			}
		});
		conNumSelector.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {  //lambda expression for the resizing of the constraints
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				numOfConstraints = newValue.intValue()+2; //newValue.intValue is the index of the newly selected item
				dataEntryPane.setCenter(makeDataEntryPane());
			}
		});
		
		Button accept = new Button("Solve");
		accept.setOnAction( (event) -> {
			try{
				RevisedSimplex revisedSimplex = new RevisedSimplex(numOfVars, numOfConstraints);
				revisedSimplex.isMinimize = isMinimize;
				readDataAndPreprocess(revisedSimplex);
				
				double[] varCoefficients = new double[numOfVars];
				for(int i = 0; i < numOfVars; ++i){
					varCoefficients[i] = Double.parseDouble(varCoefficientFields[i].getText());
				}
				
				//creating and launching the next Stage of the GUI
				RevisedSimplexGUI application = new RevisedSimplexGUI(revisedSimplex, varCoefficients);
				Stage applicationStage = new Stage();
				application.start(applicationStage);
			}catch(NumberFormatException e){
				dataEntryPane.setBottom(new Label("Form incomplete."));
			}
		});
		
		root.setStyle("-fx-border-color: grey; -fx-border-width: 5px"); 
		root.setAlignment(Pos.TOP_CENTER);
		root.getChildren().addAll(objective, varsAndConstraints, dataEntryPane, accept);
		root.setPadding(new Insets(25));
		return root;
	}
	
	/**
	 * Reads the variables and constraints entered into the application, and applies them to the RevisedSimplex
	 * 
	 * @param revisedSimplex The RevisedSimplex to apply the variables and constraints to.
	 */
	private void readDataAndPreprocess(RevisedSimplex revisedSimplex) {
		double[]   varCoefficients = new double[numOfVars];
		double[][] conCoefficients = new double[numOfConstraints][numOfVars];
		double[]   rhs             = new double[numOfConstraints];
		
		//store all the data
		for(int i = 0; i < numOfVars; ++i){
			varCoefficients[i] = Double.parseDouble(varCoefficientFields[i].getText());
			for (int j = 0; j < numOfConstraints; ++j){
				conCoefficients[j][i] = Double.parseDouble(conCoefficientFields[j][i].getText());
				if(i==0){//loop once
					rhs[j] = Double.parseDouble(this.rhs[j].getText());
				}
			}
		}
		for(int i = 0; i<numOfConstraints; ++i){
			revisedSimplex.addConstraint(conCoefficients[i], rhs[i], constraintType[i]);
		}
		revisedSimplex.optimizationType(varCoefficients, isMinimize);
		revisedSimplex.initialize(numOfVars, numOfConstraints);
	}


	/**
	 * Creates the javafx node for entering constraint data
	 * 
	 * @return The node for entering constraint data
	 */
	private Node makeDataEntryPane() {
		VBox root = new VBox(5);
		
		HBox variables = new HBox(3);
		varCoefficientFields = new TextField[numOfVars];
		String[] subscripts = {"\u2081","\u2082","\u2083","\u2084","\u2085","\u2086","\u2087"}; //unicode codes for subscripts 1 - 7
		DecimalFormat format = new DecimalFormat( "#.0");
		for(int i = 0; i < numOfVars; ++i){
			varCoefficientFields[i] = new TextField();
			varCoefficientFields[i].setPrefWidth(40);
			varCoefficientFields[i].setTextFormatter(new TextFormatter<String>(c ->{ //lambda expression for preventing the entry of non-numbers into the TextFields
				if(c.getControlNewText().isEmpty()){
			        return c;
			    }
				if(c.getControlNewText().equals("-")){
					return c;
				}
				ParsePosition parsePosition = new ParsePosition(0);
			    Object object = format.parse( c.getControlNewText(), parsePosition );
			    
			    if(object == null || parsePosition.getIndex() < c.getControlNewText().length()){
			        return null;
			    }else{
			        return c;
			    }
			}));
			
			Label varLabel = new Label("X" + subscripts[i] + ((i==numOfVars-1)? "":" + "));
			varLabel.setPadding(new Insets(3));
			variables.getChildren().addAll(varCoefficientFields[i], varLabel);
		}
		variables.setPadding(new Insets(9));
		variables.setStyle("-fx-border-color: black; -fx-border-width: 1px; -fx-border-style: hidden hidden solid hidden;"); 
		
		VBox constraints = new VBox(4);
		conCoefficientFields = new TextField[numOfConstraints][numOfVars];
		rhs = new TextField[numOfConstraints];
		constraintType = new int[numOfConstraints];
		HBox[] constraintRows = new HBox[numOfConstraints];
		for(int i = 0; i < numOfConstraints; ++i){
			constraintRows[i] = new HBox(3);
			
			for(int j = 0; j < numOfVars; ++j){
				conCoefficientFields[i][j] = new TextField();
				conCoefficientFields[i][j].setPrefWidth(40);
				conCoefficientFields[i][j].setTextFormatter(new TextFormatter<String>(c ->{//lambda expression for preventing the entry of non-numbers into the TextFields
					if(c.getControlNewText().isEmpty()){
				        return c;
				    }
					if(c.getControlNewText().equals("-")){
						return c;
					}
					ParsePosition parsePosition = new ParsePosition(0);
				    Object object = format.parse( c.getControlNewText(), parsePosition );

				    if(object == null || parsePosition.getIndex() < c.getControlNewText().length()){
				        return null;
				    }else{
				        return c;
				    }
				}));
				
				Label conLabel = new Label("X" + subscripts[j] + ((j==numOfVars-1)? "":" + "));
				conLabel.setPadding(new Insets(3));
				
				constraintRows[i].getChildren().addAll(conCoefficientFields[i][j], conLabel);
			}

			ChoiceBox<String> operator = new ChoiceBox<String>();
			operator.getItems().addAll("\u2264", "\u2265", "="); //unicode codes for greater than and less than
			operator.getSelectionModel().select(0);
			constraintType[i] = 0; //because default values
			operator.getSelectionModel().selectedIndexProperty().addListener(new ChoiceChangeListener(i));
			
			
			rhs[i] = new TextField();
			rhs[i].setTextFormatter(new TextFormatter<String>(c ->{//lambda expression for preventing the entry of non-numbers into the TextFields
				if(c.getControlNewText().isEmpty()){
			        return c;
			    }
				if(c.getControlNewText().equals("-")){
					return c;
				}
				ParsePosition parsePosition = new ParsePosition(0);
			    Object object = format.parse( c.getControlNewText(), parsePosition );

			    if(object == null || parsePosition.getIndex() < c.getControlNewText().length()){
			        return null;
			    }else{
			        return c;
			    }
			}));
			rhs[i].setPrefWidth(40);
			constraintRows[i].getChildren().addAll(operator, rhs[i]);
			
			constraints.getChildren().add(constraintRows[i]);
		}
		
		root.setPadding(new Insets(8));
		root.setStyle("-fx-border-color: grey; -fx-border-width: 2px"); 
		root.getChildren().addAll(new Label("Enter your LP problem: "), variables, new Label("Subject to: "), constraints);
		return root;
	}
	
	/**
	 * Loads the icon used to represent the application.
	 * 
	 * @return icon the specified icon.
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
	
	/**
	 * Loads the university logo used on the top of the data entry screen
	 * 
	 * @return img the specified logo.
	 */
	private Image loadImage(){
		String separator  = File.separator; //for multiplatform support
		File imageFile     = new File("src" + separator + "RevisedSimplex" + separator + "gui" + separator + "koc.png");
		InputStream input = null;
		Image img        = null;		
		try{
			input = new FileInputStream(imageFile);
			img  = new Image(input);
			input.close();
		}catch(FileNotFoundException e){
			
		}catch(IOException e){
			
		}
		
		return img;
	}
	
	/**
	 * Listener used to detect and store the constraint type(greater than, less than, equal to) immediately upon it being changed.
	 * 
	 * @author Berk Kaan Kuguoglu
	 */
	private class ChoiceChangeListener implements ChangeListener<Number>{
		int index;
		public ChoiceChangeListener(int index){
			this.index = index;
		}
		
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			constraintType[index] = newValue.intValue();
			
		}
	}
	
	public static void main(String[] args) {
	    launch(args);
	}

}
