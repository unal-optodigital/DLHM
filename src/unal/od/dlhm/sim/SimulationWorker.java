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

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Cursor;
import javax.swing.SwingWorker;
import unal.od.dlhm.diffraction.BluesteinHighNA;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class SimulationWorker extends SwingWorker<Void, Void> {

    //parent frame
    private final SimulationFrame parent;

    //parameters
    private int M, N;
    private float lambda;
    private float z;
    private float L;
    private float dxScreen;
    private float dyScreen;
    private float dxSample;
    private float dySample;

    //sample field
    private float[][] field;

    //outputs
    float[][] hologram, reference, contrast;

    //
    private BluesteinHighNA propagator;

    //selected outputs
    private boolean hologramSelected;
    private boolean referenceSelected;
    private boolean contrastSelected;

    //
    private Calibration cal;
    private String namesSuffix;

    public SimulationWorker(SimulationFrame parent) {
        this.parent = parent;
    }

    @Override
    protected Void doInBackground() throws Exception {
        parent.setCursor(Cursor.getPredefinedCursor(3));
        parent.enableSimulation(false);

        //updates the log with the inputs
        String[] parameters = parent.getFormattedParameters();

        parent.updateLog(true,
                "\nAmplitude: " + parameters[0]
                + "\nPhase: " + parameters[1]
                + "\nWavelength: " + parameters[2]
                + "\nSo. - Sa. dist.: " + parameters[3]
                + "\nSo. - Sc. dist.: " + parameters[4]
                + "\nScreen width: " + parameters[5]
                + "\nScreen height: " + parameters[6]
                + "\nSample width: " + parameters[7]
                + "\nSample height: " + parameters[8]
                + "\nNumerical aperture: " + parameters[9]);

        parent.updateLabel("Performing simulation...");
        
        //sets the names suffix
        namesSuffix = "; Amp: " + parameters[0] + "; Phase: " + parameters[1];
        
        //gets the calibration object
        cal = parent.getCalibration();

        //creates the bluestein object
        propagator = new BluesteinHighNA(M, N, lambda, z, L,
                dxSample, dySample, dxScreen, dyScreen);

        //illuminates the field
        float[][] complexRef = spherical();

        ArrayUtils.complexMultiplication2(field, complexRef);

        //diffracts the field
        propagator.diffract(field);
//        hologram = ArrayUtils.modulusSq(field);
        hologram = propagator.interpolate(ArrayUtils.modulusSq(field));

        if (referenceSelected || contrastSelected) {
            propagator.diffract(complexRef);
            reference = propagator.interpolate(ArrayUtils.modulusSq(complexRef));
        }

        if (contrastSelected) {
            contrast = new float[M][N];

            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    contrast[i][j] = hologram[i][j] - reference[i][j];
                }
            }
        }

        return null;
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            parent.setCursor(Cursor.getDefaultCursor());
            parent.enableSimulation(true);

            parent.updateLog(true,
                    "\nCould not complete the simulation.");

            return;
        }

        if (hologramSelected) {
            ImageProcessor ip = new FloatProcessor(hologram);
            ImagePlus imp = new ImagePlus("Hologram" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();
        }

        if (referenceSelected) {
            ImageProcessor ip = new FloatProcessor(reference);
            ImagePlus imp = new ImagePlus("Reference" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();
        }

        if (contrastSelected) {
            ImageProcessor ip = new FloatProcessor(contrast);
            ImagePlus imp = new ImagePlus("Contrast hologram" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();
        }

        parent.setCursor(Cursor.getDefaultCursor());
        parent.enableSimulation(true);
        parent.updateLabel("Done!");
    }

    private float[][] spherical() {
        float[][] wave = new float[M][2 * N];

        int M2 = (M / 2) - 1;
        int N2 = (N / 2) - 1;

        float z2 = z * z;
        float k = 2 * (float) Math.PI / lambda;

        for (int i = 0, m = -M2; i < M; i++, m++) {

            float rx = z2 + m * m * dxSample * dxSample;

            for (int j = 0, n = -N2; j < N; j++, n++) {

                float r = (float) Math.sqrt(rx + n * n * dySample * dySample);
                float phase = k * r;

                float factor = 1 / r;

                wave[i][2 * j] = factor * (float) Math.cos(phase);
                wave[i][2 * j + 1] = factor * (float) Math.sin(phase);
            }
        }

        return wave;
    }

    public void setField(float[][] amplitude, float[][] phase) {

        if (phase == null) {
            field = ArrayUtils.complexAmplitude(0f, amplitude);
        } else if (amplitude == null) {
            field = ArrayUtils.complexAmplitude(phase, 1f);
        } else {
            field = ArrayUtils.complexAmplitude(phase, amplitude);
        }
    }

    public void setSize(int M, int N) {
        this.M = M;
        this.N = N;
    }

    public void setParameters(float lambda, float z, float L, float screenW,
            float screenH, float sampleW, float sampleH) {

        this.lambda = lambda;
        this.z = z;
        this.L = L;
        this.dxScreen = screenW / M;
        this.dyScreen = screenH / N;
        this.dxSample = sampleW / M;
        this.dySample = sampleH / N;
    }

    public void setOutputs(boolean hologramSelected, boolean referenceSelected,
            boolean contrastSelected) {

        this.hologramSelected = hologramSelected;
        this.referenceSelected = referenceSelected;
        this.contrastSelected = contrastSelected;
    }
}
