package reservo.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main JavaFX client application for Reservo.
 * This is a skeleton - full implementation would include calendar view, booking dialogs, etc.
 */
public class ReservoClient extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Reservo - Resource Reservation System");
        
        VBox root = new VBox(10);
        root.getChildren().add(new Label("Reservo Client"));
        root.getChildren().add(new Label("JavaFX client implementation would go here"));
        root.getChildren().add(new Label("- Calendar view for resource availability"));
        root.getChildren().add(new Label("- Booking dialogs with hold/confirm workflow"));
        root.getChildren().add(new Label("- Real-time updates via WebSocket"));
        root.getChildren().add(new Label("- My Reservations view"));
        root.getChildren().add(new Label("- Waitlist management"));
        root.getChildren().add(new Label("- Notifications inbox"));
        root.getChildren().add(new Label("- Admin console"));
        
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

