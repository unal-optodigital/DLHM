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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Cursor;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.SwingWorker;
import unal.od.dlhm.diffraction.KirchhoffHelmholtz;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class BatchWorker extends SwingWorker<Void, Void> {

    //parent frame
    private final ReconstructionFrame parent;

    //parameters
    private int M, N;
    private float lambda, L, dx, dy;
    private float zStart, zEnd, zStep;
    private int planes;

    //output size
    private float dxOut, dyOut;
    private boolean outputFixed;

    //hologram field
    private float[][] interpolatedField;

    private KirchhoffHelmholtz propagator;

    //formatter
    private final DecimalFormat df;

    //selected outputs
    private boolean phaseSelected;
    private boolean amplitudeSelected;
    private boolean intensitySelected;
    private boolean realSelected;
    private boolean imaginarySelected;

    //log scaling booleans
    private boolean amplitudeLogSelected;
    private boolean intensityLogSelected;

    //8bit scaling booleans
    private boolean phaseByteSelected;
    private boolean realByteSelected;
    private boolean imaginaryByteSelected;

    //image stacks
    ImageStack phaseStack;
    ImageStack amplitudeStack;
    ImageStack intensityStack;
    ImageStack realStack;
    ImageStack imaginaryStack;

    //
    private String[] parameters;
    private Calibration cal;
    private String namesSuffix;

    private String reconstructionUnits;

    public BatchWorker(ReconstructionFrame parent) {
        this.parent = parent;

        df = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
    }

    @Override
    protected Void doInBackground() throws Exception {
        parent.setCursor(Cursor.getPredefinedCursor(3));

        parent.updateLog(true,
                "\nHologram: " + parameters[0]
                + "\nReference: " + parameters[1]
                + "\nWavelength: " + parameters[2]
                + "\nReconst. dist.: " + parameters[3]
                + parameters[4]
                + "\nSo. - Sc. dist.: " + parameters[5]
                + "\nInput Width: " + parameters[6]
                + "\nInput Height: " + parameters[7]);

        if (outputFixed) {
            parent.updateLog(false,
                    "\nOutput Width: " + parameters[8]
                    + "\nOutput Height: " + parameters[9]);

            cal = parent.getCalibration();
        } else {
            parent.updateLog(false, "\nOutput Sizes: Geometry");
        }

        phaseStack = new ImageStack(M, N);
        amplitudeStack = new ImageStack(M, N);
        intensityStack = new ImageStack(M, N);
        realStack = new ImageStack(M, N);
        imaginaryStack = new ImageStack(M, N);

        int progress = 0;
        IJ.showStatus("DLHM Batch mode: " + progress + "/" + planes);
        IJ.showProgress(progress);

        for (float z = zStart; z <= zEnd; z += zStep) {

            if (!outputFixed) {
                dxOut = dx * z / L;
                dyOut = dy * z / L;

                if (z == 0) {
                    dxOut = dx / L;
                    dyOut = dy / L;
                }
            }

            propagator = new KirchhoffHelmholtz(M, N, lambda, z, L, dx, dy,
                    dxOut, dyOut);

            float[][] outputField = new float[M][2 * N];
            for (int i = 0; i < M; i++) {
                System.arraycopy(interpolatedField[i], 0, outputField[i], 0, 2 * N);
            }

            propagator.diffract(outputField);

            String label = "z = " + df.format(umToUnits(z))
                    + " " + reconstructionUnits;

            if (!outputFixed) {
                label += "; W = " + df.format(M * dxOut) + " um; H = "
                        + df.format(N * dyOut) + " um";
            }

            if (phaseSelected) {
                float[][] phase = ArrayUtils.phase(outputField);

                ImageProcessor ip = new FloatProcessor(phase);
                ip.setMinAndMax(-Math.PI, Math.PI);
                if (phaseByteSelected) {
                    ip = ip.convertToByteProcessor();
                }

                phaseStack.addSlice(label, ip);
            }

            if (amplitudeSelected) {
                float[][] amplitude = ArrayUtils.modulus(outputField);

                ImageProcessor ip = new FloatProcessor(amplitude);
                if (amplitudeLogSelected) {
                    ip.log();
                }

                ip = ip.convertToByteProcessor();
                amplitudeStack.addSlice(label, ip);
            }

            if (intensitySelected) {
                float[][] intensity = ArrayUtils.modulusSq(outputField);

                ImageProcessor ip = new FloatProcessor(intensity);
                if (intensityLogSelected) {
                    ip.log();
                }

                ip = ip.convertToByteProcessor();
                intensityStack.addSlice(label, ip);
            }

            if (realSelected) {
                float[][] real = ArrayUtils.real(outputField);

                ImageProcessor ip = new FloatProcessor(real);
                if (realByteSelected) {
                    ip = ip.convertToByteProcessor();
                }

                realStack.addSlice(label, ip);
            }

            if (imaginarySelected) {
                float[][] imaginary = ArrayUtils.imaginary(outputField);

                ImageProcessor ip = new FloatProcessor(imaginary);
                if (imaginaryByteSelected) {
                    ip = ip.convertToByteProcessor();
                }

                imaginaryStack.addSlice(label, ip);
            }

            //refreshes the imagej window with the progress
            IJ.showProgress(++progress / (float) planes);
            IJ.showStatus("DLHM Batch mode: " + progress + "/" + planes);
        }

        return null;
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            parent.setCursor(Cursor.getDefaultCursor());

            parent.updateLog(true,
                    "\nCould not complete the reconstruction.");

            return;
        }

        if (phaseSelected) {
            ImagePlus imp = new ImagePlus("Phase" + namesSuffix, phaseStack);
            if (outputFixed) {
                imp.setCalibration(cal);
            }

            imp.show();
        }

        if (amplitudeSelected) {
            ImagePlus imp = new ImagePlus("Amplitude" + namesSuffix, amplitudeStack);
            if (outputFixed) {
                imp.setCalibration(cal);
            }

            imp.show();
        }

        if (intensitySelected) {
            ImagePlus imp = new ImagePlus("Intensity" + namesSuffix, intensityStack);
            if (outputFixed) {
                imp.setCalibration(cal);
            }

            imp.show();
        }

        if (realSelected) {
            ImagePlus imp = new ImagePlus("Real" + namesSuffix, realStack);
            if (outputFixed) {
                imp.setCalibration(cal);
            }

            imp.show();
        }

        if (imaginarySelected) {
            ImagePlus imp = new ImagePlus("Imaginary" + namesSuffix, imaginaryStack);
            if (outputFixed) {
                imp.setCalibration(cal);
            }

            imp.show();
        }

        IJ.showStatus("DLHM Batch mode: done!");
        parent.setCursor(Cursor.getDefaultCursor());
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

    public void setField(float[][] field) {
        this.interpolatedField = field;
    }

    public void setSize(int M, int N) {
        this.M = M;
        this.N = N;
    }

    public void setParameters(float lambda, float L, float inputW, float inputH) {

        this.lambda = lambda;
        this.L = L;
        this.dx = inputW / M;
        this.dy = inputH / N;
    }

    public void setOutputSizes(float outputW, float outputH) {
        this.dxOut = outputW / M;
        this.dyOut = outputH / N;

        outputFixed = true;
    }

    public void setDistances(float zStart, float zEnd, float zStep, int planes) {
        this.zStart = zStart;
        this.zEnd = zEnd;
        this.zStep = zStep;
        this.planes = planes;
    }

    public void setOutputs(boolean phaseSelected, boolean amplitudeSelected,
            boolean intensitySelected, boolean realSelected,
            boolean imaginarySelected) {

        this.phaseSelected = phaseSelected;
        this.amplitudeSelected = amplitudeSelected;
        this.intensitySelected = intensitySelected;
        this.realSelected = realSelected;
        this.imaginarySelected = imaginarySelected;
    }

    public void setByteScaling(boolean phaseByteSelected,
            boolean realByteSelected, boolean imaginaryByteSelected) {

        this.phaseByteSelected = phaseByteSelected;
        this.realByteSelected = realByteSelected;
        this.imaginaryByteSelected = imaginaryByteSelected;
    }

    public void setLogarithmicScaling(boolean amplitudeLogSelected,
            boolean intensityLogSelected) {

        this.amplitudeLogSelected = amplitudeLogSelected;
        this.intensityLogSelected = intensityLogSelected;
    }

    public void setInfo(String[] parameters, String reconstructionUnits) {
        this.parameters = parameters;
        this.namesSuffix = "; Holo: " + parameters[0] + "; Ref: " + parameters[1];
        this.reconstructionUnits = reconstructionUnits;
    }
}
