/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import org.jtransforms.fft.FloatFFT_2D;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <calelo@gmail.com>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class KirchhoffHelmholtz {

    private final int M, N;
    private final float z, L, lambda, dx, dy, dxOut, dyOut;
    private float xo, yo, xop, yop, dxp, dyp;

    private final float[][] kernel1, kernel2, outputPhase;
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
        kernel2 = new float[M][2 * N];
        outputPhase = new float[M][2 * N];
        fft = new FloatFFT_2D(M, N);

        calculateKernels();
    }

    private void calculateKernels() {
        float L2 = L * L;
        float z2 = z * z;

        //origen coordenadas holograma
        xo = -dx * M / 2;
        yo = -dy * N / 2;

        //origen coordenadas holograma interpolado
        xop = (float) ((xo * L) / Math.sqrt(L2 + xo * xo));
        yop = (float) ((yo * L) / Math.sqrt(L2 + yo * yo));

        //tamaño pixels holograma interpolado
        float ab1 = (xo + (M - 1) * dx);
        float sqrt1 = (float) Math.sqrt(L2 + ab1 * ab1);
        float sqrt2 = (float) Math.sqrt(L2 + xo * xo);

        dxp = (L / N) * ((ab1 / sqrt1) - (xo / sqrt2));

        float ab2 = (yo + (N - 1) * dy);
        float sqrt3 = (float) Math.sqrt(L2 + ab2 * ab2);
        float sqrt4 = (float) Math.sqrt(L2 + yo * yo);

        dyp = (L / N) * ((ab2 / sqrt3) - (yo / sqrt4));

        //origen de coordenadas plano de reconstruccion
        float xOutO = -dxOut * M / 2;
        float yOutO = -dyOut * N / 2;

        float k = 2 * (float) Math.PI / lambda;
//        float kLz = k * z / L;
        float k2L = k / (2 * L);

        float tmpX = 2 * xOutO * dxp;
        float tmpY = 2 * yOutO * dyp;

        float M2 = M / 2 - 1;
        float N2 = N / 2 - 1;

        for (int i = 0; i < M; i++) {

            float a1 = (dxp * i + xop) * (dxp * i + xop);
            float a2 = i * i * dxp * dxOut;

            float a3 = (i - M2) * (i - M2) * dxp * dxOut;

            for (int j = 0; j < N; j++) {
                float b1 = (dyp * j + yop) * (dyp * j + yop);
                float Rp = (float) Math.sqrt(L2 - a1 - b1);
                float R = (float) Math.sqrt(L2 + dx * dx * i * i + dy * dy * j * j);
                float rp = (float) Math.sqrt(z2 + dxOut * dxOut * i * i + dyOut * dyOut * j * j);

                float factor = (L / Rp) * (L / Rp) * (L / Rp) * (L / Rp) //kreuzer's patent
                        * (1 + L / R) * (1 / R); //

//                float phase1 = kLz * Rp; //kreuzer's patent
                float phase1 = -0.5f * k * Rp * (rp * rp - 2 * z * L) / L2;

//                float real1 = factor * (float) Math.cos(phase1);
//                float imag1 = factor * (float) Math.sin(phase1);
//                
                float b2 = j * j * dyp * dyOut;
                float phase2 = k2L * (tmpX * i + tmpY * j + a2 + b2);

//                float real2 = (float) Math.cos(phase2);
//                float imag2 = (float) Math.sin(phase2);
//
                float phase = phase1 + phase2;

                kernel1[i][2 * j] = factor * (float) Math.cos(phase);
                kernel1[i][2 * j + 1] = factor * (float) Math.sin(phase);
//                kernel1[i][2 * j] = real1 * real2 - imag1 * imag2;
//                kernel1[i][2 * j + 1] = real1 * imag2 + real2 * imag1;
//
                //kernel2
                float b3 = (j - N2) * (j - N2) * dyp * dyOut;
                float phase3 = -k2L * (a3 + b3);

                kernel2[i][2 * j] = (float) Math.cos(phase3);
                kernel2[i][2 * j + 1] = (float) Math.sin(phase3);

                //output plane phase
                outputPhase[i][2 * j] = dxp * dyp * (float) Math.cos(phase3);
                outputPhase[i][2 * j + 1] = dxp * dyp * (float) Math.sin(phase3);
            }
        }

//        ArrayUtils.complexShift(kernel2);
        fft.complexForward(kernel2);
//        ArrayUtils.complexShift(kernel2);
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

                float x = xop + i * dxp;
                float y = yop + j * dyp;

                float rp = (float) (1.0f / Math.sqrt(L2 - x * x - y * y));

                float newX = x * L * rp;
                float newY = y * L * rp;

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
                if (iyc > 0 && iyc < N / 2 && ixc > 0 && ixc < M / 2) {
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
//        ArrayUtils.complexShift(field);
        fft.complexForward(field);
//        ArrayUtils.complexShift(field);
        ArrayUtils.complexMultiplication2(field, kernel2);
//        ArrayUtils.complexShift(field);
        fft.complexInverse(field, false);
        ArrayUtils.complexShift(field);
        ArrayUtils.complexMultiplication2(field, outputPhase);
    }
}
