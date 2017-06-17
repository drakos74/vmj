package fine.vmj.fx.workspace;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Workspace_FrameStorage extends Application {
	
	@Override
	public void start(Stage primaryStage) {
		
		try
		{
			// load the FXML resource
			FXMLLoader loader = new FXMLLoader(getClass().getResource("workspace_frame_storage.fxml"));
			// store the root element so that the controllers can use it
			SplitPane rootElement = (SplitPane) loader.load();
			// create and style a scene
			Scene scene = new Scene(rootElement, 1280, 720);
			scene.getStylesheets().add(getClass().getResource("workspace_frame_storage.css").toExternalForm());
			// create the stage with the given title and the previously created
			// scene
			primaryStage.setTitle("VMJ-STORAGE");
			primaryStage.setScene(scene);
			// show the GUI
			primaryStage.show();
			
			// set the proper behavior on closing the application
			Workspace_FrameStorage_Controller controller = loader.getController();
			controller.setStage(primaryStage);
			controller.init();
			primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we)
				{
					controller.setClosed();
				}
			}));
		}
		catch (Exception e)
		{
			e.printStackTrace();
}
		
	}
	
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		launch(args);
	}
}
