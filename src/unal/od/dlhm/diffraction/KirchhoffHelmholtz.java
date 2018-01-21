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
public class KirchhoffHelmholtz {

    private final int M, N;
    private final float z, L, lambda, dx, dy, dxOut, dyOut;
    private float xo, yo, Xo, Yo, dX, dY;

    private float[][] kernel1, kernel2, outputPhase;
    private final FloatFFT_2D fft;

    public KirchhoffHelmholtz(int M, int N, float lambda, float z, float L, float dx,
            float dy, float dxOut, float dyOut) {

        this.M = M;
        this.N = N;
        this.lambda = lambda;
        this.dx = dx;
        this.dy = dy;
        this.dxOut = dxOut;
        this.dyOut = dyOut;
        this.z = z;
        this.L = L;

        kernel1 = new float[M][2 * N];
        outputPhase = new float[M][2 * N];
        fft = new FloatFFT_2D(2 * M, 2 * N);

        calculateKernels();
    }

    private void calculateKernels() {
        float L2 = L * L;
        float z2 = z * z;

        //hologram coordinates
        xo = -dx * M / 2; //first coordinate
        yo = -dy * N / 2;

        float xf = dx * (M / 2 - 1); //last coordinate
        float yf = dy * (N / 2 - 1);

        //transformed coordinates
        Xo = (float) ((xo * L) / Math.sqrt(L2 + xo * xo)); //first transformed coordinate
        Yo = (float) ((yo * L) / Math.sqrt(L2 + yo * yo));

        float Xf = (float) ((xf * L) / Math.sqrt(L2 + xf * xf)); //last transformed coordinate
        float Yf = (float) ((yf * L) / Math.sqrt(L2 + yf * yf));

        //pixel size for the transformed hologram
        dX = (Xf - Xo) / M;
        dY = (Yf - Yo) / N;

        int M2 = (M / 2) - 1;
        int N2 = (N / 2) - 1;

        float k = 2 * (float) Math.PI / lambda;
        float factor = k / L2;
        float factor2 = k / (2 * L);

        float[][] kernel2 = new float[M][2 * N];

        for (int i = 0, m = -M2; i < M; i++, m++) {

            float Rx = L2 - (m * m * dX * dX);
            float rpx = z2 + (m * m * dxOut * dxOut);
            float a = m * m * dX * dxOut;

            for (int j = 0, n = -N2; j < N; j++, n++) {

                float R = (float) Math.sqrt(Rx - (n * n * dY * dY));
                float rp2 = rpx + (n * n * dyOut * dyOut);

                float phase1 = -factor * R * ((z * L) - (rp2 / 2));
                float phase2 = -factor2 * (a + (n * n * dY * dyOut));

                float factor3 = (-0.5f / lambda) * (1 / (R * R)) * (1 + (R / L));
                float phase = phase1 + phase2;
                kernel1[i][2 * j] = -factor3 * (float) Math.sin(phase);
                kernel1[i][2 * j + 1] = factor3 * (float) Math.cos(phase);

                kernel2[i][2 * j] = (float) Math.cos(-phase2);
                kernel2[i][2 * j + 1] = (float) Math.sin(-phase2);

                //output plane phase
                outputPhase[i][2 * j] = dX * dY * (float) Math.cos(phase2);
                outputPhase[i][2 * j + 1] = dX * dY * (float) Math.sin(phase2);
            }
        }

        this.kernel2 = padComplexArray(M, N, kernel2, 2);
        fft.complexForward(this.kernel2);
    }

    /**
     * Function to pad arrays. The array is padded into a matrix with
     * @code{pad*M x pad*N} elements.
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

    public float[][] interpolate(float[][] holo) {
        if (M != holo.length || N != (holo[0].length)) {
            throw new IllegalArgumentException("Array dimension must be " + M + " x " + N + ".");
        }

        float[][] tmp = new float[M][N];

        float L2 = L * L;

        int M2 = M / 2;
        int N2 = N / 2;

        int endM = M - 1;
        int endN = N - 1;

        for (int i = 0; i < M2; i++) {
            for (int j = 0; j < N2; j++) {

                float X = Xo + i * dX;
                float Y = Yo + j * dY;

                float R = (float) (1.0f / Math.sqrt(L2 - X * X - Y * Y));

                float newX = X * L * R;
                float newY = Y * L * R;

                float xc = (newX - xo) / dx;
                float yc = (newY - yo) / dy;

                int ixc = (int) Math.floor(xc);
                int iyc = (int) Math.floor(yc);

                float x1frac = ixc + 1.0f - xc;
                float x2frac = 1.0f - x1frac;
                float y1frac = iyc + 1.0f - yc;
                float y2frac = 1.0f - y1frac;

                float x1y1 = x1frac * y1frac;
                float x1y2 = x1frac * y2frac;
                float x2y1 = x2frac * y1frac;
                float x2y2 = x2frac * y2frac;

                //Teniendo todos los valores listos, ahora hacemos el "remapeo" sobre el holograma
                if (ixc > 0 && ixc < M / 2 && iyc > 0 && iyc < N / 2) {
                    //Cuadrante 1
                    tmp[i][j] = x1y1 * holo[ixc][iyc]
                            + x2y1 * holo[ixc + 1][iyc]
                            + x1y2 * holo[ixc][iyc + 1]
                            + x2y2 * holo[ixc + 1][iyc + 1];

                    //Cuadrante 2
                    tmp[endM - i][j] = x1y1 * holo[endM - ixc][iyc]
                            + x2y1 * holo[endM - (ixc + 1)][iyc]
                            + x1y2 * holo[endM - ixc][iyc + 1]
                            + x2y2 * holo[endM - (ixc + 1)][iyc + 1];

                    //Cuadrante 3
                    tmp[i][endN - j] = x1y1 * holo[ixc][endN - iyc]
                            + x2y1 * holo[ixc + 1][endN - iyc]
                            + x1y2 * holo[ixc][endN - (iyc + 1)]
                            + x2y2 * holo[ixc + 1][endN - (iyc + 1)];

                    //Cuadrante 4
                    tmp[endM - i][endN - j] = x1y1 * holo[endM - ixc][endN - iyc]
                            + x2y1 * holo[endM - (ixc + 1)][endN - iyc]
                            + x1y2 * holo[endM - ixc][endN - (iyc + 1)]
                            + x2y2 * holo[endM - (ixc + 1)][endN - (iyc + 1)];
                }
            }
        }

        return ArrayUtils.complexAmplitude2(tmp, null);
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

        ArrayUtils.complexMultiplication2(field, outputPhase);
    }
}
