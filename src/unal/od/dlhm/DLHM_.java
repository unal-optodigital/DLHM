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

package unal.od.dlhm;

import unal.od.dlhm.rec.ReconstructionFrame;
import unal.od.dlhm.sim.SimulationFrame;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import unal.od.jdiffraction.cpu.utils.ArrayUtils;

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public class DLHM_ implements PlugIn {

    private static ReconstructionFrame RECONSTRUCTION_FRAME;
    private static SimulationFrame SIMULATION_FRAME;
    private static final String IMAGEJ_VERSION = "1.48s";
    private static final String JDIFFRACTION_VERSION = "1.2";

    private static final String ABOUT = "DLHM v1.0\n"
            + "Pablo Piedrahita-Quintero\n"
            + "Carlos Trujillo\n"
            + "Jorge Garcia-Sucerquia\n"
            + "Grupo de Procesamiento Optodigital\n"
            + "Universidad Nacional de Colombia - Sede Medellín";

    public DLHM_() {

    }

    @Override
    public void run(String arg) {

        if (IJ.versionLessThan(IMAGEJ_VERSION)) {
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (UnsupportedLookAndFeelException e) {
        }

        if (arg.equalsIgnoreCase("about")) {
            showAbout();
            return;
        }

        if (!ArrayUtils.jDiffractionVersion().equals(JDIFFRACTION_VERSION)) {
            IJ.error("Please update JDiffraction to the latest version.\n"
                    + "http://unal-optodigital.github.io/JDiffraction/");
            return;
        }

        if (arg.equalsIgnoreCase("simulation")) {
            if (SIMULATION_FRAME == null || !SIMULATION_FRAME.isDisplayable()) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        SIMULATION_FRAME = new SimulationFrame();
                        SIMULATION_FRAME.setVisible(true);
                    }
                });

            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        SIMULATION_FRAME.setVisible(true);
                        SIMULATION_FRAME.toFront();
                    }
                });
                
            }

            return;
        }

        if (RECONSTRUCTION_FRAME == null || !RECONSTRUCTION_FRAME.isDisplayable()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    RECONSTRUCTION_FRAME = new ReconstructionFrame();
                    RECONSTRUCTION_FRAME.setVisible(true);
                }
            });
            
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    RECONSTRUCTION_FRAME.setVisible(true);
                    RECONSTRUCTION_FRAME.toFront();
                }
            });
            
        }
    }

    private void showAbout() {
        new AboutFrame().setVisible(true);
    }

    public static void main(String[] args) {
        new ImageJ();

        String rute = "60411a0000.pgm";
        File f = new File(rute);
        System.out.println(f.getAbsolutePath());
        IJ.open(f.getAbsolutePath());
        IJ.getImage().getCanvas().zoomOut(0, 0);
        IJ.getImage().getCanvas().zoomOut(0, 0);
        IJ.getImage().getCanvas().zoomOut(0, 0);
        
        String rute1 = "ref120000.pgm";
        File f1 = new File(rute1);
        System.out.println(f1.getAbsolutePath());
        IJ.open(f1.getAbsolutePath());
        IJ.getImage().getCanvas().zoomOut(0, 0);
        IJ.getImage().getCanvas().zoomOut(0, 0);
        IJ.getImage().getCanvas().zoomOut(0, 0);

        String rute2 = "diatom.tif";
        File f2 = new File(rute2);
        System.out.println(f2.getAbsolutePath());
        IJ.open(f2.getAbsolutePath());
        IJ.getImage().getCanvas().zoomOut(0, 0);
        IJ.getImage().getCanvas().zoomOut(0, 0);
        IJ.getImage().getCanvas().zoomOut(0, 0);
        
        String rute3 = "diatom_inv.tif";
        File f3 = new File(rute3);
        System.out.println(f3.getAbsolutePath());
        IJ.open(f3.getAbsolutePath());
        IJ.getImage().getCanvas().zoomOut(0, 0);
        IJ.getImage().getCanvas().zoomOut(0, 0);
        IJ.getImage().getCanvas().zoomOut(0, 0);
        
        new DLHM_().run("");
        new DLHM_().run("simulation");
        new DLHM_().run("about");
    }
}
