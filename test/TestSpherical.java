
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jpied
 */
public class TestSpherical {

    private static final int M = 1024;
    private static final int N = M;
    
    private static final float z = 100;
    private static final float lambda = 0.405f;
    private static final float dxSample = 0.12f;
    private static final float dySample = dxSample;
    
    private static float[][] spherical(float sampleMax, float sampleMin, float ratio) {
        float[][] wave = new float[M][2 * N];

        int M2 = (M / 2) - 1;
        int N2 = (N / 2) - 1;

        float z2 = z * z;
        float k = 2 * (float) Math.PI / lambda;

        float ampMax = 1 / z;
        float ampMin = 1 / (float) Math.sqrt(z2 + (M2 * M2 * dxSample * dxSample) + (N2 * N2 * dySample * dySample));
        float ampDelta = ampMax - ampMin;

        float sampleDelta = sampleMax - sampleMin;

        for (int i = 0, m = -M2; i < M; i++, m++) {

            float rx = z2 + m * m * dxSample * dxSample;

            for (int j = 0, n = -N2; j < N; j++, n++) {

                float r = (float) Math.sqrt(rx + n * n * dySample * dySample);
                float phase = k * r;

                //escalates amplitude to 0-1
                float factor = ((1 / r) - ampMin) / ampDelta;
                factor = (factor * sampleDelta + sampleMin) / ratio;

                wave[i][2 * j] = factor * (float) Math.cos(phase);
                wave[i][2 * j + 1] = factor * (float) Math.sin(phase);
            }
        }

        return wave;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        new ImageJ();
        
        float[][] wave = TestSpherical.spherical(10, 5, 0.5f);
        float[][] amplitude = ArrayUtils.modulus(wave);
        
        ImageProcessor ip = new FloatProcessor(amplitude);
        ImagePlus imp = new ImagePlus("amp", ip);
        imp.show();
        
    }
    
}
