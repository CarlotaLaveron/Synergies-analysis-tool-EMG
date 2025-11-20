package GUI;
import javafx.scene.chart.Chart;
import operations.EmgNative;
import operations.diver;
import operators.MatrixConfig;
import operators.SelectedMuscles;
import operations.matrix;
import operators.TwoMatrixes;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;
import java.util.*;

public class GUI extends JFrame {

    private static final String PLACEHOLDER = "Select muscle...";
    private static String selectedRegion, selectedDelim, selectedDecimal;
    private JComboBox<String> limbCombo;
    private JPanel centerArea, musclesPanel, configPanel, fullPanel, emgFilterPanel;
    private JSpinner channelsSpinner;
    private JButton saveButton, backBtn, saveBtn;
    private JLabel selectedFileLabel = new JLabel("No file selected");
    private List<JComboBox<String>> muscleCombos = new ArrayList<>();
    private File selectedFile;
    private List<String> savedMuscles = new ArrayList<>();
    private JSplitPane split;
    double[][] X_og, H, W, X_input;
    SelectedMuscles muscles = new SelectedMuscles();
    MatrixConfig matrix_config;
    List<Chart> charts;
    private JCheckBox filterEmgCheck;
    boolean filterEMG = false;

    private final java.util.List<String> MUSCLES_UpperLimb = Arrays.asList(
            "Abductor pollicis longus", "Anconeus", "Biceps brachii", "Brachialis", "Brachioradialis", "Coracobrachialis",
            "Deltoid", "Extensor carpi radialis brevis", "Extensor carpi radialis longus", "Extensor carpi ulnaris",
            "Extensor digiti minimi", "Extensor digitorum", "Extensor indicis", "Extensor pollicis brevis", "Extensor pollicis longus",
            "Flexor carpi radialis", "Flexor carpi ulnaris", "Flexor digitorum profundus", "Flexor digitorum superficialis",
            "Flexor pollicis longus", "Infraspinatus", "Latissimus dorsi", "Levator scapulae", "Palmaris longus",
            "Pectoralis major", "Pectoralis minor", "Pronator quadratus", "Pronator teres", "Rhomboid major", "Rhomboid minor",
            "Serratus anterior", "Subscapularis", "Supraspinatus", "Supinator", "Teres major", "Teres minor",
            "Trapezius", "Triceps brachii"
    );
    private final java.util.List<String> MUSCLES_LowerLimb = Arrays.asList(
            "Adductor brevis", "Adductor longus", "Adductor magnus", "Biceps femoris", "Fibularis longus",
            "Fibularis brevis", "Gastrocnemius", "Gluteus maximus", "Gluteus medius", "Gluteus minimus", "Gracilis",
            "Hamstrings", "Iliopsoas", "Obturator externus", "Obturator internus", "Pectineus", "Piriformis", "Plantaris",
            "Popliteus", "Quadriceps femoris", "Rectus femoris", "Sartorius", "Semimembranosus", "Semitendinosus",
            "Soleus", "Tensor fasciae latae", "Tibialis anterior", "Tibialis posterior", "Vastus intermedius", "Vastus lateralis",
            "Vastus medialis"
    );


    public GUI() {
        JLabel title = new JLabel("SYNERGIES ALGORITHM");
        title.setFont(new Font("Times New Roman", Font.BOLD, 36));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        add(title, BorderLayout.NORTH);

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        split.setResizeWeight(0.405);
        split.setDividerSize(0);
        split.setEnabled(false);
        split.setContinuousLayout(true);

        JPanel topPanel = buildTopPanel();

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.405);
        split.setDividerSize(0);
        split.setEnabled(false);
        split.setContinuousLayout(true);

        JPanel left  = buildLeftPanel();
        JPanel right = buildRightPanel();

        split.setLeftComponent(left);
        split.setRightComponent(right);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH); // barra superior arriba
        mainPanel.add(split, BorderLayout.CENTER);

        setContentPane(mainPanel);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setVisible(true);

        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.405));

    }

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(95, 120, 160));  // Azul oscuro
        top.setPreferredSize(new Dimension(0, 60));  // altura fija 60 px
        top.setBorder(new EmptyBorder(15, 20, 0, 20));

        JLabel title = new JLabel("SYNERGIES ALGORITHM", SwingConstants.LEFT);
        title.setFont(new Font("Times New Roman", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        top.add(title, BorderLayout.WEST);

        // ---- contenedor derecha con Back + Exit ----
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        saveBtn = new asistencias.RoundedButton("Save Graphs");
        saveBtn.setPreferredSize(new Dimension(190, 40));
        saveBtn.setVisible(false);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3, true));
        saveBtn.setBackground(new Color(142, 167, 209, 255));
        saveBtn.addActionListener(e -> asistencias.saveGraphsWithFileDialog(charts, W, H, muscles.getNames(), 1200, 800));
        right.add(saveBtn);

        // BACK (oculto al inicio)
        backBtn = new asistencias.RoundedButton("← Back");
        backBtn.setPreferredSize(new Dimension(110, 40));
        backBtn.setVisible(false);
        backBtn.setBackground(Color.WHITE);
        backBtn.addActionListener(e -> restoreSplitView());
        right.add(backBtn);

        // EXIT
        JButton exitBtn = new asistencias.RoundedButton("Exit");
        exitBtn.setBackground(Color.WHITE);
        exitBtn.setPreferredSize(new Dimension(100, 40));
        exitBtn.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "<html><b>No progress will be saved</b></html>",
                    "ARE YOU SURE YOU WANT TO EXIT?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (option == JOptionPane.YES_OPTION) System.exit(0);
        });
        right.add(exitBtn);

        top.add(right, BorderLayout.EAST);
        return top;

    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(new Color(182, 202, 230));

        // ===== INTRO (NORTH) =====
        JLabel intro = new JLabel(
                "<html>"
                        + "<div style='font-family:Times New Roman; font-size:14px; text-align:justify;'>"
                        + "This tool analyzes EMG recordings and extracts muscle synergies using the "
                        + "<b>NMF (Non-Negative Matrix Factorization)</b> algorithm."
                        + "</div>"
                        + "</html>"
        );
        intro.setForeground(new Color(40, 56, 80));
        // márgenes simétricos izquierda/derecha
        intro.setBorder(BorderFactory.createEmptyBorder(30, 20, 0, 20));
        left.add(intro, BorderLayout.NORTH);

        // ===== TEXTO (CENTER) =====
        JLabel text = new JLabel(
                "<html>"
                        + "<div style='font-family:Times New Roman; font-size:12px; text-align:justify;'>"

                        + "<b>1. Upload an EMG file</b><br>"
                        + "Supported formats include <i>.txt</i>, <i>.csv</i>, <i>.xlsx</i> and <i>.emt</i>.<br>"
                        + "Please ensure that the parameters requested by the program match the structure of your file.<br>"
                        + "You may choose between upper or lower limb.<br> "
                        + "Please introduce the muscles in the correct order, corresponding to the columns in your EMG file.<br>"
                        + "<br>"

                        + "<b>2. Optional: Filter the signal</b><br>"
                        + "You may apply a preprocessing step that removes noise and computes the EMG envelope."
                        + "<br><br>"

                        + "<b>3. Compute synergies</b><br>"
                        + "The algorithm extracts both <b>temporal activations</b> and <b>spatial synergy vectors</b> using NMF."
                        + "<br><br>"

                        + "<b>4. Visualize & download results</b><br>"
                        + "All extracted synergies and activations are displayed graphically. You may download:"
                        + "<ul style='margin-left:15px;'>"
                        + "<li>All plots (PNG format)</li>"
                        + "<li>A data file containing muscle names and activation matrices</li>"
                        + "</ul>"
                        + "<br>"

                        + "<b>Limits</b><br>"
                        + "The number of extractable synergies ranges from <b>1 to 8</b>."
                        + "<br><br>"

                        + "<i style='color:gray;'>Tip:</i> For best results, ensure your EMG data is properly normalized and "
                        + "recorded at an adequate sampling rate."
                        + "<br><br>"

                        + "</div>"
                        + "</html>"
        );
        text.setForeground(new Color(40, 56, 80));
        text.setBorder(BorderFactory.createEmptyBorder(0, 40, 10, 40));
        left.add(text, BorderLayout.CENTER);

        // ===== LOGO + CRÉDITOS (SOUTH) =====
        ImageIcon icon = new ImageIcon("src/images/Logo-positivo.png");

        Image scaled = icon.getImage().getScaledInstance(80, -1, Image.SCALE_SMOOTH);
        ImageIcon smallIcon = new ImageIcon(scaled);

        JLabel imageLabel = new JLabel(smallIcon);

        JLabel credits = new JLabel(
                "<html><div style='font-family:Times New Roman;font-size:10px; color:#333;'>Carlota Laveron Vilas</div></html>"
        );

// PANEL BOTTOM
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(182, 202, 230));
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

// logo centrado
        JPanel logoCenter = new JPanel();
        logoCenter.setBackground(new Color(182, 202, 230));
        logoCenter.add(imageLabel);
        bottom.add(logoCenter, BorderLayout.CENTER);

// texto debajo, centrado
        JPanel creditsPanel = new JPanel();
        creditsPanel.setBackground(new Color(182, 202, 230));
        creditsPanel.add(credits);
        bottom.add(creditsPanel, BorderLayout.SOUTH);

// lo añadimos ABAJO DEL PANEL PRINCIPAL
        left.add(bottom, BorderLayout.SOUTH);


        return left;

    }



    private JPanel statusBar;
    private JPanel buildRightPanel() {
        Color BG = new Color(220, 230, 245);
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BG);
        right.setBorder(new EmptyBorder(20, 20, 5, 20)); // ajusta el top si quieres más/menos margen

        // ---- Fila: "Select .pdf file" + botón ----
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 30));
        fileRow.setOpaque(false);

        fileRow.setOpaque(false);

        JLabel label = new JLabel("Select EGM recording file:");
        label.setFont(new Font("Century Gothic", Font.PLAIN, 20));
        fileRow.add(label);

        JButton fileBtn = new asistencias.RoundedButton("Select file...");
        fileBtn.setBackground(Color.WHITE);
        fileBtn.setPreferredSize(new Dimension(150, 40));
        fileBtn.addActionListener(e -> chooseFile());
        fileRow.add(fileBtn);

        // ---- Barra/rectángulo de estado (debajo de la fila) ----
        selectedFileLabel = new JLabel("No file selected", SwingConstants.CENTER);
        selectedFileLabel.setFont(new Font("Century Gothic", Font.BOLD, 14));
        selectedFileLabel.setForeground(new Color(0, 90, 0));  // verde oscuro por defecto

        statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(200, 240, 200));     // verde clarito
        //statusBar.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        statusBar.setBorder(new EmptyBorder(7, 10, 7, 10));
        statusBar.add(selectedFileLabel, BorderLayout.CENTER);
        statusBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        statusBar.setVisible(false);

        // ---- Contenedor vertical (fila + barra) en la parte superior ----
        JPanel topBox = new JPanel();
        topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
        topBox.setOpaque(false);
        topBox.add(fileRow);
        topBox.add(Box.createVerticalStrut(8));                // pequeño espacio
        topBox.add(statusBar);

        topBox.add(Box.createVerticalStrut(10));
        topBox.add(createFilterEmgSection());

        // ---- Zona de configuración (debajo de la barra) ----
        centerArea = new JPanel(new BorderLayout());
        centerArea.setOpaque(false);

        // ===== NUEVO: hacer scrolleable SOLO el panel derecho =====
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(topBox);
        content.add(Box.createVerticalStrut(12));
        content.add(centerArea);

        JScrollPane scroll = new JScrollPane(
                content,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18); // velocidad del scroll
        right.add(scroll, BorderLayout.CENTER);

        return right;
    }

    private JTextField fsField;      // sampleFrequency
    private JTextField rangeField;   // rangeSeconds
    private JTextField minFreqField; // minEmgFreq  (High-pass)
    private JTextField maxFreqField;
    private JCheckBox highPassCheck;

    private JPanel createFilterEmgSection() {
        Dimension labelSize = new Dimension(150, 25);

        // ----- Checkbox -----
        filterEmgCheck = new JCheckBox("Filter EMG");
        filterEmgCheck.setFont(new Font("Century Gothic", Font.PLAIN, 17));
        filterEmgCheck.setOpaque(false);

        JPanel filterCheckRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        filterCheckRow.setOpaque(false);
        filterCheckRow.add(filterEmgCheck);

        // ----- Panel ocultable con filtros -----
        emgFilterPanel = new JPanel(new GridLayout(6, 2, 10, 8));
        emgFilterPanel.setOpaque(false);
        emgFilterPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        emgFilterPanel.setVisible(false);   // CERRADO por defecto

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel fsLabel = new JLabel("Sampling frequency (Hz):");
        fsLabel.setPreferredSize(labelSize);
        fsLabel.setFont(new Font("Century Gothic", Font.PLAIN, 16));

        fsField = new JTextField("1000", 10);

        gbc.gridx = 0; gbc.gridy = 0;
        emgFilterPanel.add(fsLabel, gbc);
        gbc.gridx = 1;
        emgFilterPanel.add(fsField, gbc);

        // ===== rangeSeconds =====
        JLabel rangeLabel = new JLabel("Window range (s):");
        rangeLabel.setPreferredSize(labelSize);
        rangeLabel.setFont(new Font("Century Gothic", Font.PLAIN, 16));

        rangeField = new JTextField("0.2", 10);

        gbc.gridx = 0; gbc.gridy = 1;
        emgFilterPanel.add(rangeLabel, gbc);
        gbc.gridx = 1;
        emgFilterPanel.add(rangeField, gbc);

        // ===== minEmgFreq (High-pass) =====
        JLabel minLabel = new JLabel("Minimum frequency (Hz):");
        minLabel.setPreferredSize(labelSize);
        minLabel.setFont(new Font("Century Gothic", Font.PLAIN, 16));

        minFreqField = new JTextField("40", 10);

        gbc.gridx = 0; gbc.gridy = 2;
        emgFilterPanel.add(minLabel, gbc);
        gbc.gridx = 1;
        emgFilterPanel.add(minFreqField, gbc);

        // ===== maxEmgFreq (Low-pass) =====
        JLabel maxLabel = new JLabel("Maximum frequency:");
        maxLabel.setPreferredSize(labelSize);
        maxLabel.setFont(new Font("Century Gothic", Font.PLAIN, 16));
        maxFreqField = new JTextField("150", 10);

        gbc.gridx = 0; gbc.gridy = 3;
        emgFilterPanel.add(maxLabel, gbc);
        gbc.gridx = 1;
        emgFilterPanel.add(maxFreqField, gbc);

        JLabel highPassLabel = new JLabel("Enable high-pass:");
        highPassLabel.setPreferredSize(labelSize);
        highPassLabel.setFont(new Font("Century Gothic", Font.PLAIN, 16));
        highPassCheck = new JCheckBox();
        highPassCheck.setOpaque(false);
        highPassCheck.setSelected(true);  // default ON
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        emgFilterPanel.add(highPassLabel, gbc);
        gbc.gridx = 1;
        emgFilterPanel.add(highPassCheck, gbc);

        // Listener que llama a toggleFilterPanel()
        filterEmgCheck.addItemListener(e -> {
            filterEMG = filterEmgCheck.isSelected();
            toggleFilterPanel();
        });

        // Este contenedor devolveremos, con checkbox + panel ocultable debajo
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        wrapper.add(filterCheckRow);
        wrapper.add(emgFilterPanel);
        wrapper.setBorder(new EmptyBorder(0, 20, 0, 60));

        return wrapper;
    }

    private void toggleFilterPanel() {
        boolean show = filterEmgCheck.isSelected();
        emgFilterPanel.setVisible(show);
        emgFilterPanel.revalidate();
        emgFilterPanel.repaint();
    }


    private void chooseFile() {
        FileDialog fd = new FileDialog(this, "Select EGM recording file", FileDialog.LOAD);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String name = fd.getFile();
        if (dir == null || name == null) return;

        File f = new File(dir, name);

        if (!name.toLowerCase().endsWith(".csv") && !name.toLowerCase().endsWith(".txt")
        && !name.toLowerCase().endsWith(".xlsx") && !name.toLowerCase().endsWith(".emt")) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,"Accepted formats include .csv, .txt, .xlsx, .emt", "Swing Tester", JOptionPane.ERROR_MESSAGE);
            resetMuscleConfig();
            return;
        }

        if (name.toLowerCase().endsWith(".csv") || name.toLowerCase().endsWith(".txt")) {
            JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
            panel.add(new JLabel("Column delimiter:"));
            JComboBox<String> delimBox = new JComboBox<>(new String[]{";", ",", "Tab", "Space"});
            panel.add(delimBox);

            panel.add(new JLabel("Decimal separator:"));
            JComboBox<String> decimalBox = new JComboBox<>(new String[]{".", ","});
            panel.add(decimalBox);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "File format options",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                JOptionPane.showMessageDialog(this, "Operation cancelled.", "Cancelled", JOptionPane.WARNING_MESSAGE);
                resetMuscleConfig();
                return;
            }

            selectedDelim = Objects.requireNonNull(delimBox.getSelectedItem()).toString();
            selectedDecimal = Objects.requireNonNull(decimalBox.getSelectedItem()).toString();

            //System.out.println("Selected delimiter: " + selectedDelim);
            //System.out.println("Selected decimal: " + selectedDecimal);
        } else if (name.toLowerCase().endsWith(".emt")) {
            JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
            /*panel.add(new JLabel("Column delimiter:"));
            JComboBox<String> delimBox = new JComboBox<>(new String[]{";", ",", "Tab", "Space"});
            panel.add(delimBox);*/

            panel.add(new JLabel("Decimal separator:"));
            JComboBox<String> decimalBox = new JComboBox<>(new String[]{".", ","});
            panel.add(decimalBox);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "File format options",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                JOptionPane.showMessageDialog(this, "Operation cancelled.", "Cancelled", JOptionPane.WARNING_MESSAGE);
                resetMuscleConfig();
                return;
            }
            selectedDecimal = Objects.requireNonNull(decimalBox.getSelectedItem()).toString();

            //System.out.println("Selected delimiter: " + selectedDelim);
            //System.out.println("Selected decimal: " + selectedDecimal);
        }



        selectedFile = f;
        selectedFileLabel.setText("Selected: " + selectedFile.getName());
        selectedFileLabel.setFont(new Font("Century Gothic", Font.BOLD, 15));
        selectedFileLabel.setForeground(new Color(0,90,0));
        statusBar.setBackground(new Color(200,240,200));
        statusBar.setVisible(true);

        showMuscleConfig();
        updateSaveButtonState();
    }

    private void resetMuscleConfig() {
        muscleCombos.clear();
        if (musclesPanel != null) musclesPanel.removeAll();
        if (centerArea != null) {
            centerArea.removeAll();
            centerArea.revalidate();
            centerArea.repaint();
        }
        configPanel = null;
        updateSaveButtonState();
    }

    private void showMuscleConfig() {
        if (configPanel == null)
            configPanel = buildConfigPanel();
        else {
            buildMuscleFields((int) channelsSpinner.getValue());
            hookComboListeners();
            refreshMuscleOptions();
        }

        centerArea.removeAll();
        centerArea.add(configPanel, BorderLayout.NORTH);
        centerArea.revalidate();
        centerArea.repaint();
    }

    private int sampleFrequency;
    private double rangeSeconds;
    private int minEmgFreq;
    private int maxEmgFreq;
    private boolean highPassOn;
    private JPanel buildConfigPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(25, 20, 150, 20));

        // Channels
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT,18,10));
        row1.setOpaque(false);

        JLabel lblCh = new JLabel("Channels");
        lblCh.setFont(new Font("Century Gothic", Font.PLAIN, 20));
        row1.add(lblCh);

        channelsSpinner = new JSpinner(new SpinnerNumberModel(4, 4, 32, 1));
        JSpinner.DefaultEditor ed = (JSpinner.DefaultEditor) channelsSpinner.getEditor();
        ed.getTextField().setFont(new Font("Century Gothic", Font.PLAIN, 17));
        row1.add(channelsSpinner);
        p.add(row1);

        JLabel lblRegion = new JLabel("Region to be analyzed");
        lblRegion.setBorder(new EmptyBorder(0, 240, 0, 0));
        lblRegion.setFont(new Font("Century Gothic", Font.PLAIN, 17));
        row1.add(lblRegion);

        limbCombo = new JComboBox<>(new String[]{"Upper limb", "Lower limb"});
        limbCombo.setFont(new Font("Century Gothic", Font.PLAIN, 15));
        limbCombo.setSelectedIndex(0); // por defecto: Upper
        row1.add(limbCombo);

        JLabel lblM = new JLabel("Muscles:");
        lblM.setFont(new Font("Century Gothic", Font.BOLD, 18));
        lblM.setBorder(new EmptyBorder(10,0,5,0));
        p.add(lblM);

        // Panel muscles
        musclesPanel = new JPanel(new GridBagLayout());
        musclesPanel.setOpaque(false);
        p.add(musclesPanel);

        saveButton = new JButton("NEXT");
        saveButton.setFont(new Font("Century Gothic", Font.BOLD, 18));
        saveButton.setPreferredSize(new Dimension(160,45));
        saveButton.addActionListener(e -> {
            try {
                sampleFrequency = Integer.parseInt(fsField.getText().trim());
                rangeSeconds = Double.parseDouble(rangeField.getText().trim());
                minEmgFreq = Integer.parseInt(minFreqField.getText().trim());
                maxEmgFreq = Integer.parseInt(maxFreqField.getText().trim());
                highPassOn = highPassCheck.isSelected();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter valid numeric values in EMG filter fields.",
                        "Invalid Processing Values",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            matrix_config = asistencias.askHeaderRows(this);
            if (matrix_config == null) {
                // Cancelado por el usuario → no hacemos nada
                return;
            }
            muscles = asistencias.saveMuscles(muscleCombos);
            //System.out.println(muscles.toString());
            X_og = asistencias.readFile(selectedFile, matrix_config, muscles, selectedDelim, selectedDecimal);
            System.out.println(X_og.length);
            System.out.println(X_og[0].length);
            if (X_og == null || X_og.length == 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a file with values.",
                        "Empty EMG file",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            X_input = X_og;
            try {
                if(filterEMG == true){
                    System.out.println("Filtering");
                    X_input = EmgNative.filterAllChannels(X_og, sampleFrequency, rangeSeconds, minEmgFreq, maxEmgFreq, highPassOn);
                }
                TwoMatrixes WandH = diver.default_diver(X_input);
                EmgPlotApp.show(X_og, X_input);
                H = WandH.getH();
                W = WandH.getW();
                //System.out.println("\nW");
                //matrix.printMatrix(W);
                //System.out.println("\nH");
                //matrix.printMatrix(H);
                showFullScreenPanel(H, W);
                saveButton.setEnabled(false);

            } catch (Exception ex) {
                ex.printStackTrace(); // opcional: para la consola
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Problem arrised when computing the matrixes.\nMake sure parameters align with file.");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                });
            }

        });
        saveButton.setEnabled(false);

        JPanel rowSave = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rowSave.setOpaque(false);
        rowSave.add(saveButton);

        p.add(rowSave);

        // initial
        buildMuscleFields(4);
        hookComboListeners();
        updateSaveButtonState();

        channelsSpinner.addChangeListener(e->{
            int n = (int) channelsSpinner.getValue();
            if (n < 4) { channelsSpinner.setValue(4); n = 4; }
            buildMuscleFields(n);
            hookComboListeners();
            updateSaveButtonState();
        });
        limbCombo.addActionListener(e -> {
            selectedRegion = (String) limbCombo.getSelectedItem();
            System.out.println("Selected region: " + selectedRegion);
            refreshMuscleOptions();

        });

        return p;
    }

    private JPanel fullContent;
    private graphs chartsBoard;

    private void showFullScreenPanel(double [][] H, double [][] W) {
        if (fullPanel == null) {
            fullPanel = new JPanel(new BorderLayout());
            fullPanel.setBackground(Color.WHITE);

            // --- thin gray strip UNDER the top bar ---
            JPanel Strip = new JPanel();
            Strip.setBackground(Color.white);
            Strip.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            Strip.setPreferredSize(new Dimension(0, 200));

            double[][] WH = matrix.multiplication(W, H);
            double sse = asistencias.frobNormSq(asistencias.sub(X_input, WH));   // ||X - WH||²
            double x2  = asistencias.frobNormSq(X_og);
            double R2 = 1.0 - (sse / x2);
            String information = String.format("""
                <html>
                <b>Final SSE (Sum of Squared Errors)</b> — total reconstruction error between the original matrix X and the approximation W·H.<br>
                <i>Best models have SSE ≈ 0. </i><br>
                SSE = <b>%.4g</b><br><br>
                
                <b>R&sup2; (Goodness of the reconstruction)</b> — how much of the dataset variance is captured by the synergies:<br>
                <i>R² = 1 − ( SSE / ‖X‖² )</i><br>
                <i>>0.90: excellent, 0.85-90: very good, 0.80–0.85: usable, &lt;0.80: weak model.</i><br>
                R² = <b>%.4f</b>
                </html>
                """, sse, R2);

            JLabel label = new JLabel(information, SwingConstants.LEFT);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setForeground(Color.DARK_GRAY);
            label.setFont(new Font("Century Gothic", Font.PLAIN, 16));
            //label.setHorizontalAlignment(SwingConstants.LEFT); // asegura alineación izquierda
            label.setBorder(BorderFactory.createEmptyBorder(13, 10, 0, 0));
            Strip.add(label, BorderLayout.WEST);

            fullContent = new JPanel(new BorderLayout());
            fullContent.setBackground(Color.WHITE);

            fullPanel.add(Strip, BorderLayout.NORTH);
            fullPanel.add(fullContent, BorderLayout.CENTER);
        }

        if (chartsBoard == null) {
            chartsBoard = new graphs();
            fullContent.add(chartsBoard.getView(), BorderLayout.CENTER); // <-- incrustado aquí
            charts = graphs.nmfCharts(W, H, muscles.getNames());
            chartsBoard.mountGridCharts(charts);
        }

        // keep your existing Back button behavior in the TOP BAR
        if (backBtn != null) backBtn.setVisible(true);
        if (saveBtn != null) saveBtn.setVisible(true);

        Container content = getContentPane();
        if (split != null) content.remove(split);
        content.add(fullPanel, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    private void restoreSplitView() {
        if (split == null) {
            split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    buildLeftPanel(), buildRightPanel());
            split.setResizeWeight(0.4);
            split.setDividerSize(0);
            split.setEnabled(false);
            split.setContinuousLayout(true);
        }
        if (backBtn != null) backBtn.setVisible(false);


        Container content = getContentPane();
        if (fullPanel != null) content.remove(fullPanel);  // elimina panel fullscreen
        fullPanel = null;                                  // importante

        content.add(split, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();

        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.40));

    }

    private final Font UI_FONT = new Font("Century Gothic", Font.PLAIN, 18);
    private volatile boolean updating = false;


    private void buildMuscleFields(int count) {
        muscleCombos.clear();
        musclesPanel.removeAll();

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(10, -10, 10, 10);
        gc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < count; i++) {
            gc.gridx = 0; gc.gridy = i;
            JLabel idx = new JLabel((i + 1) + ".");
            idx.setFont(UI_FONT);
            musclesPanel.add(idx, gc);

            gc.gridx = 1;
            JComboBox<String> combo = new JComboBox<>();
            combo.setPrototypeDisplayValue(PLACEHOLDER);
            combo.setFont(UI_FONT);
            combo.setPreferredSize(new Dimension(300, 36));
            musclesPanel.add(combo, gc);

            muscleCombos.add(combo);
        }

        refreshMuscleOptions();
        hookComboListeners();

        musclesPanel.revalidate();
        musclesPanel.repaint();
    }

    private void hookComboListeners() {
        muscleCombos.forEach(cb -> {
            for (var al : cb.getActionListeners()) cb.removeActionListener(al);
            cb.addActionListener(e -> {
                if (updating) return;
                refreshMuscleOptions();
                updateSaveButtonState();
            });
        });
    }

    private void refreshMuscleOptions() {
        updating = true;

        List<String> source = (limbCombo.getSelectedIndex() == 0)
                ? MUSCLES_UpperLimb
                : MUSCLES_LowerLimb;

        // Qué valores ya están elegidos en otros combos
        Set<String> chosen = new HashSet<>();
        for (JComboBox<String> cb : muscleCombos) {
            Object s = cb.getSelectedItem();
            if (s != null && !PLACEHOLDER.equals(s) && source.contains(s.toString())) {
                chosen.add(s.toString());
            }
        }

        // Actualiza cada combo solo si realmente cambia algo
        for (JComboBox<String> cb : muscleCombos) {
            Object cur = cb.getSelectedItem();
            String curStr = (cur == null) ? null : cur.toString();

            List<String> options = new ArrayList<>(1 + source.size());
            options.add(PLACEHOLDER);
            for (String m : source) {
                if (!chosen.contains(m) || m.equals(curStr)) options.add(m);
            }

            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) cb.getModel();
            if (!modelMatches(model, options)) {
                // reconstruimos el modelo solo cuando difiere
                DefaultComboBoxModel<String> newModel = new DefaultComboBoxModel<>(options.toArray(new String[0]));
                cb.setModel(newModel);
            }

            String target = (curStr != null && source.contains(curStr)) ? curStr : PLACEHOLDER;
            Object selected = cb.getSelectedItem();
            if (!Objects.equals(selected, target)) {
                cb.setSelectedItem(target);   // evita setSelectedItem redundante
            }
        }

        updating = false;
    }

    // Compara contenido del modelo con la lista calculada (para evitar cambios inútiles)
    private boolean modelMatches(DefaultComboBoxModel<String> model, List<String> options) {
        if (model.getSize() != options.size()) return false;
        for (int i = 0; i < options.size(); i++) {
            if (!Objects.equals(model.getElementAt(i), options.get(i))) return false;
        }
        return true;
    }


    private void updateSaveButtonState() {
        boolean ok = selectedFile != null;
        for (JComboBox<String> cb : muscleCombos){
            Object s = cb.getSelectedItem();
            if (s == null || PLACEHOLDER.equals(s)) { ok = false; break; }
        }
        if (saveButton != null) saveButton.setEnabled(ok);
    }
}
