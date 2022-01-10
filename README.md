# DLHM

Digital Lens-less Holographic Microscopy(DLHM) is an plugin developed for [ImageJ](https://imagej.nih.gov/ij/index.html) or any of its derivate platforms like [Fiji](https://fiji.sc/) or [Micro-Manager](https://micro-manager.org/) that enables the numerical reconstruction of digitally recorded holograms of digital lens-less holographic microscopy. The plugin can be used for teaching and research purposes.

The DLHM plugin is composed of 2 different modules: the simulation module implements a discrete version of the Rayleigh–Somerfield diffraction formula, which allows the user to directly build a simulated hologram from a known phase and/or amplitude object by just introducing the geometry parameters of the simulated setup; the plugin’s reconstruction module implements a discrete version of the Kirchhoff–Helmholtz diffraction integral, thus allowing the user to reconstruct DLHM holograms by introducing the parameters of the acquisition setup and the desired reconstruction distance. The plugin offers the two said modules within the robust environment provided by a complete set of built-in tools for image processing available in [ImageJ](https://imagej.nih.gov/ij/index.html).

Detailed and updated information about this project can be found in the [project page](https://unal-optodigital.github.io/DLHM/).


## Downloads
The installation process is the standard procedure for any ImageJ plugin. Just download either the [JAR](https://drive.google.com/file/d/1-Pp78zflVXOSH2jLV8e2qzhf2D79lcrC/view?usp=sharing) and [libs](https://drive.google.com/file/d/1RBmpqAbyP8xz170bOnNPiQ4u4TuLRQMg/view?usp=sharing) file and extract its contents under the `imagej/plugins` folder. Once installed, you should be able to access the plugin at `OD > DLHM`


## Reference
Further information about this plugin and its functional modules can be found in the following publications. These are also the preferred way of citing this tool if you are implementing it in your own works.
- Carlos Trujillo, Pablo Piedrahita-Quintero, and Jorge Garcia-Sucerquia, "Digital lensless holographic microscopy: numerical simulation and reconstruction with ImageJ," Appl. Opt. 59, 5788-5795 (2020).
- DOI: [10.1364/AO.395672](https://doi.org/10.1364/AO.395672)
- J. García-Sucerquia, C. Trujillo and J. F. Restrepo, "MICROSCOPIO, HOLOGRÁFICO SIN LENTES (MHDSL) Y MÉTODO PARA VISUALIZAR MUESTRAS," U.S. patent 7620179 (2018).

## Credits
dLHM uses [JDiffraction](https://unal-optodigital.github.io/JDiffraction/) and [JTransforms](https://sites.google.com/site/piotrwendykier/software/jtransforms) numerical routines.
