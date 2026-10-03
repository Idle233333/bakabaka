#include <jni.h>
#include <string.h>

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_touhou_aichat_NativeToken_xorBytes(JNIEnv *env, jclass clazz, jbyteArray data, jbyte key) {
    (void)clazz;
    jsize len = env->GetArrayLength(data);
    jbyteArray out = env->NewByteArray(len);
    if (out == NULL) {
        return NULL;
    }
    jbyte *src = env->GetByteArrayElements(data, NULL);
    jbyte *dst = env->GetByteArrayElements(out, NULL);
    if (src == NULL || dst == NULL) {
        if (src != NULL) env->ReleaseByteArrayElements(data, src, JNI_ABORT);
        if (dst != NULL) env->ReleaseByteArrayElements(out, dst, JNI_ABORT);
        return NULL;
    }
    for (jsize i = 0; i < len; i++) {
        dst[i] = src[i] ^ key;
    }
    env->ReleaseByteArrayElements(data, src, JNI_ABORT);
    env->ReleaseByteArrayElements(out, dst, 0);
    return out;
}
