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

/**
 *
 * @author: Pablo Piedrahita-Quintero <jppiedrahitaq@unal.edu.co>
 * @author: Carlos Trujillo <catrujila@unal.edu.co>
 * @author: Jorge Garcia-Sucerquia <jisucerquia@unal.edu.co>
 */
public interface PreferencesKeys {

    //reconstruction frame parameters
    final static String REC_FRAME_LOC_X = "REC_FRAME_LOC_X";
    final static String REC_FRAME_LOC_Y = "REC_FRAME_LOC_Y";

    final static String REC_LAMBDA = "REC_LAMBDA";
    final static String REC_RECONSTRUCTION_DISTANCE = "REC_RECONSTRUCTION_DISTANCE";
    final static String REC_SOURCE_TO_SCREEN_DISTANCE = "REC_SOURCE_TO_SCREEN_DISTANCE";
    final static String REC_INPUT_WIDTH = "REC_INPUT_WIDTH";
    final static String REC_INPUT_HEIGHT = "REC_INPUT_HEIGHT";
    final static String REC_OUTPUT_WIDTH = "REC_OUTPUT_WIDTH";
    final static String REC_OUTPUT_HEIGHT = "REC_OUTPUT_HEIGHT";
    final static String REC_STEP = "REC_STEP";
    final static String REC_PHASE_CHECKED = "REC_PHASE_CHECKED";
    final static String REC_AMPLITUDE_CHECKED = "REC_AMPLITUDE_CHECKED";
    final static String REC_INTENSITY_CHECKED = "REC_INTENSITY_CHECKED";
    final static String REC_REAL_CHECKED = "REC_REAL_CHECKED";
    final static String REC_IMAGINARY_CHECKED = "REC_IMAGINARY_CHECKED";
    final static String REC_RELATION_LOCK = "REC_RELATION_LOCK";
    final static String REC_LOG_WRAPPING = "REC_LOG_WRAPPING";
    
    final static String REC_IS_MANUAL = "REC_IS_MANUAL";

    //units
    final static String REC_LAMBDA_UNITS = "REC_LAMBDA_UNITS";
    final static String REC_RECONSTRUCTION_DISTANCE_UNITS = "REC_RECONSTRUCTION_DISTANCE_UNITS";
    final static String REC_SOURCE_TO_SCREEN_DISTANCE_UNITS = "REC_SOURCE_TO_SCREEN_DISTANCE_UNITS";
    final static String REC_INPUT_SIZE_UNITS = "REC_INPUT_SIZE_UNITS";
    final static String REC_OUTPUT_SIZE_UNITS = "REC_OUTPUT_SIZE_UNITS";

    //propagation
    final static String REC_COSINE_FILTER = "REC_COSINE_FILTER";
    final static String REC_COSINE_BORDER_WIDTH = "REC_COSINE_BORDER_WIDTH";
    final static String REC_CONTRAST_TYPE = "REC_CONTRAST_TYPE";
    final static String REC_AVERAGE_DIMENSION = "REC_AVERAGE_DIMENSION";
    
    //log scaling
    final static String REC_PHASE_8_BIT = "REC_PHASE_8_BIT";
    final static String REC_AMPLITUDE_8_BIT = "REC_AMPLITUDE_8_BIT";
    final static String REC_INTENSITY_8_BIT = "REC_INTENSITY_8_BIT";
    final static String REC_REAL_8_BIT = "REC_REAL_8_BIT";
    final static String REC_IMAGINARY_8_BIT = "REC_IMAGINARY_8_BIT";

    //8bit scaling
    final static String REC_AMPLITUDE_LOG = "REC_AMPLITUDE_LOG";
    final static String REC_INTENSITY_LOG = "REC_INTENSITY_LOG";

    //batch frame
    final static String REC_BATCH_START = "REC_BATCH_START";
    final static String REC_BATCH_END = "REC_BATCH_END";
    final static String REC_BATCH_STEP = "REC_BATCH_STEP";
    final static String REC_BATCH_PLANES = "REC_BATCH_PLANES";

    final static String REC_IS_STEP = "REC_IS_STEP";
    final static String REC_MAX_PLANES = "REC_MAX_PLANES";
    
    //simulation frame settings
    final static String SIM_FRAME_LOC_X = "SIM_FRAME_LOC_X";
    final static String SIM_FRAME_LOC_Y = "SIM_FRAME_LOC_Y";

    final static String SIM_LAMBDA = "SIM_LAMBDA";
    final static String SIM_SOURCE_TO_SCREEN_DISTANCE = "SIM_SOURCE_TO_SCREEN_DISTANCE";
    final static String SIM_SOURCE_TO_SAMPLE_DISTANCE = "SIM_SOURCE_TO_SAMPLE_DISTANCE";
    final static String SIM_SCREEN_WIDTH = "SIM_SCREEN_WIDTH";
    final static String SIM_SCREEN_HEIGHT = "SIM_SCREEN_HEIGHT";
    final static String SIM_SAMPLE_WIDTH = "SIM_SAMPLE_WIDTH";
    final static String SIM_SAMPLE_HEIGHT = "SIM_SAMPLE_HEIGHT";
    final static String SIM_HOLOGRAM_CHECKED = "SIM_HOLOGRAM_CHECKED";
    final static String SIM_REFERENCE_CHECKED = "SIM_REFERENCE_CHECKED";
    final static String SIM_CONTRAST_CHECKED = "SIM_CONTRAST_CHECKED";
    final static String SIM_RELATION_LOCK = "SIM_RELATION_LOCK";
    final static String SIM_LOG_WRAPPING = "SIM_LOG_WRAPPING";
    
    final static String SIM_IS_MANUAL = "SIM_IS_MANUAL";
    
    //units
    final static String SIM_LAMBDA_UNITS = "SIM_LAMBDA_UNITS";
    final static String SIM_SOURCE_TO_SAMPLE_DISTANCE_UNITS = "SIM_SOURCE_TO_SAMPLE_DISTANCE_UNITS";
    final static String SIM_SOURCE_TO_SCREEN_DISTANCE_UNITS = "SIM_SOURCE_TO_SCREEN_DISTANCE_UNITS";
    final static String SIM_SCREEN_SIZE_UNITS = "SIM_SCREEN_SIZE_UNITS";
    final static String SIM_SAMPLE_SIZE_UNITS = "SIM_SAMPLE_SIZE_UNITS";
}
