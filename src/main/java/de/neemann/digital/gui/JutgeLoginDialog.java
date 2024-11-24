/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.gui;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The Jutge Login of Digital
 * <p>
 * Created by Miquel Torner on 20.11.2024.
 */
public class JutgeLoginDialog extends JDialog {
    private String email;
    private String password;
    private String problem;

    /**
     * Returns the login window.
     *
     * @param parent the parent
     */
    public JutgeLoginDialog(JFrame parent) {
        super(parent, "Login", true); // Modal dialog with parent window
        setSize(300, 250); // Increase the dialog size for better visibility
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create components
        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField();
        emailField.setPreferredSize(new Dimension(200, 25)); // Set a preferred width for the email field

        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(200, 25)); // Set a preferred width for the password field

        JLabel problemLabel = new JLabel("Problem:");
        JTextField problemField = new JTextField();
        problemField.setPreferredSize(new Dimension(200, 25)); // Set a preferred width for the email field

        JCheckBox rememberCredentialsCheckBox = new JCheckBox("Remember Credentials");

        JButton sendButton = new JButton("Send");

        // Load any existing credentials
        loadJutgeCredentials(problemField, emailField, passwordField, rememberCredentialsCheckBox);

        // Set up layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5); // Padding

        // Add components to the layout
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(problemLabel, gbc);
        gbc.gridx = 1;
        add(problemField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(emailLabel, gbc);
        gbc.gridx = 1;
        add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        add(passwordLabel, gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2; // Span checkbox across two columns
        add(rememberCredentialsCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2; // Span button across two columns
        add(sendButton, gbc);

        // Add button functionality
        sendButton.addActionListener(e -> {
            email = emailField.getText();
            password = new String(passwordField.getPassword());
            problem = problemField.getText();

            if (email.isEmpty() || password.isEmpty() || problem.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Some of the fields are empty.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                saveJutgeCredentials(problem, email, password, rememberCredentialsCheckBox.isSelected());
                dispose();
            }
        });
    }

    /**
     * Returns the user credentials dialog
     *
     * @return user credentials dailog
     */
    public UserCredentials showDialog() {
        setVisible(true); // Open dialog and block until it's closed
        if (email != null && password != null && problem != null) {
            return new UserCredentials(email, password, problem);
        }
        return null; // Return null if dialog was closed without valid inputs
    }

    /**
     * User Credentials Class
     *
     */
    public static class UserCredentials {
        private final String email;
        private final String password;
        private final String problem;

        /**
         * Returns the user credentials
         *
         * @param email    the email of the user
         * @param password the password of the user
         * @param problem  the problem to be evaluated
         */
        public UserCredentials(String email, String password, String problem) {
            this.email = email;
            this.password = password;
            this.problem = problem;
        }

        /**
         * Get email method
         *
         * @return the email
         */
        public String getEmail() {
            return email;
        }

        /**
         * Get password method
         *
         * @return the password
         */
        public String getPassword() {
            return password;
        }

        /**
         * Get password method
         *
         * @return the password
         */
        public String getProblem() {
            return problem;
        }


        /**
         * Get user credentials
         *
         * @return the user credentials
         */
        public UserCredentials getCredentials() {
            return new UserCredentials(email, password, problem);
        }
    }

    private void saveJutgeCredentials(String problem, String email, String password, boolean rememberCredentials) {
        Properties properties = new Properties();

        // Always save the problem field
        properties.setProperty("problem", problem);

        // Conditionally save email and password
        if (rememberCredentials) {
            properties.setProperty("email", email);
            properties.setProperty("password", password);
        }

        try (FileOutputStream fos = new FileOutputStream("credentials.properties")) {
            properties.store(fos, "Jutge Credentials");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadJutgeCredentials(JTextField problemField, JTextField emailField, 
        JPasswordField passwordField, JCheckBox rememberCredentialsCheckBox) {    
            
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream("credentials.properties")) {
            properties.load(fis);

            // Always load the problem field
            String problem = properties.getProperty("problem", "");
            problemField.setText(problem);

            // Conditionally load email and password if they exist
            String email = properties.getProperty("email", "");
            String password = properties.getProperty("password", "");

            emailField.setText(email);
            passwordField.setText(password);

            // Check the checkbox if email and password are not empty
            if (!email.isEmpty() && !password.isEmpty()) {
                rememberCredentialsCheckBox.setSelected(true);
            }

        } catch (IOException e) {
            System.err.println("No credentials file found. Skipping loading.");
    }
}
}
