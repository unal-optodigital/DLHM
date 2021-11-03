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

import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import unal.od.dlhm.PreferencesKeys;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class ReconstructionFrame extends javax.swing.JFrame implements ImageListener, PreferencesKeys {

    private static final String TITLE = "DLHM Reconstruction";
    private static final String LOG_HEADER = "Version 1.3 - November 2021";
    private static final String LOG_SEPARATOR = "\n---------------------------";

    //
    private final ParametersVerifier verifier;

    //user inputs in user units
    private float lambdaUser;
    private float zUser;
    private float lUser;
    private float inputWUser;
    private float inputHUser;
    private float outputWUser;
    private float outputHUser;

    private float stepUser;

    //user inputs converted to um
    private float lambdaUm;
    private float zUm;
    private float lUm;
    private float inputWUm;
    private float inputHUm;
    private float outputWUm;
    private float outputHUm;

    private float stepUm;

    //boolean for the user parameters
    private boolean lambdaSet;
    private boolean zSet;
    private boolean lSet;
    private boolean inputWSet;
    private boolean inputHSet;
    private boolean outputWSet;
    private boolean outputHSet;

    private boolean stepSet;

    //last used user inputs in user units
    private float lambdaUserLast;
    private float zUserLast;
    private float lUserLast;
    private float inputWUserLast;
    private float inputHUserLast;
    private float outputWUserLast;
    private float outputHUserLast;

    //last used user inputs converted to um
    private float lambdaUmLast;
    private float zUmLast;
    private float lUmLast;
    private float inputWUmLast;
    private float inputHUmLast;
    private float outputWUmLast;
    private float outputHUmLast;

    //input field dimensions, useful for output calibration
    private int M, N;

    //array for the interpolated field (useful for the +/- and batch operations)
    private float[][] interpolatedField;

    //arrays for the interpolated hologram and reference for phase reconstruction
    //(useful for the +/- and batch operations)
    private float[][] interpolatedHologram, interpolatedReference;
     
    
    //arrays with the current opened images information
    private int[] windowsId;
    private String[] titles, titles2;

    //input images titles
    private String hologramTitle;
    private String referenceTitle;

    //calibration object for the output images
    private Calibration cal;

    //formatter
    private final DecimalFormat df;

    //preferences
    private final Preferences pref;

    //frames
    private ReconstructionSettingsFrame settingsFrame = null;
    private BatchFrame batchFrame = null;

    //hasReference
    private boolean hasRef;

    // <editor-fold defaultstate="collapsed" desc="Prefs variables">
    //frame location
    private int locX;
    private int locY;

    //last parameters used
    private String lambdaString;
    private String zString;
    private String lString;
    private String inputWString;
    private String inputHString;
    private String outputWString;
    private String outputHString;
    private String stepString;

    //parameters units
    private String lambdaUnits;
    private String reconstructionUnits;
    private String sourceToScreenUnits;
    private String inputSizeUnits;
    private String outputSizeUnits;

    //cosine filter
    private boolean filteringEnabled;
    private float borderWidth;

    //contrast calculation
    private int contrastType;
    private int averageDimension;

    //last outputs used
    private boolean phaseEnabled;
    private boolean amplitudeEnabled;
    private boolean intensityEnabled;
    private boolean realEnabled;
    private boolean imaginaryEnabled;

    //log scaling options
    private boolean amplitudeLogSelected;
    private boolean intensityLogSelected;

    //8bit scaling options
    private boolean phaseByteSelected;
    private boolean amplitudeByteSelected;
    private boolean intensityByteSelected;
    private boolean realByteSelected;
    private boolean imaginaryByteSelected;

    private boolean isManual;

    private boolean relationLock;

    private boolean logWrapping;
    // </editor-fold>

    /**
     * Creates the main frame
     */
    public ReconstructionFrame() {
        //initialized objects
        df = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
        pref = Preferences.userNodeForPackage(getClass());

        verifier = new ParametersVerifier();

        //gets the current open images and load the last preferences
        getOpenedImages();
        loadPrefs();

        initComponents();

        //adds this class as ImageListener
        ImagePlus.addImageListener(this);

//        DefaultCaret caret = (DefaultCaret) log.getCaret(); //autoscroll
//        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    /**
     * Fills the arrays with the information of the open images.
     */
    private void getOpenedImages() {
        //gets the IDs of the opened images
        windowsId = WindowManager.getIDList();

        if (windowsId == null) {
            //if there are no images open, just adds <none> option
            titles = new String[]{"<none>"};
            titles2 = new String[]{"<none>"};
        } else {
            //Titles for holo input
            titles = new String[windowsId.length];
            for (int i = 0; i < windowsId.length; i++) {
                ImagePlus imp = WindowManager.getImage(windowsId[i]);
                if (imp != null) {
                    titles[i] = imp.getTitle();
                } else {
                    titles[i] = "";
                }
            }

            //titles for reference input, with <none> option
            titles2 = new String[windowsId.length + 1];
            titles2[0] = "<none>";
            for (int i = 0; i < windowsId.length; i++) {
                ImagePlus imp = WindowManager.getImage(windowsId[i]);
                if (imp != null) {
                    titles2[i + 1] = imp.getTitle();
                } else {
                    titles2[i + 1] = "";
                }
            }
        }
    }

    /**
     * Saves the preferences when the frame is closed.
     */
    private void savePrefs() {
        //frame location
        pref.putInt(REC_FRAME_LOC_X, getLocation().x);
        pref.putInt(REC_FRAME_LOC_Y, getLocation().y);

        //parameters text
        pref.putFloat(REC_LAMBDA, lambdaUser);
        pref.putFloat(REC_RECONSTRUCTION_DISTANCE, zUser);
        pref.putFloat(REC_SOURCE_TO_SCREEN_DISTANCE, lUser);
        pref.putFloat(REC_INPUT_WIDTH, inputWUser);
        pref.putFloat(REC_INPUT_HEIGHT, inputHUser);
        pref.putFloat(REC_OUTPUT_WIDTH, outputWUser);
        pref.putFloat(REC_OUTPUT_HEIGHT, outputHUser);

        pref.putFloat(REC_STEP, stepUser);

        //outputs
        pref.putBoolean(REC_PHASE_CHECKED, phaseChk.isSelected());
        pref.putBoolean(REC_AMPLITUDE_CHECKED, amplitudeChk.isSelected());
        pref.putBoolean(REC_INTENSITY_CHECKED, intensityChk.isSelected());
        pref.putBoolean(REC_REAL_CHECKED, realChk.isSelected());
        pref.putBoolean(REC_IMAGINARY_CHECKED, imaginaryChk.isSelected());

        pref.putBoolean(REC_IS_MANUAL, manualRadio.isSelected());

        pref.putBoolean(REC_RELATION_LOCK, lockBtn.isSelected());

        pref.putBoolean(REC_LOG_WRAPPING, log.getLineWrap());
    }

    /**
     * Loads the preferences when the plugin starts.
     */
    private void loadPrefs() {
        //frame location
        locX = pref.getInt(REC_FRAME_LOC_X, 300);
        locY = pref.getInt(REC_FRAME_LOC_Y, 300);

        //outputs
        phaseEnabled = pref.getBoolean(REC_PHASE_CHECKED, false);
        amplitudeEnabled = pref.getBoolean(REC_AMPLITUDE_CHECKED, false);
        intensityEnabled = pref.getBoolean(REC_INTENSITY_CHECKED, false);
        realEnabled = pref.getBoolean(REC_REAL_CHECKED, false);
        imaginaryEnabled = pref.getBoolean(REC_IMAGINARY_CHECKED, false);

        isManual = pref.getBoolean(REC_IS_MANUAL, true);

        relationLock = pref.getBoolean(REC_RELATION_LOCK, false);

        logWrapping = pref.getBoolean(REC_LOG_WRAPPING, true);

        //parameters units
        loadUnitsPrefs();

        //propagation
        loadPropagationPrefs();

        //scaling
        loadScalingPrefs();

        //parameters strings for text fields
        loadParameters();
    }

    /**
     * Loads the last used parameters.
     */
    private void loadParameters() {
        //gets the saved floats and checks for NaN values
        lambdaUser = pref.getFloat(REC_LAMBDA, Float.NaN);
        if (Float.isNaN(lambdaUser)) {
            lambdaSet = false;
            lambdaString = "";
        } else {
            lambdaSet = true;
            lambdaString = df.format(lambdaUser);
            lambdaUm = unitsToUm(lambdaUser, lambdaUnits);
        }

        zUser = pref.getFloat(REC_RECONSTRUCTION_DISTANCE, Float.NaN);
        if (Float.isNaN(zUser)) {
            zSet = false;
            zString = "";
        } else {
            zSet = true;
            zString = df.format(zUser);
            zUm = unitsToUm(zUser, reconstructionUnits);
        }

        lUser = pref.getFloat(REC_SOURCE_TO_SCREEN_DISTANCE, Float.NaN);
        if (Float.isNaN(lUser)) {
            lSet = false;
            lString = "";
        } else {
            lSet = true;
            lString = df.format(lUser);
            lUm = unitsToUm(lUser, sourceToScreenUnits);
        }

        inputWUser = pref.getFloat(REC_INPUT_WIDTH, Float.NaN);
        if (Float.isNaN(inputWUser)) {
            inputWSet = false;
            inputWString = "";
        } else {
            inputWSet = true;
            inputWString = df.format(inputWUser);
            inputWUm = unitsToUm(inputWUser, inputSizeUnits);
        }

        inputHUser = pref.getFloat(REC_INPUT_HEIGHT, Float.NaN);
        if (Float.isNaN(inputHUser)) {
            inputHSet = false;
            inputHString = "";
        } else {
            inputHSet = true;
            inputHString = df.format(inputHUser);
            inputHUm = unitsToUm(inputHUser, inputSizeUnits);
        }

        outputWUser = pref.getFloat(REC_OUTPUT_WIDTH, Float.NaN);
        if (Float.isNaN(outputWUser)) {
            outputWSet = false;
            outputWString = "";
        } else {
            outputWSet = true;
            outputWString = df.format(outputWUser);
            outputWUm = unitsToUm(outputWUser, outputSizeUnits);
        }

        outputHUser = pref.getFloat(REC_OUTPUT_HEIGHT, Float.NaN);
        if (Float.isNaN(outputHUser)) {
            outputHSet = false;
            outputHString = "";
        } else {
            outputHSet = true;
            outputHString = df.format(outputHUser);
            outputHUm = unitsToUm(outputHUser, outputSizeUnits);
        }

        stepUser = pref.getFloat(REC_STEP, Float.NaN);
        if (Float.isNaN(stepUser)) {
            stepSet = false;
            stepString = "";
        } else {
            stepSet = true;
            stepString = df.format(stepUser);
            stepUm = unitsToUm(stepUser, reconstructionUnits);
        }
    }

    /**
     * Loads the units of the parameters.
     */
    private void loadUnitsPrefs() {
        lambdaUnits = pref.get(REC_LAMBDA_UNITS, "nm");
        reconstructionUnits = pref.get(REC_RECONSTRUCTION_DISTANCE_UNITS, "um");
        sourceToScreenUnits = pref.get(REC_SOURCE_TO_SCREEN_DISTANCE_UNITS, "mm");
        inputSizeUnits = pref.get(REC_INPUT_SIZE_UNITS, "mm");
        outputSizeUnits = pref.get(REC_OUTPUT_SIZE_UNITS, "mm");
    }

    /**
     * Loads filtering and illumination options.
     */
    private void loadPropagationPrefs() {
        filteringEnabled = pref.getBoolean(REC_COSINE_FILTER, true);
        borderWidth = pref.getFloat(REC_COSINE_BORDER_WIDTH, 0.1f);

        contrastType = pref.getInt(REC_CONTRAST_TYPE, 2);
        averageDimension = pref.getInt(REC_AVERAGE_DIMENSION, 50);
    }

    /**
     * Loads scaling options.
     */
    private void loadScalingPrefs() {
        amplitudeLogSelected = pref.getBoolean(REC_AMPLITUDE_LOG, false);
        intensityLogSelected = pref.getBoolean(REC_INTENSITY_LOG, false);

        phaseByteSelected = pref.getBoolean(REC_PHASE_8_BIT, false);
        amplitudeByteSelected = pref.getBoolean(REC_AMPLITUDE_8_BIT, true);
        intensityByteSelected = pref.getBoolean(REC_INTENSITY_8_BIT, true);
        realByteSelected = pref.getBoolean(REC_REAL_8_BIT, false);
        imaginaryByteSelected = pref.getBoolean(REC_IMAGINARY_8_BIT, false);
    }

    /**
     * Updates units labels.
     */
    public void updateUnitsPrefs() {
        loadUnitsPrefs();

        lambdaLabel.setText("Wavelength [" + lambdaUnits + "]:");
        zLabel.setText("Reconst. dist. [" + reconstructionUnits + "]:");
        lLabel.setText("So. - Sc. dist. [" + sourceToScreenUnits + "]:");
        inputWLabel.setText("Input width [" + inputSizeUnits + "]:");
        inputHLabel.setText("Input height [" + inputSizeUnits + "]:");
        outputWLabel.setText("Output width [" + outputSizeUnits + "]:");
        outputHLabel.setText("Output height [" + outputSizeUnits + "]:");
    }

    /**
     * Updates filtering and illumination options.
     */
    public void updatePropagationPrefs() {
        loadPropagationPrefs();
    }

    /**
     * Updates scaling options.
     */
    public void updateScalingPrefs() {
        loadScalingPrefs();
    }

    /**
     * Posts a message (s) on the log. If useSeparator is true prints a
     * separator before the message.
     *
     * @param useSeparator
     * @param s
     */
    public void updateLog(boolean useSeparator, String s) {
        if (useSeparator) {
            log.append(LOG_SEPARATOR);
        }
        log.append(s);
    }

    /**
     * Enables the fields after a propagation is performed. Increase and
     * decrease buttons, Step TextField and Batch button
     *
     * @param enabled
     */
    public void enableAfterPropagationOpt(boolean enabled) {
        decBtn.setEnabled(enabled);
        stepField.setEnabled(enabled);
        incBtn.setEnabled(enabled);
        batchBtn.setEnabled(enabled);
    }

//    /**
//     * Disables the buttons that can perform a propagation operation.
//     *
//     * @param enabled
//     */
//    public void enablePropagationBtns(boolean enabled) {
//        reconstructBtn.setEnabled(enabled);
//        decBtn.setEnabled(enabled);
//        incBtn.setEnabled(enabled);
//        batchBtn.setEnabled(enabled);
//    }
//
    /**
     * Returns an array containing the parameters used in the last propagation.
     *
     * @return
     */
    public String[] getFormattedParameters() {
        String[] s = new String[]{
            hologramTitle,
            referenceTitle,
            df.format(lambdaUserLast) + " " + lambdaUnits,
            df.format(zUserLast) + " " + reconstructionUnits,
            df.format(lUserLast) + " " + sourceToScreenUnits,
            df.format(inputWUserLast) + " " + inputSizeUnits,
            df.format(inputHUserLast) + " " + inputSizeUnits,
            df.format(outputWUserLast) + " " + outputSizeUnits,
            df.format(outputHUserLast) + " " + outputSizeUnits
        };

        return s;
    }

    /**
     * Returns an array containing the parameters used in the last propagation.
     *
     * @return
     */
    public String[] getFormattedParametersBatch() {
        String[] s = new String[]{
            hologramTitle,
            referenceTitle,
            df.format(lambdaUserLast) + " " + lambdaUnits,
            "",
            "",
            df.format(lUserLast) + " " + sourceToScreenUnits,
            df.format(inputWUserLast) + " " + inputSizeUnits,
            df.format(inputHUserLast) + " " + inputSizeUnits,
            df.format(outputWUserLast) + " " + outputSizeUnits,
            df.format(outputHUserLast) + " " + outputSizeUnits
        };

        return s;
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

    /**
     * Helper method to convert from um to {units}.
     *
     * @param val
     * @param units
     * @return
     */
    private float umToUnits(float val, String units) {
        if (units.equals("nm")) {
            return val * 1E3f;
        } else if (units.equals("mm")) {
            return val * 1E-3f;
        } else if (units.equals("cm")) {
            return val * 1E-4f;
        } else if (units.equals("m")) {
            return val * 1E-6f;
        }

        return val;
    }

    /**
     * Sets the input images from the user selections. Returns false if an error
     * occurs.
     *
     * @param holoIdx
     * @param refIdx
     * @return success
     */
    private boolean setInputImages(ReconstructionWorker worker, int holoIdx,
            int refIdx) {

        hologramTitle = titles[holoIdx];
        referenceTitle = titles2[refIdx];

        if (hologramTitle.equalsIgnoreCase("<none>")) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Please select at least the"
                    + " hologram input image.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        hasRef = !referenceTitle.equalsIgnoreCase("<none>");

        if (hasRef) {
            ImagePlus holoImp = WindowManager.getImage(windowsId[holoIdx]);
            ImageProcessor holoIp = holoImp.getProcessor();
            float[][] hologram = holoIp.getFloatArray();

            ImagePlus refImp = WindowManager.getImage(windowsId[refIdx - 1]);
            ImageProcessor refIp = refImp.getProcessor();
            float[][] reference = refIp.getFloatArray();

            M = holoIp.getWidth();
            N = holoIp.getHeight();

            if (M != refIp.getWidth() || N != refIp.getHeight()) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Hologram and reference"
                        + " images must have the same dimensions.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            worker.setSize(M, N);
            // PROBLEM NUM: SET_PARAMETERS NEED TO BE CALLED AFTER SET_SIZE AND BEFORE SET_HOLOGRAM
            setParameters(worker);
            worker.setHologramAndReference(hologram, reference);

        } else {
            ImagePlus holoImp = WindowManager.getImage(windowsId[holoIdx]);
            ImageProcessor holoIp = holoImp.getProcessor();
            float[][] hologram = holoIp.getFloatArray();

            M = holoIp.getWidth();
            N = holoIp.getHeight();

            worker.setSize(M, N);
            // PROBLEM NUM: SET_PARAMETERS NEED TO BE CALLED AFTER SET_SIZE AND BEFORE SET_HOLOGRAM
            setParameters(worker);
            // PROBLEM AVG: SET AVERAGE ZONE SIZE BEFORE SETTING THE HOLOGRAM
            worker.setAverageZoneSize(averageDimension);
            worker.setHologram(hologram, contrastType);

        }

        return true;
    }

    /**
     * Sets the input parameters from the user selections. Returns false if an
     * error occurs.
     *
     * @return success
     */
    private boolean setParameters(ReconstructionWorker worker) {
        boolean advance = lambdaSet && zSet && lSet && inputWSet && inputHSet;

        if (manualRadio.isSelected()) {
            advance = advance && outputWSet && outputHSet;
        }

        if (!advance) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Please check the input"
                    + " parameters.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //sets the last used parameters
        lambdaUserLast = lambdaUser;
        lambdaUmLast = lambdaUm;

        zUserLast = zUser;
        zUmLast = zUm;

        lUserLast = lUser;
        lUmLast = lUm;

        inputWUserLast = inputWUser;
        inputWUmLast = inputWUm;

        inputHUserLast = inputHUser;
        inputHUmLast = inputHUm;

        if (automaticRadio.isSelected()) {
            outputWUm = inputWUm * zUm / lUm;
            outputHUm = inputHUm * zUm / lUm;

            outputWUser = umToUnits(outputWUm, outputSizeUnits);
            outputHUser = umToUnits(outputHUm, outputSizeUnits);
        }

        outputWUserLast = outputWUser;
        outputWUmLast = outputWUm;

        outputHUserLast = outputHUser;
        outputHUmLast = outputHUm;

        //creates the calibration object for output images
        calibrate();

        //if there isn't at least one output image selected returns error
        if (!phaseEnabled && !amplitudeEnabled && !intensityEnabled
                && !realEnabled && !imaginaryEnabled) {

            JOptionPane.showMessageDialog(this, "Please select at least one "
                    + "output.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //if the cosine filter option is enabled, sets the border width
        if (filteringEnabled) {
            worker.setBorderWidth(borderWidth);
        }

        //sets the parameters and the outputs in the worker object
        worker.setParameters(lambdaUmLast, zUmLast, lUmLast, inputWUmLast,
                inputHUmLast, outputWUmLast, outputHUmLast);

        worker.setOutputs(phaseEnabled, amplitudeEnabled, intensityEnabled,
                realEnabled, imaginaryEnabled);

        //sets the scaling options
        worker.setByteScaling(phaseByteSelected, amplitudeByteSelected,
                intensityByteSelected, realByteSelected, imaginaryByteSelected);

        worker.setLogarithmicScaling(amplitudeLogSelected, intensityLogSelected);

        return true;
    }

    /**
     * Sets the input parameters from the user selections. Returns false if an
     * error occurs.
     *
     * @return success
     */
    private boolean setParametersIncAndDec(ReconstructionWorker worker,
            boolean increment) {

        boolean advance = stepSet;

        if (manualRadio.isSelected()) {
            advance = advance && outputWSet && outputHSet;

            if (!advance) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Please check the step and"
                        + " output size parameters.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        if (!advance) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Please check the step "
                    + "parameter.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //sets the last used parameters
        if (increment) {
            zUserLast += stepUser;
            zUmLast += stepUm;
        } else {
            zUserLast -= stepUser;
            zUmLast -= stepUm;
        }

        if (automaticRadio.isSelected()) {
            outputWUm = inputWUmLast * zUmLast / lUmLast;
            outputHUm = inputHUmLast * zUmLast / lUmLast;

            outputWUser = umToUnits(outputWUm, outputSizeUnits);
            outputHUser = umToUnits(outputHUm, outputSizeUnits);
        }

        outputWUserLast = outputWUser;
        outputWUmLast = outputWUm;

        outputHUserLast = outputHUser;
        outputHUmLast = outputHUm;

        //creates the calibration object for output images
        calibrate();

        //if there isn't at least one output image selected returns error
        if (!phaseEnabled && !amplitudeEnabled && !intensityEnabled
                && !realEnabled && !imaginaryEnabled) {

            JOptionPane.showMessageDialog(this, "Please select at least one "
                    + "output.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //sets the parameters and the outputs in the worker object
        worker.setParameters(lambdaUmLast, zUmLast, lUmLast, inputWUmLast,
                inputHUmLast, outputWUmLast, outputHUmLast);

        worker.setOutputs(phaseEnabled, amplitudeEnabled, intensityEnabled,
                realEnabled, imaginaryEnabled);

        //sets the scaling options
        worker.setByteScaling(phaseByteSelected, amplitudeByteSelected,
                intensityByteSelected, realByteSelected, imaginaryByteSelected);

        worker.setLogarithmicScaling(amplitudeLogSelected, intensityLogSelected);

        return true;
    }

    /**
     * Sets the input parameters from the user selections. Returns false if an
     * error occurs.
     *
     * @return success
     */
    private boolean setParametersBatch(BatchWorker worker) {

        if (manualRadio.isSelected()) {
            boolean advance = outputWSet && outputHSet;

            if (!advance) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Please check the output "
                        + "size parameters.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        //if there isn't at least one output image selected returns error
        if (!phaseEnabled && !amplitudeEnabled && !intensityEnabled
                && !realEnabled && !imaginaryEnabled) {

            JOptionPane.showMessageDialog(this, "Please select at least one "
                    + "output.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //sets the parameters and the outputs in the worker object
        worker.setParameters(lambdaUmLast, lUmLast, inputWUmLast, inputHUmLast);

        if (manualRadio.isSelected()) {
            worker.setOutputSizes(outputWUm, outputHUm);

            outputWUserLast = outputWUser;
            outputWUmLast = outputWUm;

            outputHUserLast = outputHUser;
            outputHUmLast = outputHUm;

            //creates the calibration object for output images
            calibrate();
        }

        worker.setOutputs(phaseEnabled, amplitudeEnabled, intensityEnabled,
                realEnabled, imaginaryEnabled);

        //sets the scaling options
        worker.setByteScaling(phaseByteSelected, realByteSelected,
                imaginaryByteSelected);

        worker.setLogarithmicScaling(amplitudeLogSelected, intensityLogSelected);

        return true;
    }

    /**
     * Creates the calibration object for the output images.
     *
     * @param useZ
     */
    private void calibrate() {
        float dx = inputWUmLast / M;
        float dy = inputHUmLast / N;

        float dxOut;
        float dyOut;

        cal = new Calibration();

        if (manualRadio.isSelected()) {
            dxOut = outputWUmLast / M;
            dyOut = outputHUmLast / N;
        } else {
            dxOut = dx * zUmLast / lUmLast;
            dyOut = dy * zUmLast / lUmLast;
        }

        //converts the output size, to user units
        if (outputSizeUnits.equals("nm")) {
            dxOut *= 1E3f;
            dyOut *= 1E3f;
        } else if (outputSizeUnits.equals("mm")) {
            dxOut *= 1E-3f;
            dyOut *= 1E-3f;
        } else if (outputSizeUnits.equals("cm")) {
            dxOut *= 1E-4f;
            dyOut *= 1E-4f;
        } else if (outputSizeUnits.equals("m")) {
            dxOut *= 1E-6f;
            dyOut *= 1E-6f;
        }

        cal.setUnit(outputSizeUnits);
        cal.pixelWidth = dxOut;
        cal.pixelHeight = dyOut;
    }

    /**
     * Returns the calibration object.
     *
     * @return
     */
    public Calibration getCalibration() {
        return cal;
    }

    public void setInterpolatedField(float[][] field) {
        interpolatedField = field;
    }

    public void setInterpolatedHologramAndReference(float[][] hologram, float[][] reference) {
        interpolatedHologram = hologram;
        interpolatedReference = reference;
    }

    /**
     * Listener method, updates the input combos.
     *
     * @param imp
     */
    @Override
    public void imageClosed(ImagePlus imp) {
        updateCombos();
    }

    /**
     * Listener method, updates the input combos.
     *
     * @param imp
     */
    @Override
    public void imageOpened(ImagePlus imp) {
        updateCombos();
    }

    /**
     * Listener method, updates the input combos.
     *
     * @param imp
     */
    @Override
    public void imageUpdated(ImagePlus imp) {
        updateCombos();
    }

    /**
     * Updates the information on the combos.
     */
    private void updateCombos() {
        int holoIdx = hologramCombo.getSelectedIndex();
        int refIdx = referenceCombo.getSelectedIndex();

        getOpenedImages();
        hologramCombo.setModel(new DefaultComboBoxModel<String>(titles));
        hologramCombo.setSelectedIndex((holoIdx >= titles.length)
                ? titles.length - 1 : holoIdx);

        referenceCombo.setModel(new DefaultComboBoxModel<String>(titles2));
        referenceCombo.setSelectedIndex((refIdx >= titles2.length)
                ? titles2.length - 1 : refIdx);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popup = new javax.swing.JPopupMenu();
        copyItem = new javax.swing.JMenuItem();
        copyAllItem = new javax.swing.JMenuItem();
        sep1 = new javax.swing.JPopupMenu.Separator();
        wrapItem = new javax.swing.JCheckBoxMenuItem();
        sep2 = new javax.swing.JPopupMenu.Separator();
        clearItem = new javax.swing.JMenuItem();
        outputGroup = new javax.swing.ButtonGroup();
        parametersPanel = new javax.swing.JPanel();
        hologramCombo = new javax.swing.JComboBox();
        referenceCombo = new javax.swing.JComboBox();
        lambdaField = new javax.swing.JTextField();
        zField = new javax.swing.JTextField();
        inputWField = new javax.swing.JTextField();
        inputHField = new javax.swing.JTextField();
        outputWField = new javax.swing.JTextField();
        outputHField = new javax.swing.JTextField();
        lockBtn = new javax.swing.JToggleButton();
        outputHLabel = new javax.swing.JLabel();
        outputWLabel = new javax.swing.JLabel();
        inputHLabel = new javax.swing.JLabel();
        inputWLabel = new javax.swing.JLabel();
        zLabel = new javax.swing.JLabel();
        lambdaLabel = new javax.swing.JLabel();
        referenceLabel = new javax.swing.JLabel();
        hologramLabel = new javax.swing.JLabel();
        manualRadio = new javax.swing.JRadioButton();
        automaticRadio = new javax.swing.JRadioButton();
        lLabel = new javax.swing.JLabel();
        lField = new javax.swing.JTextField();
        btnsPanel = new javax.swing.JPanel();
        settingsBtn = new javax.swing.JButton();
        batchBtn = new javax.swing.JButton();
        incBtn = new javax.swing.JButton();
        stepField = new javax.swing.JTextField();
        decBtn = new javax.swing.JButton();
        reconstructBtn = new javax.swing.JButton();
        chkPanel = new javax.swing.JPanel();
        phaseChk = new javax.swing.JCheckBox();
        amplitudeChk = new javax.swing.JCheckBox();
        intensityChk = new javax.swing.JCheckBox();
        realChk = new javax.swing.JCheckBox();
        imaginaryChk = new javax.swing.JCheckBox();
        logPane = new javax.swing.JScrollPane();
        log = new javax.swing.JTextArea();

        copyItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/page_white_copy.png"))); // NOI18N
        copyItem.setText("Copy");
        copyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyItemActionPerformed(evt);
            }
        });
        popup.add(copyItem);

        copyAllItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/page_copy.png"))); // NOI18N
        copyAllItem.setText("Copy All");
        copyAllItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyAllItemActionPerformed(evt);
            }
        });
        popup.add(copyAllItem);
        popup.add(sep1);

        wrapItem.setSelected(logWrapping);
        wrapItem.setText("Wrap");
        wrapItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wrapItemActionPerformed(evt);
            }
        });
        popup.add(wrapItem);
        popup.add(sep2);

        clearItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/page_delete.png"))); // NOI18N
        clearItem.setText("Clear");
        clearItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearItemActionPerformed(evt);
            }
        });
        popup.add(clearItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(TITLE);
        setBounds(new java.awt.Rectangle(locX, locY, 0, 0)
        );
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
        setMinimumSize(new java.awt.Dimension(545, 311));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        parametersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));
        parametersPanel.setMaximumSize(new java.awt.Dimension(255, 301));
        parametersPanel.setMinimumSize(new java.awt.Dimension(255, 301));

        hologramCombo.setModel(new DefaultComboBoxModel<String>(titles)
        );
        hologramCombo.setMaximumSize(new java.awt.Dimension(115, 20));
        hologramCombo.setMinimumSize(new java.awt.Dimension(115, 20));
        hologramCombo.setPreferredSize(new java.awt.Dimension(115, 20));

        referenceCombo.setModel(new DefaultComboBoxModel<String>(titles2));
        referenceCombo.setSelectedIndex(titles2.length > 1 ? 1 : 0);
        referenceCombo.setMaximumSize(new java.awt.Dimension(115, 20));
        referenceCombo.setMinimumSize(new java.awt.Dimension(115, 20));
        referenceCombo.setPreferredSize(new java.awt.Dimension(115, 20));

        lambdaField.setText(lambdaString);
        lambdaField.setToolTipText("Wavelength must be a positive number and different from 0.");
        lambdaField.setInputVerifier(verifier);
        lambdaField.setMaximumSize(new java.awt.Dimension(115, 20));
        lambdaField.setMinimumSize(new java.awt.Dimension(115, 20));
        lambdaField.setPreferredSize(new java.awt.Dimension(115, 20));
        lambdaField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        zField.setText(zString);
        zField.setToolTipText("Reconstruction distance must be a floating point number.");
        zField.setInputVerifier(verifier);
        zField.setMaximumSize(new java.awt.Dimension(115, 20));
        zField.setMinimumSize(new java.awt.Dimension(115, 20));
        zField.setPreferredSize(new java.awt.Dimension(115, 20));
        zField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        inputWField.setText(inputWString);
        inputWField.setToolTipText("Input width must be a positive number and different from 0.");
        inputWField.setInputVerifier(verifier);
        inputWField.setMaximumSize(new java.awt.Dimension(115, 20));
        inputWField.setMinimumSize(new java.awt.Dimension(115, 20));
        inputWField.setPreferredSize(new java.awt.Dimension(115, 20));
        inputWField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        inputHField.setText(inputHString);
        inputHField.setToolTipText("Input height must be a positive number and different from 0.");
        inputHField.setInputVerifier(verifier);
        inputHField.setMaximumSize(new java.awt.Dimension(115, 20));
        inputHField.setMinimumSize(new java.awt.Dimension(115, 20));
        inputHField.setPreferredSize(new java.awt.Dimension(115, 20));
        inputHField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        outputWField.setText(outputWString);
        outputWField.setToolTipText("Output width must be a positive number and different from 0.");
        outputWField.setEnabled(isManual);
        outputWField.setInputVerifier(verifier);
        outputWField.setMaximumSize(new java.awt.Dimension(83, 20));
        outputWField.setMinimumSize(new java.awt.Dimension(83, 20));
        outputWField.setPreferredSize(new java.awt.Dimension(83, 20));
        outputWField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        outputHField.setText(outputHString);
        outputHField.setToolTipText("Output height must be a positive number and different from 0.");
        outputHField.setEnabled(isManual);
        outputHField.setInputVerifier(verifier);
        outputHField.setMaximumSize(new java.awt.Dimension(83, 20));
        outputHField.setMinimumSize(new java.awt.Dimension(83, 20));
        outputHField.setPreferredSize(new java.awt.Dimension(83, 20));
        outputHField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        lockBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource(relationLock ? "/lock.png" : "/lock_open.png")));
        lockBtn.setSelected(relationLock);
        lockBtn.setEnabled(isManual);
        lockBtn.setMaximumSize(new java.awt.Dimension(25, 25));
        lockBtn.setMinimumSize(new java.awt.Dimension(25, 25));
        lockBtn.setPreferredSize(new java.awt.Dimension(25, 25));
        lockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockBtnActionPerformed(evt);
            }
        });

        outputHLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        outputHLabel.setText("Output height [" + outputSizeUnits + "]:");
        outputHLabel.setEnabled(isManual);
        outputHLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        outputHLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        outputHLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        outputWLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        outputWLabel.setText("Output width [" + outputSizeUnits + "]:");
        outputWLabel.setEnabled(isManual);
        outputWLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        outputWLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        outputWLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        inputHLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        inputHLabel.setText("Input height [" + inputSizeUnits + "]:");
        inputHLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        inputHLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        inputHLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        inputWLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        inputWLabel.setText("Input width [" + inputSizeUnits + "]:");
        inputWLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        inputWLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        inputWLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        zLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        zLabel.setText("Reconst. dist. [" + reconstructionUnits + "]:");
        zLabel.setToolTipText("Reconstruction distance.");
        zLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        zLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        zLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        lambdaLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lambdaLabel.setText("Wavelength [" + lambdaUnits + "]:");
        lambdaLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        lambdaLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        lambdaLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        referenceLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        referenceLabel.setText("Reference:");
        referenceLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        referenceLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        referenceLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        hologramLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        hologramLabel.setText("Hologram:");
        hologramLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        hologramLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        hologramLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        outputGroup.add(manualRadio);
        manualRadio.setSelected(isManual);
        manualRadio.setText("Manual");
        manualRadio.setNextFocusableComponent(automaticRadio);
        manualRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputSizeRadioActionPerformed(evt);
            }
        });

        outputGroup.add(automaticRadio);
        automaticRadio.setSelected(!isManual);
        automaticRadio.setText("Automatic (Geometry)");
        automaticRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputSizeRadioActionPerformed(evt);
            }
        });

        lLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lLabel.setText("So. - Sc. dist. [" + sourceToScreenUnits + "]:");
        lLabel.setToolTipText("Source to screen distance.");
        lLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        lLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        lLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        lField.setText(lString);
        lField.setToolTipText("Source to screen distance must be a positive number and different from 0.");
        lField.setInputVerifier(verifier);
        lField.setMaximumSize(new java.awt.Dimension(115, 20));
        lField.setMinimumSize(new java.awt.Dimension(115, 20));
        lField.setPreferredSize(new java.awt.Dimension(115, 20));
        lField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        javax.swing.GroupLayout parametersPanelLayout = new javax.swing.GroupLayout(parametersPanel);
        parametersPanel.setLayout(parametersPanelLayout);
        parametersPanelLayout.setHorizontalGroup(
            parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametersPanelLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(outputHLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(outputWLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(outputHField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(outputWField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(manualRadio)
                        .addGap(30, 30, 30)
                        .addComponent(automaticRadio))
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(inputHLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputWLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(zLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lambdaLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(referenceLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(hologramLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(hologramCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(referenceCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lambdaField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(zField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputWField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputHField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addComponent(lLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(5, 5, 5))
        );
        parametersPanelLayout.setVerticalGroup(
            parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametersPanelLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(hologramCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hologramLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(referenceCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(referenceLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lambdaField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lambdaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(zField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(zLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(inputWField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(inputWLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(inputHField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(inputHLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(manualRadio)
                    .addComponent(automaticRadio))
                .addGap(11, 11, 11)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(outputWLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(outputWField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(outputHLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(outputHField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(lockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(5, 5, 5))
        );

        btnsPanel.setMaximumSize(new java.awt.Dimension(270, 66));
        btnsPanel.setMinimumSize(new java.awt.Dimension(270, 66));

        settingsBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/wrench.png"))); // NOI18N
        settingsBtn.setText("Settings");
        settingsBtn.setMaximumSize(new java.awt.Dimension(132, 23));
        settingsBtn.setMinimumSize(new java.awt.Dimension(132, 23));
        settingsBtn.setPreferredSize(new java.awt.Dimension(132, 23));
        settingsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsBtnActionPerformed(evt);
            }
        });

        batchBtn.setText("Batch");
        batchBtn.setEnabled(false);
        batchBtn.setMaximumSize(new java.awt.Dimension(91, 23));
        batchBtn.setMinimumSize(new java.awt.Dimension(91, 23));
        batchBtn.setPreferredSize(new java.awt.Dimension(91, 23));
        batchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                batchBtnActionPerformed(evt);
            }
        });

        incBtn.setText("+");
        incBtn.setEnabled(false);
        incBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incAndDecBtnActionPerformed(evt);
            }
        });

        stepField.setText(stepString);
        stepField.setToolTipText("Step distance must be a positive number and different from 0.");
        stepField.setEnabled(false);
        stepField.setInputVerifier(verifier);
        stepField.setMaximumSize(new java.awt.Dimension(79, 20));
        stepField.setMinimumSize(new java.awt.Dimension(79, 20));
        stepField.setPreferredSize(new java.awt.Dimension(79, 20));
        stepField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        decBtn.setText("-");
        decBtn.setEnabled(false);
        decBtn.setMaximumSize(new java.awt.Dimension(41, 23));
        decBtn.setMinimumSize(new java.awt.Dimension(41, 23));
        decBtn.setPreferredSize(new java.awt.Dimension(41, 23));
        decBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incAndDecBtnActionPerformed(evt);
            }
        });

        reconstructBtn.setText("Reconstruct");
        reconstructBtn.setMaximumSize(new java.awt.Dimension(132, 23));
        reconstructBtn.setMinimumSize(new java.awt.Dimension(132, 23));
        reconstructBtn.setPreferredSize(new java.awt.Dimension(132, 23));
        reconstructBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reconstructBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout btnsPanelLayout = new javax.swing.GroupLayout(btnsPanel);
        btnsPanel.setLayout(btnsPanelLayout);
        btnsPanelLayout.setHorizontalGroup(
            btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnsPanelLayout.createSequentialGroup()
                .addGroup(btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(btnsPanelLayout.createSequentialGroup()
                        .addComponent(decBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stepField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(incBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(batchBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(btnsPanelLayout.createSequentialGroup()
                        .addComponent(reconstructBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(settingsBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, 0))
        );
        btnsPanelLayout.setVerticalGroup(
            btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnsPanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(settingsBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(reconstructBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(decBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stepField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(incBtn)
                    .addComponent(batchBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        chkPanel.setMaximumSize(new java.awt.Dimension(269, 23));
        chkPanel.setMinimumSize(new java.awt.Dimension(269, 23));

        phaseChk.setSelected(phaseEnabled);
        phaseChk.setText("Phase");
        phaseChk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                phaseChkActionPerformed(evt);
            }
        });

        amplitudeChk.setSelected(amplitudeEnabled);
        amplitudeChk.setText("Amp.");
        amplitudeChk.setToolTipText("Amplitude");
        amplitudeChk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                amplitudeChkActionPerformed(evt);
            }
        });

        intensityChk.setSelected(intensityEnabled);
        intensityChk.setText("Int.");
        intensityChk.setToolTipText("Intensity");
        intensityChk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intensityChkActionPerformed(evt);
            }
        });

        realChk.setSelected(realEnabled);
        realChk.setText("Real");
        realChk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                realChkActionPerformed(evt);
            }
        });

        imaginaryChk.setSelected(imaginaryEnabled);
        imaginaryChk.setText("Imaginary");
        imaginaryChk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imaginaryChkActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout chkPanelLayout = new javax.swing.GroupLayout(chkPanel);
        chkPanel.setLayout(chkPanelLayout);
        chkPanelLayout.setHorizontalGroup(
            chkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chkPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(phaseChk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(amplitudeChk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(intensityChk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(realChk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imaginaryChk)
                .addGap(0, 0, 0))
        );
        chkPanelLayout.setVerticalGroup(
            chkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chkPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(chkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(phaseChk)
                    .addComponent(amplitudeChk)
                    .addComponent(intensityChk)
                    .addComponent(realChk)
                    .addComponent(imaginaryChk))
                .addGap(0, 0, 0))
        );

        logPane.setAutoscrolls(true);
        logPane.setMaximumSize(new java.awt.Dimension(274, 240));
        logPane.setMinimumSize(new java.awt.Dimension(274, 240));
        logPane.setPreferredSize(new java.awt.Dimension(274, 240));

        log.setEditable(false);
        log.setColumns(20);
        log.setLineWrap(logWrapping);
        log.setRows(5);
        log.setText(LOG_HEADER);
        log.setWrapStyleWord(true);
        log.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                logMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                logMouseReleased(evt);
            }
        });
        logPane.setViewportView(log);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(parametersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(parametersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(logPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                .addGap(5, 5, 5))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void settingsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsBtnActionPerformed
        if (settingsFrame == null || !settingsFrame.isDisplayable()) {
            settingsFrame = new ReconstructionSettingsFrame(this);
            settingsFrame.setVisible(true);
        } else {
            settingsFrame.setState(Frame.NORMAL);
            settingsFrame.toFront();
        }
    }//GEN-LAST:event_settingsBtnActionPerformed

    private void incAndDecBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_incAndDecBtnActionPerformed
        JComponent source = (JComponent) evt.getSource();
        boolean increment = (source == incBtn);

        ReconstructionWorker worker = new ReconstructionWorker(this);

        worker.setField(interpolatedField);
        if (phaseEnabled && hasRef) {
            worker.setFieldHologramAndReference(interpolatedHologram, interpolatedReference);
        }
        worker.setSize(M, N);

        boolean success = setParametersIncAndDec(worker, increment);
        if (!success) {
            return;
        }

        worker.execute();
    }//GEN-LAST:event_incAndDecBtnActionPerformed

    private void batchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_batchBtnActionPerformed
        if (batchFrame != null && batchFrame.isVisible()) {
            batchFrame.setState(Frame.NORMAL);
            batchFrame.toFront();
            return;
        }

        BatchWorker worker = new BatchWorker(this);

        worker.setField(interpolatedField);
        if (phaseEnabled && hasRef) {
            worker.setHologramAndReference(interpolatedHologram, interpolatedReference);
        }
        worker.setSize(M, N);

        boolean success = setParametersBatch(worker);
        if (!success) {
            return;
        }

        if (batchFrame == null || !batchFrame.isDisplayable()) {
            batchFrame = new BatchFrame(this, worker);
            batchFrame.setVisible(true);
        }
    }//GEN-LAST:event_batchBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (settingsFrame != null && settingsFrame.isVisible()) {
            settingsFrame.setVisible(false);
            settingsFrame.dispose();
        }

        if (batchFrame != null && batchFrame.isVisible()) {
            batchFrame.setVisible(false);
            batchFrame.dispose();
        }

        savePrefs();
        ImagePlus.removeImageListener(this);
        setVisible(false);
        dispose();
    }//GEN-LAST:event_formWindowClosing

    private void lockBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockBtnActionPerformed
        relationLock = lockBtn.isSelected();
        lockBtn.setIcon(new ImageIcon(getClass().getResource(relationLock
                ? "/lock.png" : "/lock_open.png")));

        if (relationLock) {
            float ratio, inW, inH, outW, outH;

            try {
                inW = Float.parseFloat(inputWField.getText());
                inH = Float.parseFloat(inputHField.getText());

                if (inW == 0 || inH == 0) {
                    return;
                }
                ratio = inW / inH;

                outW = Float.parseFloat(outputWField.getText());
                if (outW == 0) {
                    return;
                }
                outH = outW / ratio;
                outputHField.setText(df.format(outH));

                //requests focus to set the parameter
                outputHField.requestFocusInWindow();

                //returns the focus to the relation lock button
                lockBtn.requestFocusInWindow();
            } catch (NumberFormatException exc) {

            }
        }
    }//GEN-LAST:event_lockBtnActionPerformed

    private void logMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_logMousePressed
        if (evt.isPopupTrigger()) {
            popup.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_logMousePressed

    private void logMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_logMouseReleased
        if (evt.isPopupTrigger()) {
            popup.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_logMouseReleased

    private void copyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyItemActionPerformed
        String s = log.getSelectedText();

        StringSelection stringSelection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }//GEN-LAST:event_copyItemActionPerformed

    private void copyAllItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyAllItemActionPerformed
        String s = log.getText();

        StringSelection stringSelection = new StringSelection((s != null) ? s : "");
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }//GEN-LAST:event_copyAllItemActionPerformed

    private void wrapItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wrapItemActionPerformed
        log.setLineWrap(wrapItem.isSelected());
    }//GEN-LAST:event_wrapItemActionPerformed

    private void clearItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearItemActionPerformed
        log.setText(LOG_HEADER);
    }//GEN-LAST:event_clearItemActionPerformed

    private void textFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_textFieldFocusGained
        JTextField field = (JTextField) evt.getComponent();
        field.selectAll();
    }//GEN-LAST:event_textFieldFocusGained

    private void reconstructBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reconstructBtnActionPerformed
        ReconstructionWorker worker = new ReconstructionWorker(this);

        int holoIdx = hologramCombo.getSelectedIndex();
        int refIdx = referenceCombo.getSelectedIndex();

        boolean success = setInputImages(worker, holoIdx, refIdx);
        if (!success) {
            return;
        }
        
        // PROBLEM NUM: SET_PARAMETERS NEED TO BE CALLED AFTER SET_SIZE AND BEFORE SET_HOLOGRAM
        
        worker.execute();
    }//GEN-LAST:event_reconstructBtnActionPerformed

    private void outputSizeRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputSizeRadioActionPerformed
        boolean enable = manualRadio.isSelected();

        outputWLabel.setEnabled(enable);
        outputWField.setEnabled(enable);
        outputHLabel.setEnabled(enable);
        outputHField.setEnabled(enable);
        lockBtn.setEnabled(enable);

        if (enable) {
            if (relationLock) {
                float ratio, inW, inH, outW, outH;

                try {
                    inW = Float.parseFloat(inputWField.getText());
                    inH = Float.parseFloat(inputHField.getText());

                    if (inW == 0 || inH == 0) {
                        return;
                    }
                    ratio = inW / inH;

                    outW = Float.parseFloat(outputWField.getText());
                    if (outW == 0) {
                        return;
                    }
                    outH = outW / ratio;
                    outputHField.setText(df.format(outH));
                } catch (NumberFormatException exc) {

                }
            }

            //requests the focus to set the output size parameters
            outputWField.requestFocusInWindow();
            outputHField.requestFocusInWindow();

            //requests focus again for the radio button
            manualRadio.requestFocusInWindow();
        }
    }//GEN-LAST:event_outputSizeRadioActionPerformed

    private void phaseChkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_phaseChkActionPerformed
        phaseEnabled = phaseChk.isSelected();
    }//GEN-LAST:event_phaseChkActionPerformed

    private void amplitudeChkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_amplitudeChkActionPerformed
        amplitudeEnabled = amplitudeChk.isSelected();
    }//GEN-LAST:event_amplitudeChkActionPerformed

    private void intensityChkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intensityChkActionPerformed
        intensityEnabled = intensityChk.isSelected();
    }//GEN-LAST:event_intensityChkActionPerformed

    private void realChkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_realChkActionPerformed
        realEnabled = realChk.isSelected();
    }//GEN-LAST:event_realChkActionPerformed

    private void imaginaryChkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imaginaryChkActionPerformed
        imaginaryEnabled = imaginaryChk.isSelected();
    }//GEN-LAST:event_imaginaryChkActionPerformed

    private class ParametersVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
//            Component parent = SwingUtilities.getRoot(input);

            boolean valid;

            if (input == lambdaField) {
                valid = checkLambdaField();
            } else if (input == zField) {
                valid = checkZField();
            } else if (input == lField) {
                valid = checkLField();
            } else if (input == inputWField) {
                valid = checkInputWField();
            } else if (input == inputHField) {
                valid = checkInputHField();
            } else if (input == outputWField) {
                valid = checkOutputWField();
            } else if (input == outputHField) {
                valid = checkOutputHField();
            } else if (input == stepField) {
                valid = checkStepField();
            } else {
                valid = true;
            }

            return valid;
        }

        private boolean checkLambdaField() {
            try {
                String txt = lambdaField.getText();

                if (txt.isEmpty()) {
                    lambdaSet = false;
                    lambdaUser = Float.NaN;
                    return true;
                }

                lambdaUser = Float.parseFloat(txt);

                if (lambdaUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    lambdaField.selectAll();

                    lambdaSet = false;
                    lambdaUser = Float.NaN;
                    return false;
                }

                lambdaUm = unitsToUm(lambdaUser, lambdaUnits);

                lambdaSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                lambdaField.selectAll();

                lambdaSet = false;
                lambdaUser = Float.NaN;
                return false;
            }
        }

        private boolean checkZField() {
            try {
                String txt = zField.getText();

                if (txt.isEmpty()) {
                    zSet = false;
                    zUser = Float.NaN;
                    return true;

                }

                zUser = Float.parseFloat(txt);
                zUm = unitsToUm(zUser, reconstructionUnits);

                zSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                zField.selectAll();

                zSet = false;
                zUser = Float.NaN;
                return false;
            }
        }

        private boolean checkLField() {
            try {
                String txt = lField.getText();

                if (txt.isEmpty()) {
                    lSet = false;
                    lUser = Float.NaN;
                    return true;
                }

                lUser = Float.parseFloat(txt);

                if (lUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    lField.selectAll();

                    lSet = false;
                    lUser = Float.NaN;
                    return false;
                }

                lUm = unitsToUm(lUser, sourceToScreenUnits);

                lSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                lField.selectAll();

                lSet = false;
                lUser = Float.NaN;
                return false;
            }
        }

        private boolean checkInputWField() {
            try {
                String txt = inputWField.getText();

                if (txt.isEmpty()) {
                    inputWSet = false;
                    inputWUser = Float.NaN;
                    return true;
                }

                inputWUser = Float.parseFloat(txt);

                if (inputWUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    inputWField.selectAll();

                    inputWSet = false;
                    inputWUser = Float.NaN;
                    return false;
                }

                inputWUm = unitsToUm(inputWUser, inputSizeUnits);

                //if the manual option and the relation lock are selected,
                //updates the size of the sample 
                if (manualRadio.isSelected() && relationLock && inputHSet) {
                    float ratio = inputWUser / inputHUser;

                    if (outputWSet) {
                        outputHUser = outputWUser / ratio;
                        outputHField.setText(df.format(outputHUser));

                        outputHUm = unitsToUm(outputHUser, outputSizeUnits);
                        outputHSet = true;

                    } else if (outputHSet) {
                        outputWUser = outputHUser / ratio;
                        outputWField.setText(df.format(outputWUser));

                        outputWUm = unitsToUm(outputWUser, outputSizeUnits);
                        outputWSet = true;
                    }
                }

                inputWSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                inputWField.selectAll();

                inputWSet = false;
                inputWUser = Float.NaN;
                return false;
            }
        }

        private boolean checkInputHField() {
            try {
                String txt = inputHField.getText();

                if (txt.isEmpty()) {
                    inputHSet = false;
                    inputHUser = Float.NaN;
                    return true;
                }

                inputHUser = Float.parseFloat(txt);

                if (inputHUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    inputWField.selectAll();

                    inputHSet = false;
                    inputHUser = Float.NaN;
                    return false;
                }

                inputHUm = unitsToUm(inputHUser, inputSizeUnits);

                //if the manual option and the relation lock are selected,
                //updates the size of the sample 
                if (manualRadio.isSelected() && relationLock && inputWSet) {
                    float ratio = inputWUser / inputHUser;

                    if (outputWSet) {
                        outputHUser = outputWUser / ratio;
                        outputHField.setText(df.format(outputHUser));

                        outputHUm = unitsToUm(outputHUser, outputSizeUnits);
                        outputHSet = true;

                    } else if (outputHSet) {
                        outputWUser = outputHUser / ratio;
                        outputWField.setText(df.format(outputWUser));

                        outputWUm = unitsToUm(outputWUser, outputSizeUnits);
                        outputWSet = true;
                    }
                }

                inputHSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                inputWField.selectAll();

                inputHSet = false;
                inputHUser = Float.NaN;
                return false;
            }
        }

        private boolean checkOutputWField() {
            try {
                String txt = outputWField.getText();

                if (txt.isEmpty()) {
                    outputWSet = false;
                    outputWUser = Float.NaN;
                    return true;
                }

                outputWUser = Float.parseFloat(txt);

                if (outputWUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    outputWField.selectAll();

                    outputWSet = false;
                    outputWUser = Float.NaN;
                    return false;
                }

                outputWUm = unitsToUm(outputWUser, outputSizeUnits);

                //if the manual option and the relation lock are selected,
                //updates the size of the sample 
                if (relationLock && inputWSet && inputHSet) {
                    float ratio = inputWUser / inputHUser;

                    outputHUser = outputWUser / ratio;
                    outputHField.setText(df.format(outputHUser));

                    outputHUm = unitsToUm(outputHUser, outputSizeUnits);
                    outputHSet = true;
                }

                outputWSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                outputWField.selectAll();

                outputWSet = false;
                outputWUser = Float.NaN;
                return false;
            }
        }

        private boolean checkOutputHField() {
            try {
                String txt = outputHField.getText();

                if (txt.isEmpty()) {
                    outputHSet = false;
                    outputHUser = Float.NaN;
                    return true;
                }

                outputHUser = Float.parseFloat(txt);

                if (outputHUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    outputHField.selectAll();

                    outputHSet = false;
                    outputHUser = Float.NaN;
                    return false;
                }

                outputHUm = unitsToUm(outputHUser, outputSizeUnits);

                //if the manual option and the relation lock are selected,
                //updates the size of the sample 
                if (relationLock && inputWSet && inputHSet) {
                    float ratio = inputWUser / inputHUser;

                    outputWUser = outputHUser / ratio;
                    outputWField.setText(df.format(outputWUser));

                    outputWUm = unitsToUm(outputWUser, outputSizeUnits);
                    outputWSet = true;
                }

                outputHSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                outputHField.selectAll();

                outputHSet = false;
                outputHUser = Float.NaN;
                return false;
            }
        }

        private boolean checkStepField() {
            try {
                String txt = stepField.getText();

                if (txt.isEmpty()) {
                    stepSet = false;
                    stepUser = Float.NaN;
                    return true;
                }

                stepUser = Float.parseFloat(txt);

                if (stepUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    stepField.selectAll();

                    stepSet = false;
                    stepUser = Float.NaN;
                    return false;
                }

                stepUm = unitsToUm(stepUser, reconstructionUnits);
                stepSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                stepField.selectAll();

                stepSet = false;
                stepUser = Float.NaN;
                return false;
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox amplitudeChk;
    private javax.swing.JRadioButton automaticRadio;
    private javax.swing.JButton batchBtn;
    private javax.swing.JPanel btnsPanel;
    private javax.swing.JPanel chkPanel;
    private javax.swing.JMenuItem clearItem;
    private javax.swing.JMenuItem copyAllItem;
    private javax.swing.JMenuItem copyItem;
    private javax.swing.JButton decBtn;
    private javax.swing.JComboBox hologramCombo;
    private javax.swing.JLabel hologramLabel;
    private javax.swing.JCheckBox imaginaryChk;
    private javax.swing.JButton incBtn;
    private javax.swing.JTextField inputHField;
    private javax.swing.JLabel inputHLabel;
    private javax.swing.JTextField inputWField;
    private javax.swing.JLabel inputWLabel;
    private javax.swing.JCheckBox intensityChk;
    private javax.swing.JTextField lField;
    private javax.swing.JLabel lLabel;
    private javax.swing.JTextField lambdaField;
    private javax.swing.JLabel lambdaLabel;
    private javax.swing.JToggleButton lockBtn;
    private javax.swing.JTextArea log;
    private javax.swing.JScrollPane logPane;
    private javax.swing.JRadioButton manualRadio;
    private javax.swing.ButtonGroup outputGroup;
    private javax.swing.JTextField outputHField;
    private javax.swing.JLabel outputHLabel;
    private javax.swing.JTextField outputWField;
    private javax.swing.JLabel outputWLabel;
    private javax.swing.JPanel parametersPanel;
    private javax.swing.JCheckBox phaseChk;
    private javax.swing.JPopupMenu popup;
    private javax.swing.JCheckBox realChk;
    private javax.swing.JButton reconstructBtn;
    private javax.swing.JComboBox referenceCombo;
    private javax.swing.JLabel referenceLabel;
    private javax.swing.JPopupMenu.Separator sep1;
    private javax.swing.JPopupMenu.Separator sep2;
    private javax.swing.JButton settingsBtn;
    private javax.swing.JTextField stepField;
    private javax.swing.JCheckBoxMenuItem wrapItem;
    private javax.swing.JTextField zField;
    private javax.swing.JLabel zLabel;
    // End of variables declaration//GEN-END:variables
}
