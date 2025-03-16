package de.neemann.digital.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import com.jutge.api.JutgeApiClient;

public class JutgeLoginDialog extends JDialog {
    private String email;
    private String token;
    private String tokenExpiration; // "YYYY-MM-DD HH:mm:ss.SSSX"
    private boolean loginSuccessful = false;

    // Components for input and error message.
    private JTextField emailField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();
    private JLabel errorLabel = new JLabel("");

    public JutgeLoginDialog(JFrame parent) {
        super(parent, "Jutge Login", true);
        setSize(400, 250);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Setup components.
        JLabel emailLabel = new JLabel("Email:");
        JLabel passwordLabel = new JLabel("Password:");

        // Set preferred dimensions for both fields.
        emailField.setPreferredSize(new Dimension(250, 25));
        passwordField.setPreferredSize(new Dimension(250, 25));

        errorLabel.setForeground(Color.RED);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener((ActionEvent e) -> {
            String inputEmail = emailField.getText();
            String inputPassword = new String(passwordField.getPassword());

            try {
                JutgeApiClient jutge = new JutgeApiClient();
                // Build credentials input.
                JutgeApiClient.CredentialsIn credentialsIn = new JutgeApiClient.CredentialsIn();
                credentialsIn.email = inputEmail;
                credentialsIn.password = inputPassword;
                // Wait for the API response.
                JutgeApiClient.CredentialsOut credentialsOut = jutge.auth.login(credentialsIn);

                if (credentialsOut == null || credentialsOut.token == null) {
                    errorLabel.setText("Login failed. Please try again."); // Likely to be due to a internal error
                } else if (credentialsOut.token.equals("")) {
                    errorLabel.setText("Login failed: either email or password are incorrect.");
                } else {
                    this.email = inputEmail;
                    this.token = credentialsOut.token;
                    this.tokenExpiration = credentialsOut.expiration;
                    loginSuccessful = true;
                    dispose();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                errorLabel.setText("Login failed. Please check your email and password.");
            }
        });

        // Layout components.
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        add(emailLabel, gbc);
        gbc.gridx = 1;
        add(emailField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        add(passwordLabel, gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(errorLabel, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(loginButton, gbc);
    }

    /**
     * Returns the credentials if login was successful.
     */
    public SendToJutgeDialog.JutgeCredentials getCredentials() {
        if (loginSuccessful) {
            return new SendToJutgeDialog.JutgeCredentials(email, token, tokenExpiration, "", "", "");
        }
        return null;
    }
}
