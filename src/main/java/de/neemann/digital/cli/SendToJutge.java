package de.neemann.digital.cli;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.w3c.dom.events.MouseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

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

public class SendToJutge {

    private final ElementLibrary library;
    private final CircuitComponent circuitComponent;

    /**
     * Creates...
     *
     * @param circuitComponent circuit component
     * @param library          library
     */
    public SendToJutge(CircuitComponent circuitComponent, ElementLibrary library) {
        this.circuitComponent = circuitComponent;
        this.library = library;
    }

    public Map<String, String> login(String email, String password) {
        try {
            URL url = new URL("http://localhost:8000/api/login");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            String jsonInput = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
            try (OutputStream os = http.getOutputStream()) {
                os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
            }

            int status = http.getResponseCode();
            HashMap<String, String> result = new HashMap<>();

            if (status == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                result = parseJsonToMap(response.toString());
            } else {
                result.put("success", "false");
            }

            result.put("status", String.valueOf(status));

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the user credentials
     *
     * @param token   user token
     * @param problem problem ID
     * 
     */
    public void sendProblem(String token, String problem) {

        System.out.println("Token: " + token);
        System.out.println("Problem: " + problem);

        // Check model for errors
        try {
            new ModelCreator(circuitComponent.getCircuit(), library).createModel(false);
        } catch (PinException | NodeException | ElementNotFoundException error) {
            JOptionPane.showMessageDialog(circuitComponent, Lang.get("msg_modelHasErrors"), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Construct the file name
        ElementAttributes settings = Settings.getInstance().getAttributes();
        File oldExportDirectory = settings.getFile("exportDirectory");
        File outputFile = new File(problem + ".v");
        File exportDirectory = new File("/tmp/");

        if (exportDirectory != null) {
            outputFile = new File(exportDirectory, problem + ".v");
        }

        settings.setFile("exportDirectory", outputFile.getParentFile());

        try (VerilogGenerator vlog = new VerilogGenerator(library, new CodePrinter(outputFile))) {
            vlog.export(circuitComponent.getCircuit());
        } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(circuitComponent, e1.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String content;
            content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
            // System.out.println("Generated Verilog file:\n" + content);

            URL url = new URL("http://localhost:8000/api/submit");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            String jsonInput = String.format("{\"token\":\"%s\",\"problem\":\"%s\"}", token, problem);
            try (OutputStream os = http.getOutputStream()) {
                os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
            }

            int status = http.getResponseCode();
            // HashMap<String, String> result = new HashMap<>();

            if (status == 200) {
                System.out.println("Correct submission");
                // StringBuilder response = new StringBuilder();
                // try (BufferedReader reader = new BufferedReader(
                // new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8))) {
                // String line;
                // while ((line = reader.readLine()) != null) {
                // response.append(line);
                // }
                // }
                // result = parseJsonToMap(response.toString());
            } else {
                System.out.println("Incorrect submission");
                // result.put("success", "false");
            }
            // result.put("status", String.valueOf(status));

            JEditorPane editorPane = new JEditorPane("text/html",
                    "<html>See the correction <a href='https://jutge.org/problems/" + problem
                            + "_en/submissions'>here</a>.</html>");
            editorPane.setEditable(false);
            editorPane.setSelectionColor(null);
            editorPane.setOpaque(false);
            editorPane.setCursor(new Cursor(Cursor.HAND_CURSOR));

            editorPane.setHighlighter(null);
            editorPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            editorPane.addHyperlinkListener(e -> {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                    try {
                        Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            JOptionPane.showMessageDialog(null, editorPane, "Alert", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e1) {
            e1.printStackTrace();
        }

        settings.setFile("exportDirectory", oldExportDirectory);
    }

    public HashMap<String, String> parseJsonToMap(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
