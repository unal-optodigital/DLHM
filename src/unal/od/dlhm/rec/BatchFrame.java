/*
 * Copyright 2017 Universidad Nacional de Colombia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unal.od.dlhm.rec;

import java.awt.Component;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import unal.od.dlhm.PreferencesKeys;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class BatchFrame extends javax.swing.JFrame implements PreferencesKeys {

    private static final String TITLE = "Batch Reconstruction";

    //
    private final BatchWorker worker;
    private final ParametersVerifier verifier;

    private float fromUser, fromUm;
    private float toUser, toUm;
    private float stepUser, stepUm;

    private int planes;

    private boolean fromSet;
    private boolean toSet;
    private boolean incrementsSet;

    //preferences
    private String fromString;
    private String toString;
    private String incrementsString;

    private String reconstructionUnits;
    private boolean isStep;
    private int maxPlanes;

    //formatter
    private final DecimalFormat df;

    private final Preferences pref;

    private final ReconstructionFrame parent;

    /**
     * Creates new form BatchFrame.
     *
     * @param parent
     * @param worker
     */
    public BatchFrame(ReconstructionFrame parent, BatchWorker worker) {
        df = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
        pref = Preferences.userNodeForPackage(getClass());

        this.worker = worker;
        verifier = new ParametersVerifier();

        this.parent = parent;

        loadPrefs();

        setLocationRelativeTo(parent);
        initComponents();
    }

    private void savePrefs() {
        pref.putFloat(REC_BATCH_START, fromUm);
        pref.putFloat(REC_BATCH_END, toUm);

        if (isStep) {
            pref.putFloat(REC_BATCH_STEP, stepUm);
        } else {
            pref.putInt(REC_BATCH_PLANES, planes);
        }
    }

    private void loadPrefs() {
        reconstructionUnits = pref.get(REC_RECONSTRUCTION_DISTANCE_UNITS, "um");
        isStep = pref.getBoolean(REC_IS_STEP, true);
        maxPlanes = pref.getInt(REC_MAX_PLANES, 10);

        fromUm = pref.getFloat(REC_BATCH_START, Float.NaN);
        if (Float.isNaN(fromUm)) {
            fromSet = false;
            fromString = "";
        } else {
            fromSet = true;
            fromUser = umToUnits(fromUm);
            fromString = df.format(fromUser);
        }

        toUm = pref.getFloat(REC_BATCH_END, Float.NaN);
        if (Float.isNaN(toUm)) {
            toSet = false;
            toString = "";
        } else {
            toSet = true;
            toUser = umToUnits(toUm);
            toString = df.format(toUser);
        }

        if (isStep) {
            stepUm = pref.getFloat(REC_BATCH_STEP, Float.NaN);
            if (Float.isNaN(toUm)) {
                incrementsSet = false;
                incrementsString = "";
            } else {
                incrementsSet = true;
                stepUser = umToUnits(stepUm);
                incrementsString = df.format(stepUser);

                BigDecimal toBD = new BigDecimal(toUser);
                BigDecimal difference = toBD.subtract(new BigDecimal(fromUser));
                BigDecimal result = difference.divide(new BigDecimal(stepUser),
                        5, RoundingMode.HALF_UP);

                planes = result.intValue() + 1;
            }
        } else {
            planes = pref.getInt(REC_BATCH_PLANES, -1);
            if (planes == -1) {
                incrementsSet = false;
                incrementsString = "";
            } else {
                incrementsSet = true;
                incrementsString = df.format(planes);
            }
        }
    }

    /**
     * Helper method to convert from {units} to um.
     *
     * @param val
     * @param units
     * @return
     */
    private float unitsToUm(float val, String units) {
        if (units.equals("nm")) {
            return val * 1E-3f;
        } else if (units.equals("mm")) {
            return val * 1E3f;
        } else if (units.equals("cm")) {
            return val * 1E4f;
        } else if (units.equals("m")) {
            return val * 1E6f;
        }

        return val;
    }

    private float umToUnits(float n) {

        if (reconstructionUnits.equals("nm")) {
            return n * 1E3f;
        } else if (reconstructionUnits.equals("mm")) {
            return n * 1E-3f;
        } else if (reconstructionUnits.equals("cm")) {
            return n * 1E-4f;
        } else if (reconstructionUnits.equals("m")) {
            return n * 1E-6f;
        }

        return n;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        inputPanel = new javax.swing.JPanel();
        toLabel = new javax.swing.JLabel();
        fromLabel = new javax.swing.JLabel();
        fromField = new javax.swing.JTextField();
        toField = new javax.swing.JTextField();
        incrementsField = new javax.swing.JTextField();
        incrementLabel = new javax.swing.JLabel();
        btnsPanel = new javax.swing.JPanel();
        okBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(TITLE);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
        setResizable(false);

        toLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        toLabel.setText("To [" + reconstructionUnits +"]:");
        toLabel.setMaximumSize(new java.awt.Dimension(55, 14));
        toLabel.setMinimumSize(new java.awt.Dimension(55, 14));
        toLabel.setPreferredSize(new java.awt.Dimension(55, 14));

        fromLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        fromLabel.setText("From [" + reconstructionUnits +"]:");
        fromLabel.setMaximumSize(new java.awt.Dimension(55, 14));
        fromLabel.setPreferredSize(new java.awt.Dimension(55, 14));

        fromField.setColumns(7);
        fromField.setText(fromString);
        fromField.setInputVerifier(verifier);
        fromField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        toField.setColumns(7);
        toField.setText(toString);
        toField.setInputVerifier(verifier);
        toField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        incrementsField.setColumns(7);
        incrementsField.setText(incrementsString);
        incrementsField.setInputVerifier(verifier);
        incrementsField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        incrementLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        incrementLabel.setText(isStep ? "Step [" + reconstructionUnits +"]:" : "Planes:");
        incrementLabel.setMaximumSize(new java.awt.Dimension(55, 14));
        incrementLabel.setMinimumSize(new java.awt.Dimension(55, 14));
        incrementLabel.setPreferredSize(new java.awt.Dimension(55, 14));

        javax.swing.GroupLayout inputPanelLayout = new javax.swing.GroupLayout(inputPanel);
        inputPanel.setLayout(inputPanelLayout);
        inputPanelLayout.setHorizontalGroup(
            inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(fromLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(incrementLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(incrementsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inputPanelLayout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(fromField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(inputPanelLayout.createSequentialGroup()
                            .addComponent(toField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE)))))
        );
        inputPanelLayout.setVerticalGroup(
            inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fromLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fromField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(toLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(incrementLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(incrementsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        okBtn.setText("Ok");
        okBtn.setPreferredSize(new java.awt.Dimension(65, 23));
        okBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBtnActionPerformed(evt);
            }
        });

        cancelBtn.setText("Cancel");
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout btnsPanelLayout = new javax.swing.GroupLayout(btnsPanel);
        btnsPanel.setLayout(btnsPanelLayout);
        btnsPanelLayout.setHorizontalGroup(
            btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnsPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(okBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelBtn))
        );
        btnsPanelLayout.setVerticalGroup(
            btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnsPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cancelBtn)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(inputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(inputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okBtnActionPerformed
        boolean advance = fromSet && toSet && incrementsSet;

        if (!advance) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Please check the input"
                    + " parameters.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isStep) {

            if ((toUser > fromUser) && (stepUser < 0)) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Given the starting and "
                        + "ending distances, the step value must be positive.",
                        "Error", JOptionPane.ERROR_MESSAGE);

                incrementsField.setText(df.format(-stepUser));
                incrementsField.requestFocusInWindow();
                return;
            }

            if ((toUser < fromUser) && (stepUser > 0)) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Given the starting and "
                        + "ending distances, the step value must be negative.",
                        "Error", JOptionPane.ERROR_MESSAGE);

                incrementsField.setText(df.format(-stepUser));
                incrementsField.requestFocusInWindow();
                return;
            }

            if (Math.abs(stepUser) > Math.abs(toUser - fromUser)) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "The absolute value of the "
                        + "step distance must be less than the absolute value of"
                        + " the difference between ending and starting distances.",
                        "Error", JOptionPane.ERROR_MESSAGE);

                incrementsField.requestFocusInWindow();
                return;
            }
        }

        if (planes > maxPlanes) {
            String[] options = new String[]{"Yes", "No"};
            int n = JOptionPane.showOptionDialog(this, planes
                    + " planes are going to be reconstructed. Do you want to "
                    + "continue?", "", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

            if (n == 1) {
                return;
            }
        }

        String[] parameters = parent.getFormattedParametersBatch();
        parameters[3] = df.format(fromUser) + " " + reconstructionUnits + " to "
                + df.format(toUser) + " " + reconstructionUnits;

        if (isStep) {
            parameters[4] = "\nStep: " + df.format(stepUser) + " "
                    + reconstructionUnits;
        } else {
            parameters[4] = "\nPlanes: " + planes;
        }

        worker.setInfo(parameters, reconstructionUnits);
        worker.setDistances(fromUm, toUm, stepUm, planes);

        worker.execute();

        setVisible(false);
        savePrefs();
        dispose();
    }//GEN-LAST:event_okBtnActionPerformed

    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBtnActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_cancelBtnActionPerformed

    private void textFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_textFieldFocusGained
        JTextField field = (JTextField) evt.getComponent();
        field.selectAll();
    }//GEN-LAST:event_textFieldFocusGained

    private class ParametersVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
            Component parent = SwingUtilities.getRoot(input);

            boolean valid;

            if (input == fromField) {
                valid = checkFromField(parent);
            } else if (input == toField) {
                valid = checkToField(parent);
            } else if (input == incrementsField) {
                valid = checkIncrementsField(parent);
            } else {
                valid = true;
            }

            return valid;
        }

        private boolean checkFromField(Component parent) {
            String txt = fromField.getText();

            if (txt.isEmpty()) {
                fromSet = false;
                fromUser = Float.NaN;
                fromUm = Float.NaN;
                return true;
            }

            try {
                fromUser = Float.parseFloat(txt);
            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                fromField.selectAll();
                
                fromSet = false;
                fromUser = Float.NaN;
                fromUm = Float.NaN;
                return false;
            }

            if (toSet && (fromUser == toUser)) {
                fromSet = false;

                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(parent, "Starting and ending "
                        + "distances must be different.", "Error",
                        JOptionPane.ERROR_MESSAGE);
//                fromField.selectAll();
                fromField.setText("");

                fromUser = Float.NaN;
                fromUm = Float.NaN;
                return false;
            }

            if (isStep && toSet && incrementsSet) {
                BigDecimal toBD = new BigDecimal(toUser);
                BigDecimal difference = toBD.subtract(new BigDecimal(fromUser));
                BigDecimal result = difference.divide(new BigDecimal(stepUser),
                        5, RoundingMode.HALF_UP);

                planes = result.intValue() + 1;

                if (planes < 0) {
                    Toolkit.getDefaultToolkit().beep();
                    stepUser = -stepUser;
                    stepUm = unitsToUm(stepUser, reconstructionUnits);
                    incrementsSet = true;

                    planes = difference.divide(new BigDecimal(stepUser),
                            5, RoundingMode.HALF_UP).intValue() + 1;

                    incrementsField.setText(df.format(stepUser));
                } else if (planes < 2) {
                    Toolkit.getDefaultToolkit().beep();
                    stepUser = difference.floatValue();
                    stepUm = unitsToUm(stepUser, reconstructionUnits);
                    incrementsSet = true;

                    planes = difference.divide(new BigDecimal(stepUser),
                            5, RoundingMode.HALF_UP).intValue() + 1;

                    incrementsField.setText(df.format(stepUser));
                }
            }

            fromUm = unitsToUm(fromUser, reconstructionUnits);
            fromSet = true;
            return true;

        }

        private boolean checkToField(Component parent) {
            String txt = toField.getText();

            if (txt.isEmpty()) {
                toSet = false;
                toUser = Float.NaN;
                toUm = Float.NaN;
                return true;
            }

            try {
                toUser = Float.parseFloat(txt);
            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                toField.selectAll();
                
                toSet = false;
                toUser = Float.NaN;
                toUm = Float.NaN;
                return false;
            }

            if (fromSet && (fromUser == toUser)) {
                toSet = false;

                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(parent, "Starting and ending "
                        + "distances must be different.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                toField.selectAll();
                toField.setText("");

                toUser = Float.NaN;
                toUm = Float.NaN;
                return false;
            }

            if (isStep && fromSet && incrementsSet) {
                BigDecimal toBD = new BigDecimal(toUser);
                BigDecimal difference = toBD.subtract(new BigDecimal(fromUser));
                BigDecimal result = difference.divide(new BigDecimal(stepUser),
                        5, RoundingMode.HALF_UP);

                planes = result.intValue() + 1;

                if (planes < 0) {
                    Toolkit.getDefaultToolkit().beep();
                    stepUser = -stepUser;
                    stepUm = unitsToUm(stepUser, reconstructionUnits);
                    incrementsSet = true;

                    planes = difference.divide(new BigDecimal(stepUser),
                            5, RoundingMode.HALF_UP).intValue() + 1;

                    incrementsField.setText(df.format(stepUser));
                } else if (planes < 2) {
                    Toolkit.getDefaultToolkit().beep();
                    stepUser = difference.floatValue();
                    stepUm = unitsToUm(stepUser, reconstructionUnits);
                    incrementsSet = true;

                    planes = difference.divide(new BigDecimal(stepUser),
                            5, RoundingMode.HALF_UP).intValue() + 1;

                    incrementsField.setText(df.format(stepUser));
                }
            }

            toUm = unitsToUm(toUser, reconstructionUnits);
            toSet = true;
            return true;
        }

        private boolean checkIncrementsField(Component parent) {
            String txt = incrementsField.getText();

            if (txt.isEmpty()) {
                incrementsSet = false;
                stepUser = Float.NaN;
                stepUm = Float.NaN;
                planes = -1;
                return true;
            }

            try {
                if (isStep) {
                    stepUser = Float.parseFloat(txt);
                } else {
                    planes = Integer.parseInt(txt);
                }
            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                incrementsField.selectAll();
                
                incrementsSet = false;
                stepUser = Float.NaN;
                stepUm = Float.NaN;
                planes = -1;
                return false;
            }

            if (isStep) {
                if (stepUser == 0) {
                    incrementsSet = false;

                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(parent, "Step value must be "
                            + "different from 0.", "Error", JOptionPane.ERROR_MESSAGE);
                    incrementsField.selectAll();

                    stepUser = Float.NaN;
                    stepUm = Float.NaN;
                    return false;
                }

                //corrects the step value when it has to be negative or positive
                //also corrects it when it is greater than the difference between
                //starting and ending points
                if (fromSet && toSet) {
                    BigDecimal toBD = new BigDecimal(toUser);
                    BigDecimal difference = toBD.subtract(new BigDecimal(fromUser));
                    BigDecimal result = difference.divide(new BigDecimal(stepUser),
                            5, RoundingMode.HALF_UP);

                    planes = result.intValue() + 1;

                    if (planes < 0) {
                        Toolkit.getDefaultToolkit().beep();
                        stepUser = -stepUser;

                        planes = difference.divide(new BigDecimal(stepUser),
                                5, RoundingMode.HALF_UP).intValue() + 1;

                        incrementsField.setText(df.format(stepUser));
                    } else if (planes < 2) {
                        Toolkit.getDefaultToolkit().beep();
                        stepUser = difference.floatValue();

                        planes = difference.divide(new BigDecimal(stepUser),
                                5, RoundingMode.HALF_UP).intValue() + 1;

                        incrementsField.setText(df.format(stepUser));
                    }
                }

                stepUm = unitsToUm(stepUser, reconstructionUnits);
                incrementsSet = true;
                return true;
            } else {
                if (planes < 2) {
                    incrementsSet = false;

                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(parent, "The number of planes"
                            + " must be 2 or more.", "Error", JOptionPane.ERROR_MESSAGE);

                    incrementsField.setText("2");
                    incrementsField.selectAll();
                    planes = -1;
                    return false;
                }

                stepUser = (toUser - fromUser) / (planes - 1);
                stepUm = unitsToUm(stepUser, reconstructionUnits);

                incrementsSet = true;
                return true;
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel btnsPanel;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JTextField fromField;
    private javax.swing.JLabel fromLabel;
    private javax.swing.JLabel incrementLabel;
    private javax.swing.JTextField incrementsField;
    private javax.swing.JPanel inputPanel;
    private javax.swing.JButton okBtn;
    private javax.swing.JTextField toField;
    private javax.swing.JLabel toLabel;
    // End of variables declaration//GEN-END:variables
}
