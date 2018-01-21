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

package unal.od.dlhm.diffraction;

import org.jtransforms.fft.FloatFFT_2D;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class BluesteinHighNA {

    private final int M, N;
    private final float z, lambda, dx, dy, dxOut, dyOut;

    private final float[][] kernel1;//, outputPhase; (not important for the intended use of this class)
    private float[][] kernel2;
    private final FloatFFT_2D fft;

    public BluesteinHighNA(int M, int N, float lambda, float z, float L, float dx,
            float dy, float dxOut, float dyOut) {

        this.M = M;
        this.N = N;
        this.lambda = lambda;
        this.z = L - z;
        this.dx = dx;
        this.dy = dy;
        this.dxOut = dxOut;
        this.dyOut = dyOut;

        kernel1 = new float[M][2 * N];
//        outputPhase = new float[M][2 * N]; //(not important for the intended use of this class)
        fft = new FloatFFT_2D(2 * M, 2 * N);

        calculateKernels();

    }

    private void calculateKernels() {
        float z2 = z * z;

        int M2 = (M / 2) - 1;
        int N2 = (N / 2) - 1;

        float k2 = (float) Math.PI / lambda;

        float[][] kernel2 = new float[M][2 * N];

        for (int i = 0, m = -M2; i < M; i++, m++) {

            float rx = 1 - (m * m * dxOut * dxOut / z2);

            for (int j = 0, n = -N2; j < N; j++, n++) {

                float r = (float) Math.sqrt(rx - (n * n * dyOut * dyOut / z2));

                float dX = dxOut / r;
                float dY = dyOut / r;

                float R = (float) Math.sqrt(z2 + (m * m * dX * dX) + (n * n * dY * dY));
                float factor = k2 / R;

                float phase1 = factor * ((dx * (dx - dX) * m * m) + (dy * (dy - dY) * n * n));
                float phase2 = factor * ((dx * dX * m * m) + (dy * dY * n * n));

                kernel1[i][2 * j] = (float) Math.cos(phase1);
                kernel1[i][2 * j + 1] = (float) Math.sin(phase1);

                kernel2[i][2 * j] = (float) Math.cos(phase2);
                kernel2[i][2 * j + 1] = (float) Math.sin(phase2);

                //output plane phase (not important for the intended use of this class)
//                float factor2 = 1 / (lambda * R);
//                float phaseOut = 2 * k2 * R - phase2;
//                outputPhase[i][2 * j] = factor2 * (float) Math.sin(phaseOut);
//                outputPhase[i][2 * j + 1] = -factor2 * (float) Math.sin(phaseOut);
            }
        }

        this.kernel2 = padComplexArray(M, N, kernel2, 2);
        fft.complexForward(this.kernel2);
    }

    /**
     * Function to pad complex arrays. The complex array is padded into a matrix
     * with @code{pad*M x pad*N} elements.
     *
     * @param M size of a
     * @param N size of a
     * @param a input array
     * @param pad padding factor
     * @return padded array
     */
    private float[][] padComplexArray(int M, int N, float[][] a, int pad) {
        if (pad <= 1) {
            return a;
        }

        float[][] padded = new float[pad * M][pad * 2 * N];

        int iStart = (pad - 1) * M / 2;
        int jStart = (pad - 1) * N / 2;

        for (int i = 0, i2 = iStart; i < M; i++, i2++) {
            for (int j = 0, j2 = jStart; j < N; j++, j2++) {
                padded[i2][2 * j2] = a[i][2 * j];
                padded[i2][2 * j2 + 1] = a[i][2 * j + 1];
            }
        }

        return padded;
    }

    /**
     * Extracts a portion of a complex padded array. The extracted complex array
     * will have @code{Mout x Nout} complex elements.
     *
     * @param M size of padded
     * @param N size of padded
     * @param padded input padded array
     * @param Mout size of output
     * @param Nout size of output
     * @return output matrix
     */
    private void unpadComplexArray(int M, int N, float[][] padded, int Mout,
            int Nout, float[][] a) {

        int iStart = (M - Mout) / 2;
        int jStart = (N - Nout) / 2;

        for (int i = 0, i2 = iStart; i < Mout; i++, i2++) {
            for (int j = 0, j2 = jStart; j < Nout; j++, j2++) {
                a[i][2 * j] = padded[i2][2 * j2];
                a[i][2 * j + 1] = padded[i2][2 * j2 + 1];
            }
        }
    }

    /**
     * Extracts a portion of a padded array. The extracted array will have
     * @code{Mout x Nout} elements.
     *
     * @param M size of padded
     * @param N size of padded
     * @param padded input padded array
     * @param Mout size of output
     * @param Nout size of output
     * @return output matrix
     */
    private void unpadArray(int M, int N, float[][] padded, int Mout, int Nout,
            float[][] a) {

        int iStart = (M - Mout) / 2;
        int jStart = (N - Nout) / 2;

        for (int i = 0, i2 = iStart; i < Mout; i++, i2++) {
            for (int j = 0, j2 = jStart; j < Nout; j++, j2++) {
                a[i][j] = padded[i2][j2];
            }
        }
    }

    public void diffract(float[][] field) {
        if (M != field.length || N != (field[0].length / 2)) {
            throw new IllegalArgumentException("Array dimension must be " + M + " x " + 2 * N + ".");
        }

        ArrayUtils.complexMultiplication2(field, kernel1);
        float[][] paddedField = padComplexArray(M, N, field, 2);

        fft.complexForward(paddedField);

        ArrayUtils.complexMultiplication2(paddedField, kernel2);

        fft.complexInverse(paddedField, true);

        ArrayUtils.complexShift(paddedField);

        unpadComplexArray(2 * M, 2 * N, paddedField, M, N, field);

        //(not important for the intended use of this class)
//        ArrayUtils.complexMultiplication2(field, outputPhase);
    }

    public float[][] interpolate(float[][] a) {
        if (M != a.length || N != (a[0].length)) {
            throw new IllegalArgumentException("Array dimension must be " + M + " x " + N + ".");
        }

        float stepM = M / (M - 1.0f);
        float stepN = N / (N - 1.0f);

        int M2 = (M / 2) - 1;
        int N2 = (N / 2) - 1;

        float z2 = z * z;

        float r_max = (float) Math.sqrt(1 - (M2 * M2 * dxOut * dxOut / z2) - (N2 * N2 * dyOut * dyOut / z2));
        float mpMin = -M2 / r_max;
        float npMin = -N2 / r_max;

        int mpMax = (int) Math.ceil((M2 + 1) / r_max);
        int npMax = (int) Math.ceil((N2 + 1) / r_max);

        float[][] tmp = new float[2 * mpMax][2 * npMax];

        for (int i = 0, m = -M2; i < M; i++, m++) {

            float rx = 1 - (m * m * dxOut * dxOut / z2);

            for (int j = 0, n = -N2; j < N; j++, n++) {

                float r = (float) Math.sqrt(rx - (n * n * dyOut * dyOut / z2));

                float mp = m / r - mpMin;// + (1);//
                float np = n / r - npMin;// + (1);//

                int imp = (int) Math.floor(mp);//
                int inp = (int) Math.floor(np);//

                //weights
                float x1frac = (imp + 1.0f) - mp;
                float x2frac = 1.0f - x1frac;
                float y1frac = (inp + 1.0f) - np;
                float y2frac = 1.0f - y1frac;

                //areas
                float x1y1 = x1frac * y1frac;
                float x1y2 = x1frac * y2frac;
                float x2y1 = x2frac * y1frac;
                float x2y2 = x2frac * y2frac;

                //interpolation
                if (imp > 0 && imp < 2 * mpMax - 1 && inp > 0 && inp < 2 * npMax - 1) {
                    tmp[imp][inp] = tmp[imp][inp] + x1y1 * a[i][j];
                    tmp[imp + 1][inp] = tmp[imp + 1][inp] + x2y1 * a[i][j];
                    tmp[imp][inp + 1] = tmp[imp][inp + 1] + x1y2 * a[i][j];
                    tmp[imp + 1][inp + 1] = tmp[imp + 1][inp + 1] + x2y2 * a[i][j];
                }
            }
        }

        float[][] out = new float[M][N];
        unpadArray(2 * mpMax, 2 * npMax, tmp, M, N, out);
        return out;
    }

}
