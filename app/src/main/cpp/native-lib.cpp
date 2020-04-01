#include <jni.h>
#include <string>

#include "mucom_module.h"


int MucomPlay(MucomModule *module, const char *directory, const char *filename) {
    if (module == NULL) return -1;
    bool result = module->Open(directory, filename);
    if (!result) return -1;
    result = module->Play();
    if (!result) return -1;
    return 0;
}

const char *MucomGetMessage(MucomModule *module) {
    if (!module) return "";
    return module->GetResult();
}

void MucomMix(MucomModule *module, short *buffer,int samples) {
    if (module == NULL) return;
    module->Mix(buffer, samples);
}

void MucomClose(MucomModule *module) {
    if (module == NULL) return;
    module->Close();
    delete module;
    module = NULL;
}


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_bkc_mucom88_AudioRenderer_getMucomInstance(JNIEnv *env, jobject instance) {
    MucomModule *module = new MucomModule();
    int size = sizeof(module);
    jbyteArray result = env->NewByteArray(size);
    env->SetByteArrayRegion(result, 0, size, (const jbyte *)&module);
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_bkc_mucom88_AudioRenderer_playSong(JNIEnv *env, jobject instance,
                                            jbyteArray mucomInstance_, jstring workDirectory_,
                                            jstring filename_) {
    jbyte *mucomInstance = env->GetByteArrayElements(mucomInstance_, NULL);
    const char *workDirectory = env->GetStringUTFChars(workDirectory_, 0);
    const char *filename = env->GetStringUTFChars(filename_, 0);

    MucomModule *module;
    memcpy(&module, mucomInstance,sizeof(module));
    char dirname[512];
    char fname[256];
    strcpy(dirname, workDirectory);
    strcpy(fname, filename);

    env->ReleaseByteArrayElements(mucomInstance_, mucomInstance, 0);
    env->ReleaseStringUTFChars(workDirectory_, workDirectory);
    env->ReleaseStringUTFChars(filename_, filename);

    return MucomPlay(module, dirname, filename);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_bkc_mucom88_AudioRenderer_getResultFromMucom(JNIEnv *env, jobject instance,
                                                      jbyteArray mucomInstance_) {
    jbyte *mucomInstance = env->GetByteArrayElements(mucomInstance_, NULL);
    MucomModule *module;
    memcpy(&module, mucomInstance,sizeof(module));
    env->ReleaseByteArrayElements(mucomInstance_, mucomInstance, 0);

    const char *msg = MucomGetMessage(module);

    int size = strlen(msg);
    jbyteArray result = env->NewByteArray(size);
    env->SetByteArrayRegion(result, 0, size, (const jbyte *)msg);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bkc_mucom88_AudioRenderer_closeSong(JNIEnv *env, jobject instance,
                                             jbyteArray mucomInstance_) {
    jbyte *mucomInstance = env->GetByteArrayElements(mucomInstance_, NULL);
    MucomModule *module;
    memcpy(&module, mucomInstance,sizeof(module));
    env->ReleaseByteArrayElements(mucomInstance_, mucomInstance, 0);
    MucomClose(module);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bkc_mucom88_AudioRenderer_renderingSong(JNIEnv *env, jobject instance,
                                                 jbyteArray mucomInstance_, jshortArray buf_,
                                                 jint samples) {
    jbyte *mucomInstance = env->GetByteArrayElements(mucomInstance_, NULL);
    jshort *buf = env->GetShortArrayElements(buf_, NULL);
    MucomModule *module;
    memcpy(&module, mucomInstance,sizeof(module));

    MucomMix(module, buf, samples);
    env->ReleaseByteArrayElements(mucomInstance_, mucomInstance, 0);
    env->ReleaseShortArrayElements(buf_, buf, 0);
}