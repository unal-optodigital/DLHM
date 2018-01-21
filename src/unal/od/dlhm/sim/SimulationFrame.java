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

package unal.od.dlhm.sim;

import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import java.awt.Component;
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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import unal.od.dlhm.PreferencesKeys;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class SimulationFrame extends javax.swing.JFrame implements ImageListener,
        PreferencesKeys {

    private static final String TITLE = "DLHM Simulation";
    private static final String LOG_HEADER = "Version 1.0 - April 2017";
    private static final String LOG_SEPARATOR = "\n---------------------------";

    //
    private final ParametersVerifier verifier;

    //user inputs in user units
    private float lambdaUser;
    private float zUser;
    private float lUser;
    private float screenWUser;
    private float screenHUser;
    private float sampleWUser;
    private float sampleHUser;
    private float NAUser;

    //user inputs converted to um
    private float lambdaUm;
    private float zUm;
    private float lUm;
    private float screenWUm;
    private float screenHUm;
    private float sampleWUm;
    private float sampleHUm;

    //boolean for the user parameters
    private boolean lambdaSet;
    private boolean zSet;
    private boolean lSet;
    private boolean screenWSet;
    private boolean screenHSet;
    private boolean sampleWSet;
    private boolean sampleHSet;

    //last used user inputs in user units
    private float lambdaUserLast;
    private float zUserLast;
    private float lUserLast;
    private float screenWUserLast;
    private float screenHUserLast;
    private float sampleWUserLast;
    private float sampleHUserLast;

    //input field dimensions, useful for output calibration
    private int M, N;

    //arrays with the current opened images information
    private int[] windowsId;
    private String[] titles;

    //input images titles
    private String amplitudeTitle;
    private String phaseTitle;

    //calibration object for the output images
    private Calibration cal;

    //formatter
    private final DecimalFormat df;

    //preferences
    private final Preferences pref;

    //frames
    private SimulationSettingsFrame settingsFrame = null;

    // <editor-fold defaultstate="collapsed" desc="Prefs variables">
    //frame location
    private int locX;
    private int locY;

    //last parameters used
    private String lambdaString;
    private String zString;
    private String lString;
    private String screenWString;
    private String screenHString;
    private String sampleWString;
    private String sampleHString;

    //parameters units
    private String lambdaUnits;
    private String sourceToSampleUnits;
    private String sourceToScreenUnits;
    private String screenSizeUnits;
    private String sampleSizeUnits;

    //last outputs used
    private boolean hologramEnabled;
    private boolean referenceEnabled;
    private boolean contrastEnabled;

    private boolean isManual;

    private boolean relationLock;

    private boolean logWrapping;
    // </editor-fold>

    /**
     * Creates the simulation frame
     */
    public SimulationFrame() {
        //initialized objects
        df = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
        pref = Preferences.userNodeForPackage(getClass());

        verifier = new ParametersVerifier();

        //gets the current opened images and load the last preferences
        getOpenedImages();
        loadPrefs();

        initComponents();

        //adds this class as ImageListener
        ImagePlus.addImageListener(this);
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
        } else {
            //titles for both, phase and amplitude, with <none> option
            titles = new String[windowsId.length + 1];
            titles[0] = "<none>";
            for (int i = 0; i < windowsId.length; i++) {
                ImagePlus imp = WindowManager.getImage(windowsId[i]);
                if (imp != null) {
                    titles[i + 1] = imp.getTitle();
                } else {
                    titles[i + 1] = "";
                }
            }
        }
    }

    /**
     * Saves the preferences when the frame is closed.
     */
    private void savePrefs() {
        //frame location
        pref.putInt(SIM_FRAME_LOC_X, getLocation().x);
        pref.putInt(SIM_FRAME_LOC_Y, getLocation().y);

        //parameters text
        pref.putFloat(SIM_LAMBDA, lambdaUser);
        pref.putFloat(SIM_SOURCE_TO_SAMPLE_DISTANCE, zUser);
        pref.putFloat(SIM_SOURCE_TO_SCREEN_DISTANCE, lUser);
        pref.putFloat(SIM_SCREEN_WIDTH, screenWUser);
        pref.putFloat(SIM_SCREEN_HEIGHT, screenHUser);
        pref.putFloat(SIM_SAMPLE_WIDTH, sampleWUser);
        pref.putFloat(SIM_SAMPLE_HEIGHT, sampleHUser);

        //outputs
        pref.putBoolean(SIM_HOLOGRAM_CHECKED, hologramChk.isSelected());
        pref.putBoolean(SIM_REFERENCE_CHECKED, referenceChk.isSelected());
        pref.putBoolean(SIM_CONTRAST_CHECKED, contrastChk.isSelected());

        pref.putBoolean(SIM_IS_MANUAL, manualRadio.isSelected());

        pref.putBoolean(SIM_RELATION_LOCK, lockBtn.isSelected());

        pref.putBoolean(SIM_LOG_WRAPPING, log.getLineWrap());
    }

    /**
     * Loads the preferences when the plugin starts.
     */
    private void loadPrefs() {
        //frame location
        locX = pref.getInt(SIM_FRAME_LOC_X, 300);
        locY = pref.getInt(SIM_FRAME_LOC_Y, 300);

        //outputs
        hologramEnabled = pref.getBoolean(SIM_HOLOGRAM_CHECKED, false);
        referenceEnabled = pref.getBoolean(SIM_REFERENCE_CHECKED, false);
        contrastEnabled = pref.getBoolean(SIM_CONTRAST_CHECKED, false);

        isManual = pref.getBoolean(SIM_IS_MANUAL, true);

        relationLock = pref.getBoolean(SIM_RELATION_LOCK, false);

        logWrapping = pref.getBoolean(SIM_LOG_WRAPPING, true);

        //parameters units
        loadUnitsPrefs();

        //parameters strings for text fields
        loadParameters();
    }

    /**
     * Loads the last used parameters.
     */
    private void loadParameters() {
        //gets the saved floats and checks for NaN values
        lambdaUser = pref.getFloat(SIM_LAMBDA, Float.NaN);
        if (Float.isNaN(lambdaUser)) {
            lambdaSet = false;
            lambdaString = "";
        } else {
            lambdaSet = true;
            lambdaString = df.format(lambdaUser);
            lambdaUm = unitsToUm(lambdaUser, lambdaUnits);
        }

        zUser = pref.getFloat(SIM_SOURCE_TO_SAMPLE_DISTANCE, Float.NaN);
        if (Float.isNaN(zUser)) {
            zSet = false;
            zString = "";
        } else {
            zSet = true;
            zString = df.format(zUser);
            zUm = unitsToUm(zUser, sourceToSampleUnits);
        }

        lUser = pref.getFloat(SIM_SOURCE_TO_SCREEN_DISTANCE, Float.NaN);
        if (Float.isNaN(lUser)) {
            lSet = false;
            lString = "";
        } else {
            lSet = true;
            lString = df.format(lUser);
            lUm = unitsToUm(lUser, sourceToScreenUnits);
        }

        screenWUser = pref.getFloat(SIM_SCREEN_WIDTH, Float.NaN);
        if (Float.isNaN(screenWUser)) {
            screenWSet = false;
            screenWString = "";
        } else {
            screenWSet = true;
            screenWString = df.format(screenWUser);
            screenWUm = unitsToUm(screenWUser, screenSizeUnits);
        }

        screenHUser = pref.getFloat(SIM_SCREEN_HEIGHT, Float.NaN);
        if (Float.isNaN(screenHUser)) {
            screenHSet = false;
            screenHString = "";
        } else {
            screenHSet = true;
            screenHString = df.format(screenHUser);
            screenHUm = unitsToUm(screenHUser, screenSizeUnits);
        }

        sampleWUser = pref.getFloat(SIM_SAMPLE_WIDTH, Float.NaN);
        if (Float.isNaN(sampleWUser)) {
            sampleWSet = false;
            sampleWString = "";
        } else {
            sampleWSet = true;
            sampleWString = df.format(sampleWUser);
            sampleWUm = unitsToUm(sampleWUser, sampleSizeUnits);
        }

        sampleHUser = pref.getFloat(SIM_SAMPLE_HEIGHT, Float.NaN);
        if (Float.isNaN(sampleHUser)) {
            sampleHSet = false;
            sampleHString = "";
        } else {
            sampleHSet = true;
            sampleHString = df.format(sampleHUser);
            sampleHUm = unitsToUm(sampleHUser, sampleSizeUnits);
        }

        if (lSet && (screenWSet || screenHSet)) {
            float wTmp = Float.isNaN(screenWUser)
                    ? Float.POSITIVE_INFINITY : screenWUm;

            float hTmp = Float.isNaN(screenHUser)
                    ? Float.POSITIVE_INFINITY : screenHUm;

            NAUser = (float) Math.sin(Math.atan(0.5
                    * Math.min(wTmp, hTmp) / lUm));

//            updateLabel("Numerical aperture: " + df.format(NAUser));
        }
    }

    /**
     * Loads the units of the parameters.
     */
    private void loadUnitsPrefs() {
        lambdaUnits = pref.get(SIM_LAMBDA_UNITS, "nm");
        sourceToSampleUnits = pref.get(SIM_SOURCE_TO_SAMPLE_DISTANCE_UNITS, "um");
        sourceToScreenUnits = pref.get(SIM_SOURCE_TO_SCREEN_DISTANCE_UNITS, "mm");
        screenSizeUnits = pref.get(SIM_SCREEN_SIZE_UNITS, "mm");
        sampleSizeUnits = pref.get(SIM_SAMPLE_SIZE_UNITS, "mm");
    }

    /**
     * Updates units labels.
     */
    public void updateUnitsPrefs() {
        loadUnitsPrefs();

        lambdaLabel.setText("Wavelength [" + lambdaUnits + "]:");
        zLabel.setText("So. - Sa. dist. [" + sourceToSampleUnits + "]:");
        lLabel.setText("So. - Sc. dist. [" + sourceToScreenUnits + "]:");
        screenWLabel.setText("Screen width [" + screenSizeUnits + "]:");
        screenHLabel.setText("Screen height [" + screenSizeUnits + "]:");
        sampleWLabel.setText("Sample width [" + sampleSizeUnits + "]:");
        sampleHLabel.setText("Sample height [" + sampleSizeUnits + "]:");
    }

    /**
     * Posts a message (s) on the log. If useSeparator is true prints a
     * separator before the message.
     *
     * @param useSeparator
     * @param s text
     */
    public void updateLog(boolean useSeparator, String s) {
        if (useSeparator) {
            log.append(LOG_SEPARATOR);
        }
        log.append(s);
    }

    /**
     * Updates the text in the infoLabel element.
     *
     * @param s text
     */
    public void updateLabel(String s) {
        infoLabel.setText(s);
    }

    /**
     * Returns an array containing the parameters used in the last propagation.
     *
     * @return
     */
    public String[] getFormattedParameters() {
        String[] s = new String[]{
            amplitudeTitle,
            phaseTitle,
            df.format(lambdaUserLast) + " " + lambdaUnits,
            df.format(zUserLast) + " " + sourceToSampleUnits,
            df.format(lUserLast) + " " + sourceToScreenUnits,
            df.format(screenWUserLast) + " " + screenSizeUnits,
            df.format(screenHUserLast) + " " + screenSizeUnits,
            df.format(sampleWUserLast) + " " + sampleSizeUnits,
            df.format(sampleHUserLast) + " " + sampleSizeUnits,
            df.format(NAUser)
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
     * @param amplitudeIdx
     * @param phaseIdx
     * @return success
     */
    private boolean setInputImages(SimulationWorker worker, int amplitudeIdx,
            int phaseIdx) {

        amplitudeTitle = titles[amplitudeIdx];
        phaseTitle = titles[phaseIdx];

        boolean hasAmplitude = !amplitudeTitle.equalsIgnoreCase("<none>");
        boolean hasPhase = !phaseTitle.equalsIgnoreCase("<none>");

        if (!hasAmplitude && !hasPhase) {
            JOptionPane.showMessageDialog(this, "Please select at least the "
                    + "amplitude or the phase of the sample field.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;

        } else if (hasAmplitude && !hasPhase) {
            ImagePlus amplitudeImp = WindowManager.getImage(windowsId[amplitudeIdx - 1]);
            ImageProcessor amplitudeIp = amplitudeImp.getProcessor();
            float[][] amplitude = amplitudeIp.getFloatArray();

            M = amplitudeIp.getWidth();
            N = amplitudeIp.getHeight();

            float maxAmp = ArrayUtils.max(amplitude);
            float minAmp = ArrayUtils.min(amplitude);

            if (maxAmp > 1 || minAmp < 0) {
                ArrayUtils.scale2(amplitude, maxAmp, minAmp, 1);
                amplitudeTitle += " (scaled)";
            }

            worker.setField(amplitude, null);
            worker.setSize(M, N);

        } else if (!hasAmplitude && hasPhase) {

            ImagePlus phaseImp = WindowManager.getImage(windowsId[phaseIdx - 1]);
            ImageProcessor phaseIp = phaseImp.getProcessor();
            float[][] phase = phaseIp.getFloatArray();

            M = phaseIp.getWidth();
            N = phaseIp.getHeight();

            worker.setField(null, phase);
            worker.setSize(M, N);

        } else {
            ImagePlus amplitudeImp = WindowManager.getImage(windowsId[amplitudeIdx - 1]);
            ImageProcessor amplitudeIp = amplitudeImp.getProcessor();
            float[][] amplitude = amplitudeIp.getFloatArray();

            ImagePlus phaseImp = WindowManager.getImage(windowsId[phaseIdx - 1]);
            ImageProcessor phaseIp = phaseImp.getProcessor();
            float[][] phase = phaseIp.getFloatArray();

            M = amplitudeIp.getWidth();
            N = amplitudeIp.getHeight();

            if (M != phaseIp.getWidth() || N != phaseIp.getHeight()) {
                JOptionPane.showMessageDialog(this, "Amplitude and phase images "
                        + "must have the same dimensions.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            float maxAmp = ArrayUtils.max(amplitude);
            float minAmp = ArrayUtils.min(amplitude);

            if (maxAmp > 1 || minAmp < 0) {
                ArrayUtils.scale2(amplitude, maxAmp, minAmp, 1);
                amplitudeTitle += " (scaled)";
            }

            worker.setField(amplitude, phase);
            worker.setSize(M, N);
        }

        return true;
    }

    /**
     * Sets the input parameters from the user selections. Returns false if an
     * error occurs.
     *
     * @return success
     */
    private boolean setParameters(SimulationWorker worker) {
        boolean advance = lambdaSet && zSet && lSet && screenWSet && screenHSet;

        if (manualRadio.isSelected()) {
            advance = advance && sampleWSet && sampleHSet;
        }

        if (!advance) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Please check the input"
                    + " parameters.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //sets the last used parameters
        lambdaUserLast = lambdaUser;

        zUserLast = zUser;

        lUserLast = lUser;

        screenWUserLast = screenWUser;
        screenHUserLast = screenHUser;

        if (automaticRadio.isSelected()) {
            sampleWUm = screenWUm * zUm / lUm;
            sampleHUm = screenHUm * zUm / lUm;

            sampleWUser = umToUnits(sampleWUm, sampleSizeUnits);
            sampleHUser = umToUnits(sampleHUm, sampleSizeUnits);
        }

        sampleWUserLast = sampleWUser;
        sampleHUserLast = sampleHUser;

        //creates the calibration object for output images
        calibrate();

        //guarantees that the numerical aperture is lower than the maximum allowed value
        if (NAUser > 0.57) {
            String[] options = new String[]{"Yes", "No"};
            int n = JOptionPane.showOptionDialog(this, "The numerical aperture "
                    + "of the simulation is greater than 0.57. Do you want to"
                    + "continue?", "", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

            if (n == 1) {
                return false;
            }
        }

        //if there isn't at least one output image selected returns error
        if (!hologramEnabled && !referenceEnabled && !contrastEnabled) {
            JOptionPane.showMessageDialog(this, "Please select at least one "
                    + "output.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //Sets the parameters in the worker object
        worker.setParameters(lambdaUm, zUm, lUm, screenWUm, screenHUm,
                sampleWUm, sampleHUm);
        worker.setOutputs(hologramEnabled, referenceEnabled, contrastEnabled);

        return true;
    }

    /**
     * Creates the calibration object for the output images.
     *
     */
    public void calibrate() {
        float dx = screenWUser / M;
        float dy = screenHUser / N;

        cal = new Calibration();

        cal.setUnit(screenSizeUnits);
        cal.pixelWidth = dx;
        cal.pixelHeight = dy;
    }

    /**
     * Returns the calibration object.
     *
     * @return
     */
    public Calibration getCalibration() {
        return cal;
    }

    public void enableSimulation(boolean enable) {
        simulateBtn.setEnabled(enable);
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
        int ampIdx = amplitudeCombo.getSelectedIndex();
        int phaseIdx = phaseCombo.getSelectedIndex();

        getOpenedImages();
        amplitudeCombo.setModel(new DefaultComboBoxModel<String>(titles));
        amplitudeCombo.setSelectedIndex((ampIdx >= titles.length)
                ? titles.length - 1 : ampIdx);

        phaseCombo.setModel(new DefaultComboBoxModel<String>(titles));
        phaseCombo.setSelectedIndex((phaseIdx >= titles.length)
                ? titles.length - 1 : phaseIdx);
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
        amplitudeCombo = new javax.swing.JComboBox();
        phaseCombo = new javax.swing.JComboBox();
        lambdaField = new javax.swing.JTextField();
        zField = new javax.swing.JTextField();
        screenWField = new javax.swing.JTextField();
        screenHField = new javax.swing.JTextField();
        sampleWField = new javax.swing.JTextField();
        sampleHField = new javax.swing.JTextField();
        lockBtn = new javax.swing.JToggleButton();
        sampleHLabel = new javax.swing.JLabel();
        sampleWLabel = new javax.swing.JLabel();
        screenHLabel = new javax.swing.JLabel();
        screenWLabel = new javax.swing.JLabel();
        zLabel = new javax.swing.JLabel();
        lambdaLabel = new javax.swing.JLabel();
        phaseLabel = new javax.swing.JLabel();
        amplitudeLabel = new javax.swing.JLabel();
        manualRadio = new javax.swing.JRadioButton();
        automaticRadio = new javax.swing.JRadioButton();
        lLabel = new javax.swing.JLabel();
        lField = new javax.swing.JTextField();
        btnsPanel = new javax.swing.JPanel();
        settingsBtn = new javax.swing.JButton();
        simulateBtn = new javax.swing.JButton();
        chkPanel = new javax.swing.JPanel();
        hologramChk = new javax.swing.JCheckBox();
        referenceChk = new javax.swing.JCheckBox();
        contrastChk = new javax.swing.JCheckBox();
        logPane = new javax.swing.JScrollPane();
        log = new javax.swing.JTextArea();
        infoLabel = new javax.swing.JLabel();

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

        amplitudeCombo.setModel(new DefaultComboBoxModel<String>(titles)
        );
        amplitudeCombo.setSelectedIndex(titles.length > 1 ? 1 : 0);
        amplitudeCombo.setMaximumSize(new java.awt.Dimension(115, 20));
        amplitudeCombo.setMinimumSize(new java.awt.Dimension(115, 20));
        amplitudeCombo.setPreferredSize(new java.awt.Dimension(115, 20));

        phaseCombo.setModel(new DefaultComboBoxModel<String>(titles));
        phaseCombo.setSelectedIndex(titles.length > 1 ? 1 : 0);
        phaseCombo.setMaximumSize(new java.awt.Dimension(115, 20));
        phaseCombo.setMinimumSize(new java.awt.Dimension(115, 20));
        phaseCombo.setPreferredSize(new java.awt.Dimension(115, 20));

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
        zField.setToolTipText("Source to sample distance must be a positive number and different from 0.");
        zField.setInputVerifier(verifier);
        zField.setMaximumSize(new java.awt.Dimension(115, 20));
        zField.setMinimumSize(new java.awt.Dimension(115, 20));
        zField.setPreferredSize(new java.awt.Dimension(115, 20));
        zField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        screenWField.setText(screenWString);
        screenWField.setToolTipText("Screen width must be a positive number and different from 0.");
        screenWField.setInputVerifier(verifier);
        screenWField.setMaximumSize(new java.awt.Dimension(115, 20));
        screenWField.setMinimumSize(new java.awt.Dimension(115, 20));
        screenWField.setPreferredSize(new java.awt.Dimension(115, 20));
        screenWField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        screenHField.setText(screenHString);
        screenHField.setToolTipText("Screen height must be a positive number and different from 0.");
        screenHField.setInputVerifier(verifier);
        screenHField.setMaximumSize(new java.awt.Dimension(115, 20));
        screenHField.setMinimumSize(new java.awt.Dimension(115, 20));
        screenHField.setPreferredSize(new java.awt.Dimension(115, 20));
        screenHField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        sampleWField.setText(sampleWString);
        sampleWField.setToolTipText("Sample width must be a positive number and different from 0.");
        sampleWField.setEnabled(isManual);
        sampleWField.setInputVerifier(verifier);
        sampleWField.setMaximumSize(new java.awt.Dimension(83, 20));
        sampleWField.setMinimumSize(new java.awt.Dimension(83, 20));
        sampleWField.setPreferredSize(new java.awt.Dimension(83, 20));
        sampleWField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textFieldFocusGained(evt);
            }
        });

        sampleHField.setText(sampleHString);
        sampleHField.setToolTipText("Sample height must be a positive number and different from 0.");
        sampleHField.setEnabled(isManual);
        sampleHField.setInputVerifier(verifier);
        sampleHField.setMaximumSize(new java.awt.Dimension(83, 20));
        sampleHField.setMinimumSize(new java.awt.Dimension(83, 20));
        sampleHField.setPreferredSize(new java.awt.Dimension(83, 20));
        sampleHField.addFocusListener(new java.awt.event.FocusAdapter() {
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

        sampleHLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        sampleHLabel.setText("Sample height [" + sampleSizeUnits + "]:");
        sampleHLabel.setEnabled(isManual);
        sampleHLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        sampleHLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        sampleHLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        sampleWLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        sampleWLabel.setText("Sample width [" + sampleSizeUnits + "]:");
        sampleWLabel.setEnabled(isManual);
        sampleWLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        sampleWLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        sampleWLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        screenHLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        screenHLabel.setText("Screen height [" + screenSizeUnits + "]:");
        screenHLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        screenHLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        screenHLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        screenWLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        screenWLabel.setText("Screen width [" + screenSizeUnits + "]:");
        screenWLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        screenWLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        screenWLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        zLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        zLabel.setText("So. - Sa. dist. [" + sourceToSampleUnits + "]:");
        zLabel.setToolTipText("Source to sample distance.");
        zLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        zLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        zLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        lambdaLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lambdaLabel.setText("Wavelength [" + lambdaUnits + "]:");
        lambdaLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        lambdaLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        lambdaLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        phaseLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        phaseLabel.setText("Phase:");
        phaseLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        phaseLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        phaseLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        amplitudeLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        amplitudeLabel.setText("Amplitude:");
        amplitudeLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        amplitudeLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        amplitudeLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        outputGroup.add(manualRadio);
        manualRadio.setSelected(isManual);
        manualRadio.setText("Manual");
        manualRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleSizeRadioActionPerformed(evt);
            }
        });

        outputGroup.add(automaticRadio);
        automaticRadio.setSelected(!isManual);
        automaticRadio.setText("Automatic (Geometry)");
        automaticRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleSizeRadioActionPerformed(evt);
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
                            .addComponent(sampleHLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleWLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(sampleHField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleWField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(manualRadio)
                        .addGap(30, 30, 30)
                        .addComponent(automaticRadio))
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(screenHLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(screenWLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(zLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lambdaLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(phaseLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(amplitudeLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(amplitudeCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(phaseCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lambdaField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(zField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(screenWField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(screenHField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                    .addComponent(amplitudeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(amplitudeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(phaseCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(phaseLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                    .addComponent(screenWField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(screenWLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(screenHField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(screenHLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(manualRadio)
                    .addComponent(automaticRadio))
                .addGap(11, 11, 11)
                .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(sampleWLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleWField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(parametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(sampleHLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sampleHField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(parametersPanelLayout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(lockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(5, 5, 5))
        );

        btnsPanel.setMaximumSize(new java.awt.Dimension(270, 31));
        btnsPanel.setMinimumSize(new java.awt.Dimension(270, 31));

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

        simulateBtn.setText("Simulate");
        simulateBtn.setMaximumSize(new java.awt.Dimension(132, 23));
        simulateBtn.setMinimumSize(new java.awt.Dimension(132, 23));
        simulateBtn.setPreferredSize(new java.awt.Dimension(132, 23));
        simulateBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                simulateBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout btnsPanelLayout = new javax.swing.GroupLayout(btnsPanel);
        btnsPanel.setLayout(btnsPanelLayout);
        btnsPanelLayout.setHorizontalGroup(
            btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnsPanelLayout.createSequentialGroup()
                .addComponent(simulateBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(settingsBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
        btnsPanelLayout.setVerticalGroup(
            btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(btnsPanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(btnsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(settingsBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(simulateBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4))
        );

        chkPanel.setMaximumSize(new java.awt.Dimension(269, 23));
        chkPanel.setMinimumSize(new java.awt.Dimension(269, 23));

        hologramChk.setSelected(hologramEnabled);
        hologramChk.setText("Hologram");
        hologramChk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hologramChkActionPerformed(evt);
            }
        });

        referenceChk.setSelected(referenceEnabled);
        referenceChk.setText("Reference");
        referenceChk.setToolTipText("");
        referenceChk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                referenceChkActionPerformed(evt);
            }
        });

        contrastChk.setSelected(contrastEnabled);
        contrastChk.setText("Contrast hologram");
        contrastChk.setToolTipText("Contrast hologram.");
        contrastChk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contrastChkActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout chkPanelLayout = new javax.swing.GroupLayout(chkPanel);
        chkPanel.setLayout(chkPanelLayout);
        chkPanelLayout.setHorizontalGroup(
            chkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chkPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(hologramChk)
                .addGap(7, 7, 7)
                .addComponent(referenceChk, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(contrastChk))
        );
        chkPanelLayout.setVerticalGroup(
            chkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chkPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(chkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(hologramChk)
                    .addComponent(referenceChk)
                    .addComponent(contrastChk))
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

        infoLabel.setText("Ready!");
        infoLabel.setMaximumSize(new java.awt.Dimension(525, 14));
        infoLabel.setMinimumSize(new java.awt.Dimension(525, 14));
        infoLabel.setPreferredSize(new java.awt.Dimension(525, 14));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(parametersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(logPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(5, 5, 5))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(infoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(parametersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(logPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(btnsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(5, 5, 5)
                .addComponent(infoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void settingsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsBtnActionPerformed
        if (settingsFrame == null || !settingsFrame.isDisplayable()) {
            settingsFrame = new SimulationSettingsFrame(this);
            settingsFrame.setVisible(true);
        } else {
            settingsFrame.setState(Frame.NORMAL);
            settingsFrame.toFront();
        }
    }//GEN-LAST:event_settingsBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing

        if (settingsFrame != null && settingsFrame.isVisible()) {
            settingsFrame.setVisible(false);
            settingsFrame.dispose();
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
            float ratio, screenW, screenH, sampleW, sampleH;

            try {
                screenW = Float.parseFloat(screenWField.getText());
                screenH = Float.parseFloat(screenHField.getText());

                if (screenW == 0 || screenH == 0) {
                    return;
                }
                ratio = screenW / screenH;

                sampleW = Float.parseFloat(sampleWField.getText());
                if (sampleW == 0) {
                    return;
                }

                sampleH = sampleW / ratio;
                sampleHField.setText(df.format(sampleH));
                
                //requests focus to set the parameter
                sampleHField.requestFocusInWindow();
                
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

    private void simulateBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_simulateBtnActionPerformed
        SimulationWorker worker = new SimulationWorker(this);

        int amplitudeIdx = amplitudeCombo.getSelectedIndex();
        int phaseIdx = phaseCombo.getSelectedIndex();

        boolean success = setInputImages(worker, amplitudeIdx, phaseIdx);
        if (!success) {
            return;
        }
        updateLabel("Input images set!");

        success = setParameters(worker);
        if (!success) {
            return;
        }
        updateLabel("Input parameters set!");

        worker.execute();
    }//GEN-LAST:event_simulateBtnActionPerformed

    private void sampleSizeRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleSizeRadioActionPerformed
        boolean enable = manualRadio.isSelected();

        sampleWLabel.setEnabled(enable);
        sampleWField.setEnabled(enable);
        sampleHLabel.setEnabled(enable);
        sampleHField.setEnabled(enable);
        lockBtn.setEnabled(enable);

        if (enable) {
            if (relationLock) {
                float ratio, screenW, screenH, sampleW, sampleH;

                try {
                    screenW = Float.parseFloat(screenWField.getText());
                    screenH = Float.parseFloat(screenHField.getText());

                    if (screenW == 0 || screenH == 0) {
                        return;
                    }
                    ratio = screenW / screenH;

                    sampleW = Float.parseFloat(sampleWField.getText());
                    if (sampleW == 0) {
                        return;
                    }

                    sampleH = sampleW / ratio;
                    sampleHField.setText(df.format(sampleH));
                } catch (NumberFormatException exc) {

                }
            }
            
            //requests the focus to set the output size parameters
            sampleWField.requestFocusInWindow();
            sampleHField.requestFocusInWindow();
            
            //requests focus again for the radio button
            manualRadio.requestFocusInWindow();
        }
    }//GEN-LAST:event_sampleSizeRadioActionPerformed

    private void hologramChkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hologramChkActionPerformed
        hologramEnabled = hologramChk.isSelected();
    }//GEN-LAST:event_hologramChkActionPerformed

    private void referenceChkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_referenceChkActionPerformed
        referenceEnabled = referenceChk.isSelected();
    }//GEN-LAST:event_referenceChkActionPerformed

    private void contrastChkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contrastChkActionPerformed
        contrastEnabled = contrastChk.isSelected();
    }//GEN-LAST:event_contrastChkActionPerformed

    private class ParametersVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
            Component parent = SwingUtilities.getRoot(input);

            boolean valid;

            if (input == lambdaField) {
                valid = checkLambdaField();
            } else if (input == zField) {
                valid = checkZField(parent);
            } else if (input == lField) {
                valid = checkLField(parent);
            } else if (input == screenWField) {
                valid = checkScreenWField(parent);
            } else if (input == screenHField) {
                valid = checkScreenHField(parent);
            } else if (input == sampleWField) {
                valid = checkSampleWField(parent);
            } else if (input == sampleHField) {
                valid = checkSampleHField(parent);
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

        private boolean checkZField(Component parent) {
            try {
                String txt = zField.getText();

                if (txt.isEmpty()) {
                    zSet = false;
                    zUser = Float.NaN;
                    return true;
                }

                zUser = Float.parseFloat(txt);

                if (zUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    zField.selectAll();

                    zSet = false;
                    zUser = Float.NaN;
                    return false;
                }

                zUm = unitsToUm(zUser, sourceToSampleUnits);

                //guarantees that L is greater than z
                if (lSet && (lUm <= zUm)) {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(parent, "Source to screen"
                            + " distance must be greater than the source to"
                            + " sample distance.", "Error",
                            JOptionPane.ERROR_MESSAGE);
//                    zField.selectAll();
                    zField.setText("");

                    zSet = false;
                    zUser = Float.NaN;
                    return true;
                }

                //guarantees that z meets the conditions for proper sampling
                if (sampleWSet || sampleHSet) {
                    float wTmp = Float.isNaN(sampleWUser)
                            ? Float.NEGATIVE_INFINITY : sampleWUm;

                    float hTmp = Float.isNaN(sampleHUser)
                            ? Float.NEGATIVE_INFINITY : sampleHUm;

                    float zMin = (float) Math.sqrt(2) / 2 * Math.max(wTmp, hTmp);

                    if (zUm < zMin) {
                        zMin = umToUnits(zMin, sourceToSampleUnits);

                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(parent, "The minimum "
                                + "allowed source to sample distance for "
                                + "the given sample dimensions is "
                                + df.format(zMin) + " " + sourceToSampleUnits,
                                "Error", JOptionPane.ERROR_MESSAGE);
//                        zField.selectAll();
                        zField.setText("");

                        zSet = false;
                        zUser = Float.NaN;
                        return true;
                    }
                }

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

        private boolean checkLField(Component parent) {
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

                //guarantees that L is greater than z
                if (zSet && (lUm <= zUm)) {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(parent, "Source to screen"
                            + " distance must be greater than the source to"
                            + " sample distance.", "Error",
                            JOptionPane.ERROR_MESSAGE);
//                    lField.selectAll();
                    lField.setText("");

                    lSet = false;
                    lUser = Float.NaN;
                    return true;
                }

                //guarantees that L meets the conditions for proper sampling
                if (screenWSet || screenHSet) {
                    float wTmp = Float.isNaN(screenWUser)
                            ? Float.NEGATIVE_INFINITY : screenWUm;

                    float hTmp = Float.isNaN(screenHUser)
                            ? Float.NEGATIVE_INFINITY : screenHUm;

                    float lMin = (float) Math.sqrt(2) / 2 * Math.max(wTmp, hTmp);

                    if (lUm < lMin) {
                        lMin = umToUnits(lMin, sourceToScreenUnits);

                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(parent, "The minimum "
                                + "allowed source to screen distance for "
                                + "the given screen dimensions is "
                                + df.format(lMin) + " " + sourceToScreenUnits,
                                "Error", JOptionPane.ERROR_MESSAGE);
//                        lField.selectAll();
                        lField.setText("");

                        lSet = false;
                        lUser = Float.NaN;
                        return true;
                    }
                }

                //updates the NA
                if (screenWSet || screenHSet) {
                    float wTmp = Float.isNaN(screenWUser)
                            ? Float.POSITIVE_INFINITY : screenWUm;

                    float hTmp = Float.isNaN(screenHUser)
                            ? Float.POSITIVE_INFINITY : screenHUm;

                    NAUser = (float) Math.sin(Math.atan(0.5
                            * Math.min(wTmp, hTmp) / lUm));

                    updateLabel("Numerical aperture: " + df.format(NAUser));
                }

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

        private boolean checkScreenWField(Component parent) {
            try {
                String txt = screenWField.getText();

                if (txt.isEmpty()) {
                    screenWSet = false;
                    screenWUser = Float.NaN;
                    return true;
                }

                screenWUser = Float.parseFloat(txt);

                if (screenWUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    screenWField.selectAll();

                    screenWSet = false;
                    screenWUser = Float.NaN;
                    return false;
                }

                screenWUm = unitsToUm(screenWUser, screenSizeUnits);

                //guarantees that L meets the conditions for proper sampling
                if (lSet) {
                    float hTmp = Float.isNaN(screenHUser)
                            ? Float.NEGATIVE_INFINITY : screenHUm;

                    float lMin = (float) Math.sqrt(2) / 2 * Math.max(screenWUm, hTmp);

                    if (lUm < lMin) {
                        float screenDim = (2 * lUm) / (float) Math.sqrt(2);
                        screenDim = umToUnits(screenDim, screenSizeUnits);

                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(parent, "The maximum "
                                + "allowed screen dimension for the given source"
                                + "to screen distance is " + df.format(screenDim)
                                + " " + screenSizeUnits, "Error",
                                JOptionPane.ERROR_MESSAGE);
                        screenWField.setText("");

                        screenWSet = false;
                        screenWUser = Float.NaN;
                        return true;
                    }
                }

                //updates the NA
                if (lSet) {
                    float hTmp = Float.isNaN(screenHUser)
                            ? Float.POSITIVE_INFINITY : screenHUm;

                    NAUser = (float) Math.sin(Math.atan(0.5
                            * Math.min(screenWUm, hTmp) / lUm));

                    updateLabel("Numerical aperture: " + df.format(NAUser));
                }

                //if the manual option and the relation lock are selected,
                //updates the size of the sample 
                if (manualRadio.isSelected() && relationLock && screenHSet) {
                    float ratio = screenWUser / screenHUser;

                    if (sampleWSet) {
                        sampleHUser = sampleWUser / ratio;
                        sampleHField.setText(df.format(sampleHUser));

                        sampleHUm = unitsToUm(sampleHUser, sampleSizeUnits);
                        sampleHSet = true;

                    } else if (sampleHSet) {
                        sampleWUser = sampleHUser / ratio;
                        sampleWField.setText(df.format(sampleWUser));

                        sampleWUm = unitsToUm(sampleWUser, sampleSizeUnits);
                        sampleWSet = true;
                    }
                }

                screenWSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                screenWField.selectAll();

                screenWSet = false;
                screenWUser = Float.NaN;
                return false;
            }
        }

        private boolean checkScreenHField(Component parent) {
            try {
                String txt = screenHField.getText();

                if (txt.isEmpty()) {
                    screenHSet = false;
                    screenHUser = Float.NaN;
                    return true;
                }

                screenHUser = Float.parseFloat(txt);

                if (screenHUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    screenHField.selectAll();

                    screenHSet = false;
                    screenHUser = Float.NaN;
                    return false;
                }

                screenHUm = unitsToUm(screenHUser, screenSizeUnits);

                //guarantees that L meets the conditions for proper sampling
                if (lSet) {
                    float wTmp = Float.isNaN(screenWUser)
                            ? Float.NEGATIVE_INFINITY : screenWUm;

                    float lMin = (float) Math.sqrt(2) / 2 * Math.max(wTmp, screenHUm);

                    if (lUm < lMin) {
                        float screenDim = (2 * lUm) / (float) Math.sqrt(2);
                        screenDim = umToUnits(screenDim, screenSizeUnits);

                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(parent, "The maximum "
                                + "allowed screen dimension for the given source"
                                + "to screen distance is " + df.format(screenDim)
                                + " " + screenSizeUnits, "Error",
                                JOptionPane.ERROR_MESSAGE);
                        screenHField.setText("");

                        screenHSet = false;
                        screenHUser = Float.NaN;
                        return true;
                    }
                }

                //updates the NA
                if (lSet) {
                    float wTmp = Float.isNaN(screenWUser)
                            ? Float.POSITIVE_INFINITY : screenWUm;

                    NAUser = (float) Math.sin(Math.atan(0.5
                            * Math.min(wTmp, screenHUm) / lUm));

                    updateLabel("Numerical aperture: " + df.format(NAUser));
                }

                //if the manual option and the relation lock are selected,
                //updates the size of the sample 
                if (manualRadio.isSelected() && relationLock && screenWSet) {
                    float ratio = screenWUser / screenHUser;

                    if (sampleWSet) {
                        sampleHUser = sampleWUser / ratio;
                        sampleHField.setText(df.format(sampleHUser));

                        sampleHUm = unitsToUm(sampleHUser, sampleSizeUnits);
                        sampleHSet = true;

                    } else if (sampleHSet) {
                        sampleWUser = sampleHUser / ratio;
                        sampleWField.setText(df.format(sampleWUser));

                        sampleWUm = unitsToUm(sampleWUser, sampleSizeUnits);
                        sampleWSet = true;
                    }
                }

                screenHSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                screenHField.selectAll();

                screenHSet = false;
                screenHUser = Float.NaN;
                return false;
            }
        }

        private boolean checkSampleWField(Component parent) {
            try {
                String txt = sampleWField.getText();

                if (txt.isEmpty()) {
                    sampleWSet = false;
                    sampleWUser = Float.NaN;
                    return true;
                }

                sampleWUser = Float.parseFloat(txt);

                if (sampleWUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    sampleWField.selectAll();

                    sampleWSet = false;
                    sampleWUser = Float.NaN;
                    return false;
                }

                sampleWUm = unitsToUm(sampleWUser, sampleSizeUnits);

                //guarantees that z meets the conditions for proper sampling
                if (zSet) {
                    float hTmp = Float.isNaN(sampleHUser)
                            ? Float.NEGATIVE_INFINITY : sampleHUm;

                    float zMin = (float) Math.sqrt(2) / 2 * Math.max(sampleWUm, hTmp);

                    if (zUm < zMin) {
                        float sampleDim = (2 * zUm) / (float) Math.sqrt(2);
                        sampleDim = umToUnits(sampleDim, sampleSizeUnits);

                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(parent, "The maximum "
                                + "allowed sample dimension for the given source"
                                + "to sample distance is " + df.format(sampleDim)
                                + " " + sampleSizeUnits, "Error",
                                JOptionPane.ERROR_MESSAGE);
//                        sampleWField.selectAll();
                        sampleWField.setText("");

                        sampleWSet = false;
                        sampleWUser = Float.NaN;
                        return true;
                    }
                }

                //if the manual option and the relation lock are selected,
                //updates the size of the sample 
                if (relationLock && screenWSet && screenHSet) {
                    float ratio = screenWUser / screenHUser;

                    sampleHUser = sampleWUser / ratio;
                    sampleHField.setText(df.format(sampleHUser));

                    sampleHUm = unitsToUm(sampleHUser, sampleSizeUnits);
                    sampleHSet = true;
                }

                sampleWSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                sampleWField.selectAll();

                sampleWSet = false;
                sampleWUser = Float.NaN;
                return false;
            }
        }

        private boolean checkSampleHField(Component parent) {
            try {
                String txt = sampleHField.getText();

                if (txt.isEmpty()) {
                    sampleHSet = false;
                    sampleHUser = Float.NaN;
                    return true;
                }

                sampleHUser = Float.parseFloat(txt);

                if (sampleHUser <= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    sampleHField.selectAll();

                    sampleHSet = false;
                    sampleHUser = Float.NaN;
                    return false;
                }

                sampleHUm = unitsToUm(sampleHUser, sampleSizeUnits);

                //guarantees that z meets the conditions for proper sampling
                if (zSet) {
                    float wTmp = Float.isNaN(sampleWUser)
                            ? Float.NEGATIVE_INFINITY : sampleWUm;

                    float zMin = (float) Math.sqrt(2) / 2 * Math.max(wTmp, sampleHUm);

                    System.out.println("" + zUm);
                    System.out.println("" + zMin);

                    if (zUm < zMin) {
                        float sampleDim = 2 * zUm / (float) Math.sqrt(2);
                        sampleDim = umToUnits(sampleDim, sampleSizeUnits);

                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(parent, "The maximum "
                                + "allowed sample dimension for the given source"
                                + "to sample distance is " + df.format(sampleDim)
                                + " " + sampleSizeUnits, "Error",
                                JOptionPane.ERROR_MESSAGE);
                        sampleHField.setText("");

                        sampleHSet = false;
                        sampleHUser = Float.NaN;
                        return true;
                    }
                }

                //if the manual option and the relation lock are selected,
                //updates the size of the sample 
                if (relationLock && screenWSet && screenHSet) {
                    float ratio = screenWUser / screenHUser;

                    sampleWUser = sampleHUser / ratio;
                    sampleWField.setText(df.format(sampleWUser));

                    sampleWUm = unitsToUm(sampleWUser, sampleSizeUnits);
                    sampleWSet = true;
                }

                sampleHSet = true;
                return true;

            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                sampleHField.selectAll();

                sampleHSet = false;
                sampleHUser = Float.NaN;
                return false;
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox amplitudeCombo;
    private javax.swing.JLabel amplitudeLabel;
    private javax.swing.JRadioButton automaticRadio;
    private javax.swing.JPanel btnsPanel;
    private javax.swing.JPanel chkPanel;
    private javax.swing.JMenuItem clearItem;
    private javax.swing.JCheckBox contrastChk;
    private javax.swing.JMenuItem copyAllItem;
    private javax.swing.JMenuItem copyItem;
    private javax.swing.JCheckBox hologramChk;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JTextField lField;
    private javax.swing.JLabel lLabel;
    private javax.swing.JTextField lambdaField;
    private javax.swing.JLabel lambdaLabel;
    private javax.swing.JToggleButton lockBtn;
    private javax.swing.JTextArea log;
    private javax.swing.JScrollPane logPane;
    private javax.swing.JRadioButton manualRadio;
    private javax.swing.ButtonGroup outputGroup;
    private javax.swing.JPanel parametersPanel;
    private javax.swing.JComboBox phaseCombo;
    private javax.swing.JLabel phaseLabel;
    private javax.swing.JPopupMenu popup;
    private javax.swing.JCheckBox referenceChk;
    private javax.swing.JTextField sampleHField;
    private javax.swing.JLabel sampleHLabel;
    private javax.swing.JTextField sampleWField;
    private javax.swing.JLabel sampleWLabel;
    private javax.swing.JTextField screenHField;
    private javax.swing.JLabel screenHLabel;
    private javax.swing.JTextField screenWField;
    private javax.swing.JLabel screenWLabel;
    private javax.swing.JPopupMenu.Separator sep1;
    private javax.swing.JPopupMenu.Separator sep2;
    private javax.swing.JButton settingsBtn;
    private javax.swing.JButton simulateBtn;
    private javax.swing.JCheckBoxMenuItem wrapItem;
    private javax.swing.JTextField zField;
    private javax.swing.JLabel zLabel;
    // End of variables declaration//GEN-END:variables
}
