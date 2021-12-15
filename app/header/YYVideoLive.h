//
// Created by Administrator on 2019/11/4.
//

#ifndef YYVIDEOPLAYER_ANDROID_YYVIDEOLIVE_H
#define YYVIDEOPLAYER_ANDROID_YYVIDEOLIVE_H

#include <jni.h>
#ifdef __cplusplus
extern "C" {
#endif

jint JNI_OnLoad_YYVideoLive(JavaVM *vm , void *reserved);
void JNI_OnUnLoad_YYVideoLive(JavaVM *vm , void *reserved);

#ifdef __cplusplus
}
#endif


#endif //YYVIDEOPLAYER_ANDROID_YMFLIVE_H
