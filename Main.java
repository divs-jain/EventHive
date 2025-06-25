import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.sql.Date;
import java.sql.Time;
import javafx.geometry.Insets;
import java.sql.*;

public class Main extends Application {

    private static Connection connection;
    private static int loggedInUserId = -1;


    public void connect1() {
        String url = "jdbc:mysql://localhost:3306/EMS";
        String user = "root";
        String password = "12345678";

        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connected to MySQL Database!");
        } catch (SQLException e) {
            System.out.println("❌ Connection Failed!");
            e.fillInStackTrace();
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    @Override
    public void start(Stage stage) {
        connect1(); // Establish DB connection at start
        showMainScreen(stage);
    }

    private void showMainScreen(Stage stage) {
        Label title = new Label("Welcome to Event Management System");
        Button loginButton = new Button("Login");
        Button signupButton = new Button("Sign Up");

        loginButton.setOnAction(e -> showLoginScreen(stage));
        signupButton.setOnAction(e -> showSignupScreen(stage));

        VBox root = new VBox(10, title, loginButton, signupButton);
        root.setStyle("-fx-padding: 20;");
        stage.setScene(new Scene(root, 300, 200));
        stage.setTitle("Event Management System");
        stage.show();
    }

    private void showLoginScreen(Stage stage) {
        Label loginLabel = new Label("Login");
        ChoiceBox<String> userType = new ChoiceBox<>();
        userType.getItems().addAll("User", "Admin");
        userType.setValue("User");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginBtn = new Button("Login");
        Button backBtn = new Button("Back");

        loginBtn.setOnAction(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            String role = userType.getValue();

            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?")) {
                stmt.setString(1, user);
                stmt.setString(2, pass);
                stmt.setString(3, role);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    loggedInUserId = rs.getInt("user_id"); // Save logged-in user ID
                    if (role.equals("Admin")) {
                        showAdminPage(stage);
                    } else {
                        showEventPage(stage);
                    }
                } else {
                    new Alert(Alert.AlertType.ERROR, "Invalid credentials!").showAndWait();
                    showMainScreen(stage);
                }
            } catch (SQLException ex) {
                ex.fillInStackTrace();
                new Alert(Alert.AlertType.ERROR, "Database Error!").showAndWait();
            }
        });

        backBtn.setOnAction(ev -> showMainScreen(stage));

        VBox loginLayout = new VBox(10, loginLabel, userType, usernameField, passwordField, loginBtn, backBtn);
        loginLayout.setStyle("-fx-padding: 20;");
        stage.setScene(new Scene(loginLayout, 300, 250));
    }

    private void showSignupScreen(Stage stage) {
        Label signupLabel = new Label("Sign Up");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Choose a Password");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField contactField = new TextField();
        contactField.setPromptText("Contact Number");

        Button signupBtn = new Button("Sign Up");
        Button backBtn = new Button("Back");

        signupBtn.setOnAction(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            String email = emailField.getText();
            String contact = contactField.getText();

            try (PreparedStatement checkStmt = getConnection().prepareStatement(
                    "SELECT * FROM users WHERE username = ?")) {
                checkStmt.setString(1, user);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    new Alert(Alert.AlertType.ERROR, "Username already exists!").showAndWait();
                } else {
                    PreparedStatement insertStmt = getConnection().prepareStatement(
                            "INSERT INTO users (username, password, email, phone, role) VALUES (?, ?, ?, ?, ?)");
                    insertStmt.setString(1, user);
                    insertStmt.setString(2, pass);
                    insertStmt.setString(3, email);
                    insertStmt.setString(4, contact);
                    insertStmt.setString(5, "user"); // default role is 'user'
                    insertStmt.executeUpdate();
                    new Alert(Alert.AlertType.INFORMATION, "Signup Successful!").showAndWait();
                }
                showMainScreen(stage);
            } catch (SQLException ex) {
                ex.fillInStackTrace();
                new Alert(Alert.AlertType.ERROR, "Database Error!").showAndWait();
            }
        });

        backBtn.setOnAction(ev -> showMainScreen(stage));

        VBox signupLayout = new VBox(10, signupLabel, usernameField, passwordField, emailField, contactField, signupBtn, backBtn);
        signupLayout.setStyle("-fx-padding: 20;");
        stage.setScene(new Scene(signupLayout, 300, 300));
    }

    private void showEventPage(Stage stage) {
        VBox eventLayout = new VBox(10);
        eventLayout.setStyle("-fx-padding: 20;");

        Label eventLabel = new Label("Your Registered Events");

        try {
            PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT e.name, e.event_date, e.event_time, e.event_id " +
                            "FROM events e " +
                            "JOIN registrations r ON e.event_id = r.event_id " +
                            "WHERE r.user_id = ? AND r.status = 'registered'"
            );
            stmt.setInt(1, loggedInUserId);  // Ensure loggedInUserId is correctly initialized
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                new Alert(Alert.AlertType.INFORMATION, "No registered events found!").showAndWait();
            } else {
                do {
                    String name = rs.getString("name");
                    Date date = rs.getDate("event_date");
                    Time time = rs.getTime("event_time");
                    int eventId = rs.getInt("event_id");

                    Label eventInfo = new Label("• " + name + " on " + date + " at " + time);
                    Button detailsBtn = new Button("Details");

                    // Handle event details button click
                    detailsBtn.setOnAction(ev -> showEventDetailsPage(stage, eventId));

                    HBox eventRow = new HBox(10, eventInfo, detailsBtn);
                    eventLayout.getChildren().add(eventRow);

                } while (rs.next());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error fetching your registered events!").showAndWait();
        }

        // Browse events button to show the event list
        Button browseBtn = new Button("Browse Events");
        browseBtn.setOnAction(e -> {
            try {
                showEventList(stage);  // This function is already implemented
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error showing event list: " + ex.getMessage()).showAndWait();
            }
        });

        // Back button to go to the login screen or previous page
        Button backBtn = new Button("Back");
        backBtn.setOnAction(ev -> showLoginScreen(stage));

        eventLayout.getChildren().addAll(browseBtn, backBtn);

        stage.setScene(new Scene(eventLayout, 400, 400));
    }

    private void showEventDetails(Stage stage, int eventId) {
        try {
            // Get the event details from the events table
            PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT e.name, e.description, e.event_date, e.event_time, v.name AS venue, t.price " +
                            "FROM events e " +
                            "JOIN venues v ON e.venue_id = v.venue_id " +
                            "JOIN tickets t ON e.event_id = t.event_id " +
                            "WHERE e.event_id = ?");
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String eventName = rs.getString("name");
                String eventDesc = rs.getString("description");
                LocalDate eventDate = rs.getDate("event_date").toLocalDate();
                String eventTime = rs.getTime("event_time").toString();
                String venue = rs.getString("venue");
                double price = rs.getDouble("price");

                // Display event details and price
                Label eventDetailsLabel = new Label("Event: " + eventName + "\n" +
                        "Description: " + eventDesc + "\n" +
                        "Date: " + eventDate + "\n" +
                        "Time: " + eventTime + "\n" +
                        "Venue: " + venue + "\n" +
                        "Price: " + price);

                Button confirmAndPayBtn = new Button("Confirm and Pay");
                confirmAndPayBtn.setOnAction(e -> {
                    try {
                        registerForEvent(stage, eventId, price);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Error during registration: " + ex.getMessage()).showAndWait();
                    }
                });

                VBox eventDetailsLayout = new VBox(10, eventDetailsLabel, confirmAndPayBtn);
                eventDetailsLayout.setStyle("-fx-padding: 20;");
                stage.setScene(new Scene(eventDetailsLayout, 400, 400));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error fetching event details!").showAndWait();
        }
    }

    private void showEventDetailsPage(Stage stage, int eventId) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20;");

        try {
            PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT e.name, e.description, e.event_date, e.event_time, v.name AS venue_name " +
                            "FROM events e " +
                            "JOIN venues v ON e.venue_id = v.venue_id " +
                            "WHERE e.event_id = ?"
            );
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                String desc = rs.getString("description");
                Date date = rs.getDate("event_date");
                Time time = rs.getTime("event_time");
                String venue = rs.getString("venue_name");

                LocalDate localDate = date.toLocalDate();
                String dayOfWeek = localDate.getDayOfWeek().toString();

                Label title = new Label("Event Details");
                Label nameLabel = new Label("Name: " + name);
                Label descLabel = new Label("Description: " + desc);
                Label dateLabel = new Label("Date: " + date + " (" + dayOfWeek + ")");
                Label timeLabel = new Label("Time: " + time);
                Label venueLabel = new Label("Venue: " + venue);

                Button cancelBtn = new Button("Cancel Registration");
                Button backBtn = new Button("Back");

                cancelBtn.setOnAction(ev -> {
                    cancelRegistration(eventId);  // Call the cancel registration logic
                    showEventPage(stage);  // Refresh the registered events page
                });

                backBtn.setOnAction(ev -> showEventPage(stage));

                layout.getChildren().addAll(title, nameLabel, descLabel, dateLabel, timeLabel, venueLabel, cancelBtn, backBtn);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading event details!").showAndWait();
        }

        stage.setScene(new Scene(layout, 400, 350));
    }

    private void cancelRegistration(int eventId) {
        try {
            // Update the status to 'cancelled' instead of deleting the registration
            PreparedStatement stmt = getConnection().prepareStatement(
                    "UPDATE registrations SET status = 'cancelled' WHERE user_id = ? AND event_id = ?");
            stmt.setInt(1, loggedInUserId);  // Ensure loggedInUserId is correctly set
            stmt.setInt(2, eventId);
            stmt.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Registration cancelled.").showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error cancelling registration!").showAndWait();
        }
    }

    private void showEventList(Stage stage) {
        VBox eventListLayout = new VBox(10);
        eventListLayout.setStyle("-fx-padding: 20;");
        Label eventListLabel = new Label("Browse Events");
        eventListLayout.getChildren().add(eventListLabel);

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> showEventPage(stage));

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM events")) {

            // Clear the previous list in case of navigation issues
            eventListLayout.getChildren().clear();

            while (rs.next()) {
                int eventId = rs.getInt("event_id");
                String eventName = rs.getString("name");
                String eventDesc = rs.getString("description");

                Label eventLabel = new Label(eventName + " - " + eventDesc);
                Button registerBtn = new Button("Register");
                registerBtn.setOnAction(e -> showEventDetailsBeforeRegister(stage, eventId));

                HBox eventRow = new HBox(10, eventLabel, registerBtn);
                eventListLayout.getChildren().add(eventRow);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error fetching events: " + ex.getMessage()).showAndWait();
        }

        eventListLayout.getChildren().add(backBtn);
        stage.setScene(new Scene(eventListLayout, 400, 400));
    }

    private void showEventDetailsBeforeRegister(Stage stage, int eventId) {
        try {
            // Updated SQL query to join events and tickets tables
            PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT e.*, v.name AS venue_name, t.price AS ticket_price " +
                            "FROM events e " +
                            "JOIN venues v ON e.venue_id = v.venue_id " +
                            "JOIN tickets t ON t.event_id = e.event_id " +
                            "WHERE e.event_id = ?");
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String eventName = rs.getString("name");
                String desc = rs.getString("description");
                String venue = rs.getString("venue_name");
                LocalDate date = rs.getDate("event_date").toLocalDate();
                String time = rs.getTime("event_time").toString();
                double price = rs.getDouble("ticket_price");

                VBox detailBox = new VBox(10);
                detailBox.setPadding(new Insets(20));

                detailBox.getChildren().addAll(
                        new Label("Event: " + eventName),
                        new Label("Description: " + desc),
                        new Label("Date: " + date),
                        new Label("Time: " + time),
                        new Label("Venue: " + venue),
                        new Label("Price: ₹" + price)
                );

                Button confirmBtn = new Button("Confirm and Pay");
                confirmBtn.setOnAction(ev -> registerForEvent(stage, eventId, price));
                detailBox.getChildren().add(confirmBtn);

                Button backBtn = new Button("Back");
                backBtn.setOnAction(ev -> showEventList(stage));
                detailBox.getChildren().add(backBtn);

                stage.setScene(new Scene(detailBox, 400, 400));
            }
        } catch (SQLException e) {
            // Detailed error logging
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading event details! " + e.getMessage()).showAndWait();
        }
    }

    private void registerForEvent(Stage stage, int eventId, double price) {
        try {
            // Register user for the event in the registrations table
            PreparedStatement stmt = getConnection().prepareStatement(
                    "INSERT INTO registrations (user_id, event_id, status) VALUES (?, ?, ?)");
            stmt.setInt(1, loggedInUserId);  // Logged in user
            stmt.setInt(2, eventId);
            stmt.setString(3, "registered");
            stmt.executeUpdate();

            // Insert ticket for the user
            PreparedStatement ticketStmt = getConnection().prepareStatement(
                    "INSERT INTO tickets (event_id, price) VALUES (?, ?)");
            ticketStmt.setInt(1, eventId);
            ticketStmt.setDouble(2, price);
            ticketStmt.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Registered successfully!").showAndWait();
            showEventPage(stage);  // Redirect to event page after registration
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error registering for event!").showAndWait();
        }
    }

    private void showAdminPage(Stage stage) {
        Label adminLabel = new Label("Admin Dashboard");
        Button manageEventsBtn = new Button("Manage Events");
        Button viewEventsBtn = new Button("View All Events");
        Button backBtn = new Button("Back");

        manageEventsBtn.setOnAction(e -> showManageEventsScreen(stage));
        viewEventsBtn.setOnAction(e -> showAdminEventList(stage));
        backBtn.setOnAction(ev -> showLoginScreen(stage));

        VBox adminLayout = new VBox(10, adminLabel, manageEventsBtn, viewEventsBtn, backBtn);
        adminLayout.setStyle("-fx-padding: 20;");
        stage.setScene(new Scene(adminLayout, 300, 250));
    }

    private void showAdminEventList(Stage stage) {
        VBox eventListLayout = new VBox(10);
        eventListLayout.setStyle("-fx-padding: 20;");
        Label eventListLabel = new Label("All Events");
        eventListLayout.getChildren().add(eventListLabel);

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> showAdminPage(stage));

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM events")) {

            while (rs.next()) {
                int eventId = rs.getInt("event_id");
                String eventName = rs.getString("name");
                String eventDesc = rs.getString("description");

                Label eventLabel = new Label(eventName + " - " + eventDesc);
                Button editBtn = new Button("Edit");
                Button deleteBtn = new Button("Delete");

                // Action for Edit button
                editBtn.setOnAction(e -> showEditEventScreen(stage, eventId));

                // Action for Delete button
                deleteBtn.setOnAction(e -> {
                    deleteEvent(eventId);
                    showAdminEventList(stage); // Refresh the list after deletion
                });

                HBox eventRow = new HBox(10, eventLabel, editBtn, deleteBtn);
                eventListLayout.getChildren().add(eventRow);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error fetching events!").showAndWait();
        }

        eventListLayout.getChildren().add(backBtn);
        stage.setScene(new Scene(eventListLayout, 400, 400));
    }

    private void deleteEvent(int eventId) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);  // Begin transaction

            // Step 1: Delete registrations (if any)
            PreparedStatement delReg = conn.prepareStatement(
                    "DELETE FROM registrations WHERE event_id = ?");
            delReg.setInt(1, eventId);
            delReg.executeUpdate();

            // Step 2: Delete tickets
            PreparedStatement delTickets = conn.prepareStatement(
                    "DELETE FROM tickets WHERE event_id = ?");
            delTickets.setInt(1, eventId);
            delTickets.executeUpdate();

            // Step 3: Delete the event itself
            PreparedStatement delEvent = conn.prepareStatement(
                    "DELETE FROM events WHERE event_id = ?");
            delEvent.setInt(1, eventId);
            delEvent.executeUpdate();

            conn.commit(); // Everything succeeded

            new Alert(Alert.AlertType.INFORMATION, "Event deleted successfully!").showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback(); // Rollback if anything failed
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            new Alert(Alert.AlertType.ERROR, "Error deleting event!").showAndWait();
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showEditEventScreen(Stage stage, int eventId) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20;");

        try {
            // Fetch event details from the events table
            PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT * FROM events WHERE event_id = ?");
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                String desc = rs.getString("description");
                Date date = rs.getDate("event_date");
                Time time = rs.getTime("event_time");
                int venueId = rs.getInt("venue_id");

                // UI Fields
                TextField eventNameField = new TextField(name);
                TextField eventDescField = new TextField(desc);
                DatePicker eventDatePicker = new DatePicker(date.toLocalDate());
                TextField eventTimeField = new TextField(time.toString());

                // Venue combo box
                ComboBox<String> venueCombo = new ComboBox<>();
                int selectedIndex = -1;
                int currentIndex = 0;

                try (Statement stmtVenue = getConnection().createStatement();
                     ResultSet venueRs = stmtVenue.executeQuery("SELECT venue_id, name FROM venues")) {
                    while (venueRs.next()) {
                        int id = venueRs.getInt("venue_id");
                        String venueName = venueRs.getString("name");
                        String item = id + " - " + venueName;
                        venueCombo.getItems().add(item);
                        if (id == venueId) {
                            selectedIndex = currentIndex;
                        }
                        currentIndex++;
                    }
                }

                if (selectedIndex != -1) {
                    venueCombo.getSelectionModel().select(selectedIndex);
                }

                // Fetch price from tickets table
                double price = 0.0;
                try (PreparedStatement ticketStmt = getConnection().prepareStatement(
                        "SELECT price FROM tickets WHERE event_id = ?")) {
                    ticketStmt.setInt(1, eventId);
                    ResultSet ticketRs = ticketStmt.executeQuery();
                    if (ticketRs.next()) {
                        price = ticketRs.getDouble("price");
                    }
                }

                TextField priceField = new TextField(String.valueOf(price));

                // Buttons
                Button updateBtn = new Button("Update Event");
                Button backBtn = new Button("Back");

                updateBtn.setOnAction(e -> {
                    try {
                        String updatedName = eventNameField.getText();
                        String updatedDesc = eventDescField.getText();
                        LocalDate updatedDate = eventDatePicker.getValue();
                        String updatedTime = eventTimeField.getText();
                        String updatedVenue = venueCombo.getValue();
                        int updatedVenueId = Integer.parseInt(updatedVenue.split(" - ")[0]);
                        double updatedPrice = Double.parseDouble(priceField.getText());

                        // Validation checks
                        if (updatedName.isEmpty() || updatedDesc.isEmpty() || updatedTime.isEmpty() || updatedVenue == null || updatedDate == null) {
                            new Alert(Alert.AlertType.ERROR, "Please fill in all fields.").showAndWait();
                            return;
                        }

                        // Validate time format (HH:MM)
                        if (!updatedTime.matches("\\d{2}:\\d{2}")) {
                            new Alert(Alert.AlertType.ERROR, "Please enter a valid time in HH:MM format.").showAndWait();
                            return;
                        }

                        // Validate price is a valid number
                        if (updatedPrice < 0) {
                            new Alert(Alert.AlertType.ERROR, "Price cannot be negative.").showAndWait();
                            return;
                        }

                        updateEvent(eventId, updatedName, updatedDesc, updatedDate, updatedTime, updatedVenueId, updatedPrice, stage);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Please check your input values.").showAndWait();
                    }
                });

                backBtn.setOnAction(ev -> showAdminEventList(stage));

                layout.getChildren().addAll(
                        new Label("Event Name:"), eventNameField,
                        new Label("Description:"), eventDescField,
                        new Label("Date:"), eventDatePicker,
                        new Label("Time (HH:MM):"), eventTimeField,
                        new Label("Venue:"), venueCombo,
                        new Label("Price:"), priceField,
                        updateBtn, backBtn
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading event details!").showAndWait();
        }

        stage.setScene(new Scene(layout, 400, 500));
    }

    private void updateEvent(int eventId, String name, String desc, LocalDate date, String timeStr, int venueId, double price, Stage stage) {
        try {
            // Update event details in the events table
            PreparedStatement stmt = getConnection().prepareStatement(
                    "UPDATE events SET name = ?, description = ?, event_date = ?, event_time = ?, venue_id = ? WHERE event_id = ?");

            stmt.setString(1, name);
            stmt.setString(2, desc);
            stmt.setDate(3, Date.valueOf(date));
            stmt.setTime(4, Time.valueOf(timeStr + ":00"));
            stmt.setInt(5, venueId);
            stmt.setInt(6, eventId);

            stmt.executeUpdate();

            // Update price in the tickets table
            PreparedStatement priceStmt = getConnection().prepareStatement(
                    "UPDATE tickets SET price = ? WHERE event_id = ?");
            priceStmt.setDouble(1, price);
            priceStmt.setInt(2, eventId);
            priceStmt.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Event updated successfully!").showAndWait();
            showAdminEventList(stage); // Refresh event list after update
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error updating event!").showAndWait();
        }
    }





    private void showManageEventsScreen(Stage stage) {
        Label manageLabel = new Label("Manage Events");
        Button createEventBtn = new Button("Create New Event");
        Button backBtn = new Button("Back");

        createEventBtn.setOnAction(e -> showCreateEventScreen(stage));
        backBtn.setOnAction(ev -> showAdminPage(stage));

        VBox manageLayout = new VBox(10, manageLabel, createEventBtn, backBtn);
        manageLayout.setStyle("-fx-padding: 20;");
        stage.setScene(new Scene(manageLayout, 300, 250));
    }

    private void showCreateEventScreen(Stage stage) {
        Label createEventLabel = new Label("Create Event");

        TextField eventNameField = new TextField();
        eventNameField.setPromptText("Event Name");

        TextField eventDescriptionField = new TextField();
        eventDescriptionField.setPromptText("Event Description");

        DatePicker eventDatePicker = new DatePicker();
        TextField eventTimeField = new TextField();
        eventTimeField.setPromptText("Time (HH:MM)");

        TextField eventPriceField = new TextField();  // Add price field
        eventPriceField.setPromptText("Price");

        ComboBox<String> eventTypeCombo = new ComboBox<>();
        ComboBox<String> venueCombo = new ComboBox<>();

        // Load event types
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT event_type_id, type_name FROM event_types")) {
            while (rs.next()) {
                int id = rs.getInt("event_type_id");
                String name = rs.getString("type_name");
                eventTypeCombo.getItems().add(id + " - " + name);
            }
        } catch (SQLException e) {
            e.fillInStackTrace();
        }

        // Load venues
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT venue_id, name, capacity FROM venues")) {
            while (rs.next()) {
                int id = rs.getInt("venue_id");
                String name = rs.getString("name");
                int capacity = rs.getInt("capacity");
                venueCombo.getItems().add(id + " - " + name + " (Cap: " + capacity + ")");
            }
        } catch (SQLException e) {
            e.fillInStackTrace();
        }

        Button createEventBtn = new Button("Create Event");
        Button backBtn = new Button("Back");

        createEventBtn.setOnAction(e -> {
            String eventName = eventNameField.getText();
            String eventDesc = eventDescriptionField.getText();
            LocalDate eventDate = eventDatePicker.getValue();
            String timeStr = eventTimeField.getText();
            String selectedType = eventTypeCombo.getValue();
            String selectedVenue = venueCombo.getValue();
            String priceStr = eventPriceField.getText();  // Get price from the price field

            if (selectedType == null || selectedVenue == null || eventDate == null || timeStr.isEmpty() || priceStr.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Please fill all fields!").showAndWait();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);  // Parse price as double

                int typeId = Integer.parseInt(selectedType.split(" - ")[0]);
                int venueId = Integer.parseInt(selectedVenue.split(" - ")[0]);

                createEvent(eventName, eventDesc, eventDate, timeStr, typeId, venueId, price, stage);  // Pass price to createEvent
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid price entered!").showAndWait();
            }
        });

        backBtn.setOnAction(ev -> showManageEventsScreen(stage));

        VBox layout = new VBox(10,
                createEventLabel,
                eventNameField,
                eventDescriptionField,
                eventDatePicker,
                eventTimeField,
                eventPriceField,  // Add price field to layout
                eventTypeCombo,
                venueCombo,
                createEventBtn,
                backBtn
        );
        layout.setStyle("-fx-padding: 20;");
        stage.setScene(new Scene(layout, 400, 500));  // Increased height for the price field
    }


    private void createEvent(String name, String desc, LocalDate date, String timeStr,
                             int typeId, int venueId, double price, Stage stage) {
        // Declare the necessary objects here to ensure they are closed later
        PreparedStatement stmt = null;
        PreparedStatement ticketStmt = null;
        ResultSet rs = null;

        try {
            // Step 1: Insert the event into the events table
            String insertEventQuery = "INSERT INTO events (name, description, event_date, event_time, event_type_id, venue_id, created_by, capacity) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            stmt = getConnection().prepareStatement(insertEventQuery, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, name);
            stmt.setString(2, desc);
            stmt.setDate(3, Date.valueOf(date));
            stmt.setTime(4, Time.valueOf(timeStr + ":00"));
            stmt.setInt(5, typeId);
            stmt.setInt(6, venueId);
            stmt.setInt(7, 1); // Dummy admin ID (this can be changed if needed)
            stmt.setInt(8, 100); // Default capacity

            // Execute the update to insert the event
            stmt.executeUpdate();

            // Get the generated event ID
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int eventId = rs.getInt(1);  // Retrieve the event ID

                // Step 2: Insert ticket price into the tickets table
                String insertTicketQuery = "INSERT INTO tickets (event_id, price) VALUES (?, ?)";
                ticketStmt = getConnection().prepareStatement(insertTicketQuery);
                ticketStmt.setInt(1, eventId);  // Use the generated event ID
                ticketStmt.setDouble(2, price); // Use the dynamic price passed to the function

                // Execute the insert statement for tickets
                ticketStmt.executeUpdate();

                // Show success alert
                new Alert(Alert.AlertType.INFORMATION, "Event and ticket created successfully!").showAndWait();
            }

            // Redirect to the manage events screen after successful creation
            showManageEventsScreen(stage);

        } catch (SQLException e) {
            e.printStackTrace();
            // Show error alert if something goes wrong
            new Alert(Alert.AlertType.ERROR, "Error creating event! " + e.getMessage()).showAndWait();
        } finally {
            // Always close the resources to prevent memory leaks
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (ticketStmt != null) ticketStmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }




    public static void main(String[] args) {
        launch(args);
    }
}
