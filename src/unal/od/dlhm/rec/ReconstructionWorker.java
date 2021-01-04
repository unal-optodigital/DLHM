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

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Cursor;
import javax.swing.SwingWorker;
import unal.od.dlhm.diffraction.KirchhoffHelmholtz;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class ReconstructionWorker extends SwingWorker<Void, Void> {

    //parent frame
    private final ReconstructionFrame parent;

    //parameters
    private int M, N;
    private float lambda, z, L, dx, dy, dxOut, dyOut;
    private float borderWidth;
    private int averageZoneSize;

    //hologram field
    private float[][] hologram, interpolatedField, outputField, outputField2, outputField3, outputField4;
    private boolean interpolated;

    //Reference & Hologram 
    private float[][] referenceSH;
    private float[][] hologramSH;

    private KirchhoffHelmholtz propagator;

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
    private boolean amplitudeByteSelected;
    private boolean intensityByteSelected;
    private boolean realByteSelected;
    private boolean imaginaryByteSelected;

    //
    boolean filteringEnabled;
    boolean hasReference;
    boolean hasHologram;

    //
    private Calibration cal;
    private String namesSuffix;

    public ReconstructionWorker(ReconstructionFrame parent) {
        this.parent = parent;
    }

    @Override
    protected Void doInBackground() throws Exception {
        parent.setCursor(Cursor.getPredefinedCursor(3));

        //updates the log with the inputs
        String[] parameters = parent.getFormattedParameters();

        parent.updateLog(true,
                "\nHologram: " + parameters[0]
                + "\nReference: " + parameters[1]
                + "\nWavelength: " + parameters[2]
                + "\nReconst. dist.: " + parameters[3]
                + "\nSo. - Sc. dist.: " + parameters[4]
                + "\nInput Width: " + parameters[5]
                + "\nInput Height: " + parameters[6]
                + "\nOutput Width: " + parameters[7]
                + "\nOutput Height: " + parameters[8]);

        //sets the names suffix
        namesSuffix = "; z = " + parameters[3] + "; Holo: " + parameters[0]
                + "; Ref: " + parameters[1];

        //calibration
        cal = parent.getCalibration();

        if (phaseSelected && hasReference) {
            //Correr para holo

            propagator = new KirchhoffHelmholtz(M, N, lambda, z, L, dx, dy, dxOut, dyOut);

            if (!interpolated) {
                if (filteringEnabled) {
                    cosineFilter();
                }

                interpolatedField = propagator.interpolate(hologramSH);
                parent.setInterpolatedField(interpolatedField);
            }

            //copies the interpolated field into a new array for the output field
            outputField2 = new float[M][2 * N];

            for (int i = 0; i < M; i++) {
                System.arraycopy(interpolatedField[i], 0, outputField2[i], 0, 2 * N);
            }

            propagator.diffract(outputField2);

            //Correr para Ref
            propagator = new KirchhoffHelmholtz(M, N, lambda, z, L, dx, dy, dxOut, dyOut);

            if (!interpolated) {
                if (filteringEnabled) {
                    cosineFilter();
                }

                interpolatedField = propagator.interpolate(referenceSH);
                parent.setInterpolatedField(interpolatedField);
            }

            //copies the interpolated field into a new array for the output field
            outputField3 = new float[M][2 * N];

            for (int i = 0; i < M; i++) {
                System.arraycopy(interpolatedField[i], 0, outputField3[i], 0, 2 * N);
            }

            propagator.diffract(outputField3);
        }

        //creates the propagator object
        propagator = new KirchhoffHelmholtz(M, N, lambda, z, L, dx, dy,
                dxOut, dyOut);

        if (!interpolated) {
            if (filteringEnabled) {
                cosineFilter();
            }

            interpolatedField = propagator.interpolate(hologram);
            parent.setInterpolatedField(interpolatedField);
        }

        //copies the interpolated field into a new array for the output field
        outputField = new float[M][2 * N];
        for (int i = 0; i < M; i++) {
            System.arraycopy(interpolatedField[i], 0, outputField[i], 0, 2 * N);
        }

        propagator.diffract(outputField);

        return null;
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            parent.setCursor(Cursor.getDefaultCursor());
            parent.enableAfterPropagationOpt(true);

            parent.updateLog(true,
                    "\nCould not complete the reconstruction.");

            return;
        }

//        ImagePlus imp2 = new ImagePlus("Amplitude; z = " + parameters[3] + names,
//                    amplitudeByteSelected ? ip2.convertToByteProcessor() : ip2);
        if (phaseSelected && hasReference) {

            outputField4 = new float[M][2 * N];

            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    float a = outputField2[i][2 * j];
                    float b = outputField2[i][2 * j + 1];
                    float c = outputField3[i][2 * j];
                    float d = outputField3[i][2 * j + 1];
                    outputField4[i][2 * j] = (a * c + b * d) / (c * c + d * d);
                    outputField4[i][2 * j + 1] = (b * c - a * d) / (c * c + d * d);
                }
            }
            float[][] phase = ArrayUtils.phase(outputField4);

            ImageProcessor ip = new FloatProcessor(phase);
            if (phaseByteSelected) {
                ip = ip.convertToByteProcessor();
            }

            ImagePlus imp = new ImagePlus("Phase" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();
        } else if (phaseSelected) {
            float[][] phase = ArrayUtils.phase(outputField);

            ImageProcessor ip = new FloatProcessor(phase);
            if (phaseByteSelected) {
                ip = ip.convertToByteProcessor();
            }

            ImagePlus imp = new ImagePlus("Phase" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();

        }

        if (amplitudeSelected) {
            float[][] amplitude = ArrayUtils.modulus(outputField);

            ImageProcessor ip = new FloatProcessor(amplitude);
            if (amplitudeLogSelected) {
                ip.log();
            }
            if (amplitudeByteSelected) {
                ip = ip.convertToByteProcessor();
            }

            ImagePlus imp = new ImagePlus("Amplitude" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();
        }

        if (intensitySelected) {
            float[][] intensity = ArrayUtils.modulusSq(outputField);

            ImageProcessor ip = new FloatProcessor(intensity);
            if (intensityLogSelected) {
                ip.log();
            }
            if (intensityByteSelected) {
                ip = ip.convertToByteProcessor();
            }

            ImagePlus imp = new ImagePlus("Intensity" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();
        }

        if (realSelected) {
            float[][] real = ArrayUtils.real(outputField);

            ImageProcessor ip = new FloatProcessor(real);
            if (realByteSelected) {
                ip = ip.convertToByteProcessor();
            }

            ImagePlus imp = new ImagePlus("Real" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();
        }

        if (imaginarySelected) {
            float[][] imaginary = ArrayUtils.imaginary(outputField);

            ImageProcessor ip = new FloatProcessor(imaginary);
            if (imaginaryByteSelected) {
                ip = ip.convertToByteProcessor();
            }

            ImagePlus imp = new ImagePlus("Imaginary" + namesSuffix, ip);
            imp.setCalibration(cal);
            imp.show();
        }

        parent.setCursor(Cursor.getDefaultCursor());
        parent.enableAfterPropagationOpt(true);
    }

    private float[][] generateSphericalFront(float max) {
        float[][] sphericalFront = new float[M][2 * N];

        int M2 = M / 2 - 1;
        int N2 = N / 2 - 1;
        float k = 2 * (float) Math.PI / lambda;
        float L2 = L * L;

        for (int i = 0, m = -M2; i < M; i++, m++) {
            float rx = L2 + (dx * dx * m * m);

            for (int j = 0, n = -N2; j < N; j++, n++) {
                float r = (float) Math.sqrt(rx + (dy * dy * n * n));

                float phase = k * r;

                sphericalFront[i][2 * j] = (float) Math.cos(phase) / r;
                sphericalFront[i][2 * j + 1] = (float) Math.sin(phase) / r;
            }
        }

        return ArrayUtils.scale(ArrayUtils.modulusSq(sphericalFront), max);
    }

    private void cosineFilter() {
        int xBorder = (int) borderWidth * M;
        int yBorder = (int) borderWidth * N;

        float[] xCos = new float[xBorder];
        float[] yCos = new float[yBorder];

        for (int i = 0; i < xBorder; i++) {
            xCos[i] = (1 - (float) Math.cos(Math.PI * i / (xBorder - 1))) / 2.0f;
        }

        for (int j = 0; j < yBorder; j++) {
            yCos[j] = (1 - (float) Math.cos(Math.PI * j / (yBorder - 1))) / 2.0f;
        }

        for (int i = 0; i < M; i++) {
            float xWeight;
            if (i < xBorder) {
                xWeight = xCos[i];
            } else if (i > (M - 1 - xBorder)) {
                xWeight = xCos[M - 1 - i];
            } else {
                xWeight = 1;
            }

            for (int j = 0; j < N; j++) {
                if (j < yBorder) {
                    hologram[i][j] = yCos[i] * xWeight * hologram[i][j];
                } else if (j >= yBorder && j < N - yBorder) {
                    if (xWeight == 1) {
                        j += N - 2 * yBorder - 1;
                    } else {
                        hologram[i][j] = xWeight * hologram[i][j];
                    }
                } else if (j > (N - 1 - yBorder)) {
                    hologram[i][j] = yCos[N - 1 - j] * xWeight * hologram[i][j];
                }
            }
        }
    }

    private float average(float[][] hologram) {
        float average = 0;

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                average += hologram[i][j];
            }
        }

        average = average / (M * N);

        return average;
    }

    private float[][] averageByZones(float[][] hologram) {
        //calculates the number of zones in each dimension
        int xZones = (int) Math.ceil((float) M / averageZoneSize);
        int yZones = (int) Math.ceil((float) N / averageZoneSize);

        float[][] averages = new float[xZones][yZones];

        //number of data points in each zone
        int zonePoints = averageZoneSize * averageZoneSize;

        //calculates the average value for each zone
        for (int i = 0; i < xZones; i++) {
            for (int j = 0; j < yZones; j++) {
                //variable to count the number of data points 
                //outside of the image bounds
                int outsidePoints = 0;

                for (int m = i * averageZoneSize; m < (i + 1) * averageZoneSize; m++) {
                    for (int n = j * averageZoneSize; n < (j + 1) * averageZoneSize; n++) {

                        if (m >= M || n >= N) {
                            outsidePoints++;
                            continue;
                        }

                        averages[i][j] += hologram[m][n];
                    }
                }

                averages[i][j] = averages[i][j] / (zonePoints - outsidePoints);
            }
        }

        return averages;
    }

    public void setHologramAndReference(float[][] hologram, float[][] reference) {
        this.hologram = new float[M][N];
        this.referenceSH = new float[M][N];
        this.hologramSH = new float[M][N];
        this.hasReference = true;

        this.hologramSH = hologram;
        this.referenceSH = reference;

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                this.hologram[i][j] = hologram[i][j] - reference[i][j];
            }
        }
    }

    public void setHologram(float[][] hologram, int contrastType) {
        this.hologram = new float[M][N];
        this.hasReference = false;

        switch (contrastType) {
            case 0: //numerical
                float max = ArrayUtils.max(hologram);
                float[][] spherical = generateSphericalFront(max);

                for (int i = 0; i < M; i++) {
                    for (int j = 0; j < N; j++) {
                        this.hologram[i][j] = hologram[i][j] - spherical[i][j];
                    }
                }

                break;
            case 1: //average
                // if the averageZoneSize is equal to 1 or is greater than
                // the minimum size of the image just takes the average of all
                // the image

                if (averageZoneSize == -1) { //average of all image

                    float average = average(hologram);

                    for (int i = 0; i < M; i++) {
                        for (int j = 0; j < N; j++) {
                            this.hologram[i][j] = hologram[i][j] - average;
                        }
                    }

                } else { //average by zones

                    float[][] averages = averageByZones(hologram);

                    for (int i = 0; i < M; i++) {
                        int m = i / averageZoneSize;

                        for (int j = 0; j < N; j++) {
                            int n = j / averageZoneSize;

                            this.hologram[i][j] = hologram[i][j] - averages[m][n];
                        }
                    }
                }
                break;
            case 2: //none

                this.hologram = hologram;
                break;
        }
    }

    public void setField(float[][] field) {
        interpolated = true;
        this.interpolatedField = field;
    }

    public void setSize(int M, int N) {
        this.M = M;
        this.N = N;
    }

    public void setParameters(float lambda, float z, float L, float inputW,
            float inputH, float outputW, float outputH) {

        this.lambda = lambda;
        this.z = z;
        this.L = L;
        this.dx = inputW / M;
        this.dy = inputH / N;
        this.dxOut = outputW / M;
        this.dyOut = outputH / N;
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
            boolean amplitudeByteSelected, boolean intensityByteSelected,
            boolean realByteSelected, boolean imaginaryByteSelected) {

        this.phaseByteSelected = phaseByteSelected;
        this.amplitudeByteSelected = amplitudeByteSelected;
        this.intensityByteSelected = intensityByteSelected;
        this.realByteSelected = realByteSelected;
        this.imaginaryByteSelected = imaginaryByteSelected;
    }

    public void setLogarithmicScaling(boolean amplitudeLogSelected,
            boolean intensityLogSelected) {

        this.amplitudeLogSelected = amplitudeLogSelected;
        this.intensityLogSelected = intensityLogSelected;
    }

    public void setBorderWidth(float borderWidth) {
        if (borderWidth < 0.5) {
            borderWidth = 0.5f;
        }

        this.borderWidth = borderWidth;
        filteringEnabled = true;
    }

    public void setAverageZoneSize(int size) {
        int min = Math.min(M, N);

        // if the averageZoneSize is equal to 1 or is greater than the minimum
        // size of the image just takes the average of all the image. Sets the
        // averageZoneSize to -1 to identify the situation.
        if (size > min || size == 1) {
            size = -1;
        }

        averageZoneSize = size;
    }

}
