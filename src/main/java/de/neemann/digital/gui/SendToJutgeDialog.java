package de.neemann.digital.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class SendToJutgeDialog extends JDialog {
    private String email;
    private String token;
    private String tokenExpiration; // new field for token expiration
    private String problem;
    private String topModule;
    private String anotations;

    // New label to display logged in status or error message.
    private JLabel loginStatusLabel = new JLabel("");
    // New button to open the login dialog.
    private JButton loginButton = new JButton("Log In");

    /**
     * Returns the login window.
     *
     * @param parent the parent
     */
    public SendToJutgeDialog(JFrame parent) {
        super(parent, "Send to Jutge", true); // Modal dialog with parent window
        setSize(450, 350); // Increase the dialog size for better visibility
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create components for problem details.
        JLabel problemLabel = new JLabel("Problem:");
        JTextField problemField = new JTextField();
        problemField.setPreferredSize(new Dimension(200, 25)); 

        JLabel topModuleLabel = new JLabel("Top module name:");
        JTextField topModuleField = new JTextField();
        topModuleField.setPreferredSize(new Dimension(200, 25)); 

        JLabel anotationsLabel = new JLabel("Anotations:");
        JTextField anotationsField = new JTextField();
        anotationsField.setPreferredSize(new Dimension(200, 25)); 

        // Remember credentials checkbox and send button.
        JCheckBox rememberCredentialsCheckBox = new JCheckBox("Remember Login");
        JButton sendButton = new JButton("Send");
        sendButton.setEnabled(false); // Disable send button by default

        // Panel for login status and login button.
        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        loginStatusLabel.setForeground(Color.BLUE); // default color when logged in
        loginPanel.add(loginStatusLabel);
        loginPanel.add(loginButton);

        // Load any existing credentials.
        loadJutgeCredentials(topModuleField, problemField, problemField, rememberCredentialsCheckBox);
        
        // Check if saved credentials are valid and not expired.
        if (token != null && !token.isEmpty() && email != null && !email.isEmpty() && tokenExpiration != null && !tokenExpiration.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                Date expDate = sdf.parse(tokenExpiration);
                if (expDate.after(new Date())) {
                    loginStatusLabel.setText("Logged in as " + email);
                    loginButton.setText("Re-Log In");
                    sendButton.setEnabled(true);
                } else {
                    loginStatusLabel.setText("Session expired. Please log in again.");
                    loginStatusLabel.setForeground(Color.RED);
                    sendButton.setEnabled(false);
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
                loginStatusLabel.setText("Error parsing token expiration date.");
                loginStatusLabel.setForeground(Color.RED);
                sendButton.setEnabled(false);
            }
        } else {
            loginStatusLabel.setText("Not logged in");
            sendButton.setEnabled(false);
        }

        // Add action to loginButton to open the new login dialog.
        loginButton.addActionListener((ActionEvent e) -> {
            JutgeLoginDialog loginDialog = new JutgeLoginDialog((JFrame) getParent());
            loginDialog.setVisible(true);
            SendToJutgeDialog.JutgeCredentials creds = loginDialog.getCredentials();
            if (creds != null) {
                this.email = creds.getEmail();
                this.token = creds.getToken();
                this.tokenExpiration = creds.getExpiration();
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                    Date expDate = sdf.parse(tokenExpiration);
                    if (expDate.after(new Date())) {
                        loginStatusLabel.setText("Logged in as " + this.email);
                        loginStatusLabel.setForeground(Color.BLUE);
                        sendButton.setEnabled(true);
                    } else {
                        loginStatusLabel.setText("Session expired. Please log in again.");
                        loginStatusLabel.setForeground(Color.RED);
                        sendButton.setEnabled(false);
                    }
                } catch (ParseException ex) {
                    ex.printStackTrace();
                    loginStatusLabel.setText("Error parsing token expiration date.");
                    loginStatusLabel.setForeground(Color.RED);
                    sendButton.setEnabled(false);
                }
            } else {
                loginStatusLabel.setText("Login failed. Check your email and password.");
                loginStatusLabel.setForeground(Color.RED);
                sendButton.setEnabled(false);
            }
        });

        // Set up layout using GridBagLayout.
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5); // Padding

        int row = 0;
        // Add problem label and field.
        gbc.gridx = 0;
        gbc.gridy = row;
        add(problemLabel, gbc);
        gbc.gridx = 1;
        add(problemField, gbc);
        row++;

        // Add topModule label and field.
        gbc.gridx = 0;
        gbc.gridy = row;
        add(topModuleLabel, gbc);
        gbc.gridx = 1;
        add(topModuleField, gbc);
        row++;

        // Add anotations label and field.
        gbc.gridx = 0;
        gbc.gridy = row;
        add(anotationsLabel, gbc);
        gbc.gridx = 1;
        add(anotationsField, gbc);
        row++;

        // Add login panel (status label + login button).
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(loginPanel, gbc);
        row++;

        // Add remember credentials checkbox.
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(rememberCredentialsCheckBox, gbc);
        row++;

        // Add send button.
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(sendButton, gbc);

        // Add send button functionality.
        sendButton.addActionListener(e -> {
            // Check if token is expired before sending.
            if (tokenExpiration != null && !tokenExpiration.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                    Date expDate = sdf.parse(tokenExpiration);
                    if (expDate.before(new Date())) {
                        JOptionPane.showMessageDialog(this, "Session expired. Please log in again.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        loginStatusLabel.setText("Session expired. Please log in again.");
                        loginStatusLabel.setForeground(Color.RED);
                        sendButton.setEnabled(false);
                        return;
                    }
                } catch (ParseException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error checking token expiration. Please try logging in again.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    sendButton.setEnabled(false);
                    return;
                }
            }

            problem = problemField.getText();
            topModule = topModuleField.getText();
            anotations = anotationsField.getText();

            if (problem.isEmpty() || topModule.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Some of the fields are empty.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                // Save credentials if needed.
                saveJutgeCredentials(problem, topModule, email, token, tokenExpiration, rememberCredentialsCheckBox.isSelected());
                dispose();
            }
        });
    }

    /**
     * Returns the user credentials dialog
     *
     * @return user credentials dialog
     */
    public JutgeCredentials showDialog() {
        setVisible(true); // Open dialog and block until it's closed
        if (problem != null) {
            return new JutgeCredentials(email, token, tokenExpiration, problem, topModule, anotations);
        }
        return null; // Return null if dialog was closed without valid inputs
    }

    /**
     * User Credentials Class
     *
     */
    public static class JutgeCredentials {
        private final String email;
        private final String token;
        private final String expiration;
        private final String problem;
        private final String topModule;
        private final String anotations;

        public JutgeCredentials(String email, String token, String expiration, String problem, String topModule, String anotations) {
            this.email = email;
            this.token = token;
            this.expiration = expiration;
            this.problem = problem;
            this.topModule = topModule;
            this.anotations = anotations;
        }

        public String getEmail() {
            return email;
        }

        public String getToken() {
            return token;
        }

        public String getExpiration() {
            return expiration;
        }

        public String getProblem() {
            return problem;
        }

        public String getAnotations() {
            return anotations;
        }

        public String getTopModule() {
            return topModule;
        }
    }

    private void saveJutgeCredentials(String problem, String topModule, String email, String token, String expiration, boolean rememberCredentials) {
        Properties properties = new Properties();

        properties.setProperty("problem", problem);
        properties.setProperty("topModule", topModule);

        // Save email, token, and expiration only if rememberCredentials is selected.
        if (rememberCredentials && email != null && token != null) {
            properties.setProperty("email", email);
            properties.setProperty("token", token);
            if (expiration != null && !expiration.isEmpty()) {
                properties.setProperty("expiration", expiration);
            }
        }
        
        try (FileOutputStream fos = new FileOutputStream("credentials.properties")) {
            properties.store(fos, "Jutge Credentials");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadJutgeCredentials(JTextField topModuleField, JTextField problemField, JTextField emailField,
            JCheckBox rememberCredentialsCheckBox) {

        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream("credentials.properties")) {
            properties.load(fis);

            problemField.setText(properties.getProperty("problem", ""));
            topModuleField.setText(properties.getProperty("topModule", ""));
            String savedEmail = properties.getProperty("email", "");
            String savedToken = properties.getProperty("token", "");
            String savedExpiration = properties.getProperty("expiration", "");

            // If credentials exist, update local fields.
            if (!savedEmail.isEmpty() && !savedToken.isEmpty()) {
                this.email = savedEmail;
                this.token = savedToken;
                if (!savedExpiration.isEmpty()) {
                    this.tokenExpiration = savedExpiration;
                }
            }
        } catch (IOException e) {
            System.err.println("No credentials file found. Skipping loading.");
        }
    }
}
