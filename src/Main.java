import GUI.GUI;

import javax.swing.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // O si tienes FlatLaf añadido:
        // FlatLightLaf.setup();
    } catch (Exception e) {
        e.printStackTrace();
    }
    SwingUtilities.invokeLater(() -> {
        new GUI().setVisible(true);
    });

    //main

}
