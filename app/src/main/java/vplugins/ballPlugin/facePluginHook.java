package vplugins.ballPlugin;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.graphics.SurfaceTexture;

public class facePluginHook implements IXposedHookLoadPackage {
	private static final String TAG = "LSPosed";

	private static final String yy = "com.duowan.mobile";
	private static final String douyin = "com.ss.android.ugc.aweme";
	private static final String tiktok = "com.ss.android.ugc.trill";

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Log.e(TAG, "handleLoadPackage = " + lpparam.packageName);
		if (lpparam.packageName.equals(douyin)) {

			Log.e(TAG, "hooked in : " + douyin);

//			Tracer.traceClass(lpparam, "android.hardware.camera2");
//			Tracer.traceClass(lpparam, "android.media.ImageReader");
			// Tracer.traceClass(lpparam, "com.ss.android.vendor.HwCameraKit");
			// Tracer.traceClass(lpparam, "com.huawei.camera.camerakit");
			// Tracer.traceClass(lpparam, "com.google.ar.core.o");
			// Tracer.traceClass(lpparam, "com.ss.android.ttve.mediacodec");
			//Tracer.traceClass(lpparam, "com.ss.android.ttvecamera.k");
			//Tracer.traceClass(lpparam, "com.ss.android.ttvecamera.TECameraFrame");
			// Tracer.traceClass(lpparam, "com.ss.avframework");
			//Tracer.traceClass(lpparam, "com.ss.android.ttvecamera.TEVendorCamera");
//
//			Tracer.traceClass(lpparam, "com.ss.android.medialib.camera");
			//Tracer.traceClass(lpparam, "android.hardware.camera2.CaptureRequest");
			// Tracer.traceClass(lpparam, "com.ss.video.rtc.engine");

			//Tracer.traceClass(lpparam, "com.ss.android.vesdk");
			//Tracer.traceClass(lpparam, "com.ss.ttm.player.EGLSurfaceTexture");

			// Tracer.traceClass(lpparam, "com.ss.android.medialib");
			// Tracer.traceClass(lpparam, "com.ss.texturerender.effect");


			// Tracer.traceClass(lpparam, "com.bytedance.android.live.broadcast.stream.capture");
			// Tracer.traceClass(lpparam, "com.ss.android.medialib.presenter");
			// Tracer.traceClass(lpparam, "com.ss.android.ttve.mediacodec");


//
//			Tracer.traceClass(lpparam, "com.bef.effectsdk");
			Tracer.traceClass(lpparam, "com.ss.android.ttvecamera");
			Tracer.traceClass(lpparam, "com.bytedance.android.live.broadcast.widget");
			Tracer.traceClass(lpparam, "com.ss.android.ttve");
//			Tracer.traceClass(lpparam, "androidx.fragment.app");
//			Tracer.traceClass(lpparam, "com.ss.android.ttvecamera.k.e");
//			Tracer.traceClass(lpparam, "com.ss.android.ttvecamera.TECameraFrame");


			// CameraHelper.hookGetCameraCharacteristics(lpparam.classLoader);
			// CameraHelper.hookHwCameraKit(lpparam.classLoader);
			// CameraHelper.hookOnCaptureCompleted(lpparam.classLoader);

//			CameraHelper.hookG_LIZIZ(lpparam.classLoader);
//			CameraHelper.hookOnFrameAvailable(lpparam.classLoader);
			CameraHelper.hookGetSurfaceTextureSize(lpparam.classLoader);
			CameraHelper.hookGetContext(lpparam.classLoader);
			CameraHelper.hookGetCaptureRequestBuilder(lpparam.classLoader);
			CameraHelper.hookCreateCameraSession(lpparam.classLoader);
			CameraHelper.hookShareButton(lpparam.classLoader);

//			CameraHelper.hookImageReader(lpparam.classLoader);
//			CameraHelper.hookTECameraFrame(lpparam.classLoader);

//			CameraHelper.hookOpenCamera(lpparam.classLoader);
		}
	}
}
