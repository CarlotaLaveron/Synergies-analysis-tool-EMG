#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

#include "operations_EmgNative.h"   // header generado por javac
#include "emg.h"
#include "queue.h"
#include "moving_average.h"
#include "peak_to_peak.h"

JNIEXPORT jdoubleArray JNICALL Java_operations_EmgNative_filterChannel
  (JNIEnv *env, jobject obj,
   jdoubleArray jSamples,
   jint sampleFrequency,
   jdouble rangeSeconds,
   jint minEmgFreq,
   jint maxEmgFreq,
   jboolean highPassOn) {

    // Longitud del array de entrada
    jsize len = (*env)->GetArrayLength(env, jSamples);

    // Obtener puntero a los datos Java
    jdouble *samples = (*env)->GetDoubleArrayElements(env, jSamples, NULL);
    if (samples == NULL) {
        return NULL;
    }

    // Opción del filtro pasa alto
    EMG_OPTIONS hpOpt = highPassOn ? HIGH_PASS_FILTER_ON : HIGH_PASS_FILTER_OFF;

    // Crear estructura EMG (ver emg.h)
    EMG *emg = new_EMG(
        (uint16_t) sampleFrequency,
        (float) rangeSeconds,
        (uint16_t) minEmgFreq,
        (uint16_t) maxEmgFreq,
        hpOpt,
        REFERENCE_UNAVAILABLE
    );

    if (emg == NULL) {
        (*env)->ReleaseDoubleArrayElements(env, jSamples, samples, 0);
        return NULL;
    }

    // Buffer de salida
    jdouble *outBuf = (jdouble *) malloc(len * sizeof(jdouble));
    if (outBuf == NULL) {
        free_EMG(emg);
        (*env)->ReleaseDoubleArrayElements(env, jSamples, samples, 0);
        return NULL;
    }

    // Filtrar muestra a muestra
    for (jsize i = 0; i < len; ++i) {
        double filtered = filter_EMG(emg, (double)samples[i]);
        outBuf[i] = (jdouble) filtered;
    }

    // Crear array Java de salida
    jdoubleArray jOut = (*env)->NewDoubleArray(env, len);
    if (jOut == NULL) {
        free(outBuf);
        free_EMG(emg);
        (*env)->ReleaseDoubleArrayElements(env, jSamples, samples, 0);
        return NULL;
    }

    (*env)->SetDoubleArrayRegion(env, jOut, 0, len, outBuf);

    // Limpieza
    free(outBuf);
    free_EMG(emg);
    (*env)->ReleaseDoubleArrayElements(env, jSamples, samples, 0);

    return jOut;
}
