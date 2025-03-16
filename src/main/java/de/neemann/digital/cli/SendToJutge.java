package de.neemann.digital.cli;

import java.awt.Desktop;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.draw.elements.PinException;
import de.neemann.digital.draw.library.ElementLibrary;
import de.neemann.digital.draw.library.ElementNotFoundException;
import de.neemann.digital.draw.model.ModelCreator;
import de.neemann.digital.gui.Settings;
import de.neemann.digital.gui.components.CircuitComponent;
import de.neemann.digital.hdl.printer.CodePrinter;
import de.neemann.digital.hdl.verilog2.VerilogGenerator;
import de.neemann.digital.lang.Lang;

/**
 * The Send To Jutge of Digital
 * <p>
 * Created by Miquel Torner on 12.12.2024.
 */
public class SendToJutge {

    private final ElementLibrary library;
    private final CircuitComponent circuitComponent;
    // API endpoint and compiler_id used by the Jutge platform.
    private final String endpoint = "https://api.jutge.org/api";
    private final String compiler_id = "Circuits";
    // A unique boundary for the multipart/form-data body.
    private final String boundary = "----WebkitFormBoundary" + System.currentTimeMillis();

    /**
     * Creates an instance of SendToJutge.
     *
     * @param circuitComponent the circuit component to be exported
     * @param library          the element library
     */
    public SendToJutge(CircuitComponent circuitComponent, ElementLibrary library) {
        this.circuitComponent = circuitComponent;
        this.library = library;
    }

    /**
     * Exports the circuit to a Verilog file, reads the generated code,
     * and manually submits it using the "student.submissions.submitFull" API
     * endpoint.
     *
     * @param token      the user's token for authentication
     * @param problem_id the problem identifier
     * @param topModule  the top module name (used for naming the Verilog file)
     * @param anotations the submission annotation
     */
    public void sendProblem(String token, String problem_id, String topModule, String anotations) {

        // Check the model for errors.
        try {
            new ModelCreator(circuitComponent.getCircuit(), library).createModel(false);
        } catch (PinException | NodeException | ElementNotFoundException error) {
            JOptionPane.showMessageDialog(circuitComponent, Lang.get("msg_modelHasErrors"), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Construct the file name and export directory.
        ElementAttributes settings = Settings.getInstance().getAttributes();
        File oldExportDirectory = settings.getFile("exportDirectory");
        File outputFile = new File(topModule + ".v");
        File exportDirectory = new File(System.getProperty("java.io.tmpdir"));

        if (exportDirectory != null) {
            outputFile = new File(exportDirectory, topModule + ".v");
        }

        settings.setFile("exportDirectory", outputFile.getParentFile());

        // Export the circuit to a Verilog file.
        try (VerilogGenerator vlog = new VerilogGenerator(library, new CodePrinter(outputFile))) {
            vlog.export(circuitComponent.getCircuit());
        } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(circuitComponent,
                    "Error while exporting to Verilog.\nCheck your circuit labels and connections and try again.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Read the exported Verilog file into a byte array.
        byte[] verilogProgram = null;
        try {
            verilogProgram = Files.readAllBytes(outputFile.toPath());
        } catch (IOException e1) {
            System.err.println("Error reading Verilog file: " + outputFile.toPath());
            return;
        }

        // String verilogCode = new String(verilogProgram, StandardCharsets.UTF_8);

        // Default submission URL (if submission_id extraction fails).
        String submissionUrl = "https://jutge.org/problems/" + problem_id + "/submissions";

        try {
            // Open a connection to the API endpoint.
            HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);

            // Prepare the multipart body.
            try (DataOutputStream os = new DataOutputStream(connection.getOutputStream())) {
                // Build the JSON data with the submitFull function.
                String inputJson = String.format(
                        "{\"func\":\"student.submissions.submitFull\", \"input\":{\"problem_id\":\"%s\", \"compiler_id\":\"%s\", \"annotation\":\"%s\"}, \"meta\":{\"token\":\"%s\"}}",
                        problem_id, compiler_id, anotations, token);
                // Write the JSON part.
                os.writeBytes("--" + boundary + "\r\n");
                os.writeBytes("Content-Disposition: form-data; name=\"data\"\r\n\r\n");
                os.writeBytes(inputJson + "\r\n");

                // Write the file part.
                os.writeBytes("--" + boundary + "\r\n");
                os.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"program.v\"\r\n");
                os.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
                os.write(verilogProgram);
                os.writeBytes("\r\n");

                // End the multipart body.
                os.writeBytes("--" + boundary + "--\r\n");
                os.flush();
            }

            // Get the API response.
            int responseCode = connection.getResponseCode();
            // System.out.println("Response Code: " + responseCode);

            StringBuilder responseBuilder = new StringBuilder();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // System.out.println("Request succeeded!");
                try (Scanner scanner = new Scanner(connection.getInputStream())) {
                    while (scanner.hasNextLine()) {
                        responseBuilder.append(scanner.nextLine());
                    }
                }
                // System.out.println("Response: " + responseBuilder.toString());
            } else {
                try (Scanner scanner = new Scanner(connection.getErrorStream())) {
                    while (scanner.hasNextLine()) {
                        responseBuilder.append(scanner.nextLine());
                    }
                }
                System.err.println("Error Response: " + responseBuilder.toString());
            }

            // Try to extract the submission_id from the response.
            String rawResponse = responseBuilder.toString();
            int jsonStart = rawResponse.indexOf("{");
            int jsonEnd = rawResponse.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonPart = rawResponse.substring(jsonStart, jsonEnd + 1);
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(jsonPart);
                    JSONObject output = (JSONObject) json.get("output");
                    if (output != null && output.get("submission_id") != null) {
                        String submissionId = output.get("submission_id").toString();
                        submissionUrl = "https://jutge.org/problems/" + problem_id + "/submissions/" + submissionId;
                    }
                } catch (ParseException pe) {
                    pe.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Instead of using a JEditorPane, ask the user if they want to open the
        // specific submission page.
        showSubmissionDialog(submissionUrl);

        // Clean up temporary file and restore export directory.
        outputFile.delete();
        settings.setFile("exportDirectory", oldExportDirectory);
    }

    /**
     * Displays a dialog asking the user if they want to open the submission page.
     * If they agree, attempts to open the URL in the default browser.
     *
     * @param url the URL to open
     */
    private void showSubmissionDialog(String url) {
        int option = JOptionPane.showConfirmDialog(null,
                "Submission succeeded. Do you want to open the submission page?\n" + url,
                "Submission", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Desktop browsing is not supported. Please copy this URL:\n" + url);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to open the link. Please copy and paste this URL:\n" + url);
            }
        }
    }
}
