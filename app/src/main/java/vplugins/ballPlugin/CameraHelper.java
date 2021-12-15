package vplugins.ballPlugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class CameraHelper {

    private static final String TAG = "CameraHelper";
    private static OESTO2DTool mOESTO2DTool = null;
    private static String mSnapShotPath = Environment.getExternalStorageDirectory().getPath() + File.separator +
            "YYImage";

    public static int mCaptureTexturesIds = 0;
    private static byte[] mYuvCaptureBuffer = null;
    private static byte[] mRGBACaptureBuffer = null;
    private static byte[] mNV21CaptureBuffer = null;
    private static long mSnapCnt = 0;
    private static int mStartShot = 0;
    private static ByteBuffer RGBAByteBuffer = null;
    private static int mImageFormat = 0; // android.graphics.ImageFormat
    private static int mWidth = 0;
    private static int mHeight = 0;

    private static ImageReader mImageReader = null;
    private static Handler mCameraHandler = null;
    private static HandlerThread mCameraHandleThread = null;
    private static ImageReaderCallback imageReaderCallback = new ImageReaderCallback();
    private static CaptureRequest.Builder mRequestBuilder = null;
    private static Context mContext = null;
    private static int mSurfaceTextureWidth = 0;
    private static int mSurfaceTextureHeight = 0;
    public static NV21ToBitmap nv21ToBitmap = null;

    private static boolean mFisrtLoad = false;


    public static String onFrameAvailableClass1 = "com.ss.android.ttvecamera.k.g$1";

    public static String onFrameAvailableClass2 = "com.ss.android.medialib.presenter.f$12";


    private static void startCameraThread() {
        mCameraHandleThread = new HandlerThread("CameraThread");
        mCameraHandleThread.start();
        mCameraHandler = new Handler(mCameraHandleThread.getLooper());
        Log.e(TAG, " startCameraThread---->");
    }

    public static class ImageReaderCallback implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if (null == image) {
                Log.e(TAG,
                        "onImageAvailable acquireLatestImage null");
                return;
            }
            if (mStartShot > 0) {
                byte[] imageData = getDataFromImage(image);
                if (imageData != null) {
                    saveYUV2PNG(imageData, mWidth, mHeight, mStartShot);
                    mStartShot = 0;
                }
            }
            image.close();

        }
    }


    private static byte[] getDataFromImage(Image image) {
        Log.e(TAG,
                "getDataFromImage");
        Rect crop = image.getCropRect();
        mImageFormat = image.getFormat();
        mWidth = crop.width();
        mHeight = crop.height();
        int rowStride, pixelStride;
        // Read image data
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = null;
        int offset = 0;
        int bufferSize = mWidth * mHeight * ImageFormat.getBitsPerPixel(mImageFormat) / 8;
        if (mYuvCaptureBuffer == null ||
                mRGBACaptureBuffer == null ||
                mYuvCaptureBuffer != null && mYuvCaptureBuffer.length != bufferSize) {
            mYuvCaptureBuffer = new byte[mWidth * mHeight * ImageFormat.getBitsPerPixel(mImageFormat) / 8];
            mRGBACaptureBuffer = new byte[mWidth * mHeight * 4];
            mNV21CaptureBuffer = new byte[mWidth * mHeight * ImageFormat.getBitsPerPixel(mImageFormat) / 8];
        }
        byte[] rowData = new byte[planes[0].getRowStride()];
        for (int i = 0; i < planes.length; i++) {
            int shift = (i == 0) ? 0 : 1;
            buffer = planes[i].getBuffer();
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            Log.d(TAG,
                    " planes:" + i + " format:" + mImageFormat +
                            " pixelStride " + pixelStride + " rowStride " + rowStride);
            Log.d(TAG, " width " + mWidth + " height " + mHeight);

            // For multi-planar yuv images, assuming yuv420 with 2x2 chroma subsampling.
            int w = crop.width() >> shift;
            int h = crop.height() >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(mImageFormat) / 8;
                int length;
                if (pixelStride == bytesPerPixel) {
                    // Special case: optimized read of the entire row
                    length = w * bytesPerPixel;
                    buffer.get(mYuvCaptureBuffer, offset, length);
                    offset += length;
                } else {
                    // Generic case: should work for any pixelStride but slower.
                    // Use intermediate buffer to avoid read byte-by-byte from
                    // DirectByteBuffer, which is very bad for performance
                    length = (w - 1) * pixelStride + bytesPerPixel;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        mYuvCaptureBuffer[offset++] = rowData[col * pixelStride];
                    }
                }
                // Advance buffer the remainder of the row stride
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return mYuvCaptureBuffer;
    }

    public static void hookGetCaptureRequestBuilder(ClassLoader clLoader) {

        XposedHelpers.findAndHookMethod("com.ss.android.ttvecamera.b.b"
                , clLoader, "LIZLLL",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " hookGetCaptureRequestBuilder before---->");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " hookGetCaptureRequestBuilder after---->");
                        super.afterHookedMethod(param);

                    }
                });
    }

    public static void hookGetSurfaceTextureSize(ClassLoader clLoader) {
//        final Class<?> TECameraSettings =
//                XposedHelpers.findClass("com.ss.android.ttvecamera.TECameraSettings",
//                        clLoader);

//        final Class<?> TECameraSettings =
//                XposedHelpers.findField(TECameraSettings,
//                        "");

        XposedHelpers.findAndHookMethod("com.ss.android.ttvecamera.framework.b"
                , clLoader, "LJIIIZ",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " hookGhookGetSurfaceTextureSizeetContext before---->" + param.thisObject);
                        boolean faceDetection = (Boolean) XposedHelpers.getObjectField(param.thisObject,
                                "LJJIJL");
                        Log.e(TAG, " hookGhookGetSurfaceTextureSizeetContext before---->faceDetection:" + faceDetection);

//                        Log.e(TAG, " hookGetSurfaceTextureSize after---->:" +
//                                " mSurfaceTextureWidth:" + mSurfaceTextureWidth + " mSurfaceTextureHeight:"
//                                + mSurfaceTextureHeight);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " hookGetSurfaceTextureSize after---->");
                        super.afterHookedMethod(param);

                    }
                });
    }

    public static void hookGetContext(ClassLoader clLoader) {

        XposedHelpers.findAndHookMethod("com.ss.android.ttvecamera.g"
                , clLoader, "LJIJI",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " hookGetContext before---->");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " hookGetContext after---->");
                        super.afterHookedMethod(param);
                        mContext = (Context) XposedHelpers.getObjectField(param.thisObject,
                                "LJIJJ");
                        nv21ToBitmap = new NV21ToBitmap(mContext);
                        Log.e(TAG, " hookGetContext after---->" + mContext);
                    }
                });
    }


    public static void hookCreateCameraSession(ClassLoader clLoader) {


        final Class<?> list =
                XposedHelpers.findClass("java.util.List",
                        clLoader);

        final Class<?> handler =
                XposedHelpers.findClass("android.os.Handler",
                        clLoader);

        XposedHelpers.findAndHookMethod("com.ss.android.ttvecamera.b.b"
                , clLoader, "LIZ",
                list,
                handler,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!mFisrtLoad) {
                            startCameraThread();
                            if (mImageReader != null) {
                                mImageReader.close();
                            }
                            int previewWidth = 1280;
                            int previewHeight = 720;
                            mImageReader = ImageReader.newInstance(previewWidth, previewHeight,
                                    ImageFormat.YUV_420_888, 1);
                            Log.e(TAG, "ImageReader.newInstance previewWidth:" + 1280 + " previewHeight:" + previewHeight);
                            mImageReader.setOnImageAvailableListener(imageReaderCallback, mCameraHandler);
                            mFisrtLoad = true;
                        }
                        Log.e(TAG, " hookCreateCameraSession before enter---->");
                        List<Surface> sfObjectList = (List<Surface>) param.args[0];
                        try {
                            if (sfObjectList != null) {
                                mRequestBuilder = (CaptureRequest.Builder) XposedHelpers.getObjectField(param.thisObject,
                                        "LJIJ");
                                Log.e(TAG, "hookGetCaptureRequestBuilder : " + mRequestBuilder);
                                mRequestBuilder.addTarget(mImageReader.getSurface());
                                sfObjectList.add(mImageReader.getSurface());
                                Log.e(TAG, "hookCreateCameraSession sfObjectList" + sfObjectList +
                                        " add " + mImageReader.getSurface());
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.e(TAG, " hookCreateCameraSession before finish---->");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " hookCreateCameraSession after---->");
                        super.afterHookedMethod(param);
                    }
                });
    }


    public static void hookOnFrameAvailable(ClassLoader clLoader) {
        final Class<?> surface_texture =
                XposedHelpers.findClass("android.graphics.SurfaceTexture",
                        clLoader);

        XposedHelpers.findAndHookMethod(onFrameAvailableClass1
                , clLoader, "onFrameAvailable", surface_texture,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " onFrameAvailable before---->");

                        SurfaceTexture sfObject = (SurfaceTexture) param.args[0];
                        try {
                            if (sfObject != null) {

//                                Method detachFromGLContext = XposedHelpers.findMethodBestMatch(sfObject.getClass(), "detachFromGLContext");
//                                detachFromGLContext.invoke(sfObject);
//
//                                Method attachToGLContext =
//                                        XposedHelpers.findMethodBestMatch(sfObject.getClass(),
//                                                "attachToGLContext", int.class);
//                                attachToGLContext.invoke(sfObject, mCaptureTexturesIds);

                                Method updateTexImage = XposedHelpers.findMethodBestMatch(sfObject.getClass(), "updateTexImage");
                                updateTexImage.invoke(sfObject);

                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (mCaptureTexturesIds > 0) {

                            if (mOESTO2DTool == null) {
                                mOESTO2DTool = new OESTO2DTool();
                            }
                            Log.e(TAG, " mOESTO2DTool start mCaptureTexturesIds: " + mCaptureTexturesIds);
                            mOESTO2DTool.saveOESTextureToJPEG(mCaptureTexturesIds, 768, 1024);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Log.e(TAG, " onFrameAvailable after---->");
                    }
                });
    }

    public static void hookG_LIZIZ(ClassLoader clLoader) {

        XposedHelpers.findAndHookMethod("com.ss.android.ttvecamera.k.g"
                , clLoader, "LIZIZ",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        mCaptureTexturesIds = XposedHelpers.getIntField(param.thisObject,
                                "LJIIJJI");
                        Log.e(TAG, "mCaptureTexturesIds : " + mCaptureTexturesIds);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " g_LIZIZ after---->");
                        super.afterHookedMethod(param);
                    }
                });
    }


    public static void hookShareButton(ClassLoader clLoader) {

        XposedHelpers.findAndHookMethod("com.bytedance.android.live.broadcast.widget.PreviewShareWidget$b"
                , clLoader, "LIZIZ",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, "hookShareButton before---->");
                        mStartShot = 1;
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, "hookShareButton after---->");
                        super.afterHookedMethod(param);
                    }
                });

        XposedHelpers.findAndHookMethod("com.bytedance.android.live.broadcast.widget.PreviewBeautyWidget"
                , clLoader, "LIZ",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, "hookBeautyButton before---->");
                        mStartShot = 2;
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, "hookBeautyButton after---->");
                        super.afterHookedMethod(param);
                    }
                });
    }



    public static void hookHwCameraKit(ClassLoader clLoader) {
        final Class<?> Mode =
                XposedHelpers.findClass("com.huawei.camera.camerakit.Mode",
                        clLoader);

        final Class<?> Image =
                XposedHelpers.findClass("android.media.Image",
                        clLoader);

        XposedHelpers.findAndHookMethod("com.ss.android.vendor.HwCameraKit.c"
                , clLoader, "onImageAvailable", Mode, int.class, Image,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " onImageAvailable before---->");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " onImageAvailable after---->");
                        super.afterHookedMethod(param);
                    }
                });

    }

    public static void hookImageReader(ClassLoader clLoader) {

        final Class<?> Image =
                XposedHelpers.findClass("android.media.ImageReader",
                        clLoader);


        XposedHelpers.findAndHookMethod("com.ss.android.ttvecamera.k.a$4"
                , clLoader, "onImageAvailable", Image,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, "k.d onImageAvailable load success beforeHookedMethod");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, "k.d onImageAvailable load success afterHookedMethod");
                        super.afterHookedMethod(param);
                    }
                });

        XposedHelpers.findAndHookMethod("com.ss.android.ttvecamera.k.e$1"
                , clLoader, "onImageAvailable", Image,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, "k.e onImageAvailable load success beforeHookedMethod");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, "k.e onImageAvailable load success afterHookedMethod");
                        super.afterHookedMethod(param);
                    }
                });
    }

    public static void hookOpenCamera(ClassLoader clLoader) {
        Class<?> CameraDevice$StateCallback =
                XposedHelpers.findClass("android.hardware.camera2.CameraDevice$StateCallback",
                        clLoader);

        Class<?> Handler =
                XposedHelpers.findClass("android.os.Handler",
                        clLoader);

        if (Handler == null || CameraDevice$StateCallback == null) {
            Log.e(TAG, " no find class pparm");
            return;
        }

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager"
                , clLoader, "openCamera", String.class, CameraDevice$StateCallback,
                Handler,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " openCamera load success beforeHookedMethod");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " openCamera load success afterHookedMethod");

                        Throwable ex = new Throwable();
                        StackTraceElement[] stackElements = ex.getStackTrace();
                        if (stackElements != null) {
                            for (int i = 0; i < stackElements.length; i++) {

                                XposedBridge.log("Dump Stack---: " + i + ": " +
                                        stackElements[i].getClassName()
                                        + "----" + stackElements[i].getFileName()
                                        + "----" + stackElements[i].getLineNumber()
                                        + "----" + stackElements[i].getMethodName());
                            }
                            Log.e(TAG, "Dump Stack: " + "---------------over----------------");
                        }

                        super.afterHookedMethod(param);
                    }
                });
    }

    public static void hookOnCaptureCompleted(ClassLoader clLoader) {

        final Class<?> CameraCaptureSession =
                XposedHelpers.findClass("android.hardware.camera2.CameraCaptureSession",
                        clLoader);

        final Class<?> CaptureRequest =
                XposedHelpers.findClass("android.hardware.camera2.CaptureRequest",
                        clLoader);

        final Class<?> TotalCaptureResult =
                XposedHelpers.findClass("android.hardware.camera2.TotalCaptureResult",
                        clLoader);

        XposedHelpers.findAndHookMethod("com.ss.android.ttvecamera.framework.b$3"
                , clLoader, "onCaptureCompleted", CameraCaptureSession,
                CaptureRequest, TotalCaptureResult,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " onCaptureCompleted before");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " onCaptureCompleted after---->");
                        super.afterHookedMethod(param);
                    }
                });
    }

    public static void hookGetCameraCharacteristics(ClassLoader clLoader) {
        final Class<?> CameraCharacteristics =
                XposedHelpers.findClass("android.hardware.camera2.CameraCharacteristics",
                        clLoader);
        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager"
                , clLoader, "getCameraCharacteristics", String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG,
                                " getCameraCharacteristics before --->cameraId: " +
                                        param.args[0]);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.e(TAG, " getCameraCharacteristics after---->");
                        android.hardware.camera2.CameraCharacteristics result =
                                (android.hardware.camera2.CameraCharacteristics) param
                                        .getResult();

                        super.afterHookedMethod(param);
                    }
                });
    }


    public static void saveYUV2PNG(final byte[] data, final int width, final int height, int index) {
//        Thread taskThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
                mNV21CaptureBuffer = I420ToNV21(data, mWidth, mHeight);
                String fileNamePrefix = createLogFileName();
                String fileName = fileNamePrefix + ".png";
                File pictureFile = new File(fileName);
                FileOutputStream filecon = null;

                if (index == 2) {
                    saveNV21(fileNamePrefix, mNV21CaptureBuffer, width, height);
                }
                try {
                    pictureFile.createNewFile();
                    filecon = new FileOutputStream(pictureFile);
                    Log.e(TAG, "saveYUV2PNG 14:" + mNV21CaptureBuffer + " " + width + " " + height);
                    if (nv21ToBitmap != null) {
                        // 使用libyuv进行转换
                        // LibyuvUtils.I420ToRGBA(mYuvCaptureBuffer, mRGBACaptureBuffer, mWidth, mHeight);
                        // Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        //
                        Log.e(TAG, "nv21ToBitmap1 mNV21CaptureBuffer size: " + mNV21CaptureBuffer.length);
                        Bitmap bitmap = nv21ToBitmap.nv21ToBitmap(mNV21CaptureBuffer, width, height);
                        if (bitmap != null) {
                            Log.e(TAG, "nv21ToBitmap1 bitmap getByteCount size: " + bitmap.getByteCount());
                            //bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mRGBACaptureBuffer));
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, filecon);
                            Log.e(TAG, "nv21ToBitmap1 save success! " + fileName);
                        }
                    } else {
                        YuvImage yuvImage = new YuvImage(mNV21CaptureBuffer, ImageFormat.NV21, width, height,null);
                        if (yuvImage != null) {
                            yuvImage.compressToJpeg(
                                    new Rect(0, 0, width, height), 100, filecon);
                            Log.e(TAG, "YuvImage1 compressToJpeg save success! " + fileName);
                        }
                    }
                    filecon.flush();
                    filecon.close();
                } catch (IOException e) {
                    Log.e(TAG, "saveYUV2PNG exception:" + e.getMessage());
                } finally {
                    filecon = null;
                }
//            }
//        }, "YY_yyvideolib_saveYUV2JPEG_Thread");
//
//        taskThread.start();
    }

    private static void saveNV21(String fileName, byte data[], int width, int height) {
        FileOutputStream originFilecon = null;
        String originYuvFileName = fileName + ".nv21";
        File originPictureFile = new File(originYuvFileName);
        try {
            originPictureFile.createNewFile();
            originFilecon = new FileOutputStream(originPictureFile);
            originFilecon.write(data);
            originFilecon.flush();
            originFilecon.close();
            Log.e(TAG, "saveNV21 :" + originYuvFileName);
        } catch (Exception e) {
            Log.e(TAG, "saveNV21 exception:" + e.getMessage());
        }
    }

    private static String createLogFileName() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileExt = simpleDateFormate.format(date);
        File file = new File(mSnapShotPath);
        if (!file.exists() && !file.isDirectory()) {
            file.mkdir();
        }
        String fileName = mSnapShotPath + File.separator + fileExt + "_dy";
        return fileName;
    }

    public static byte[] doI420ToNV21(byte[] data, int width, int height) {
         byte[] ret = new byte[data.length];
         int total = width * height;

         ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);
         ByteBuffer bufferV = ByteBuffer.wrap(ret, total, total / 4);
         ByteBuffer bufferU = ByteBuffer.wrap(ret, total + total / 4, total / 4);

         bufferY.put(data, 0, total);
         for (int i = 0; i < total / 4; i += 1) {
             bufferU.put(data[total + i]);
             bufferV.put(data[i + total + total / 4]);
         }

         return ret;
     }

    public static byte[] I420ToNV21(byte[] data, int width, int height) {
        byte[] ret = new byte[data.length];
        int total = width * height;

        ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);
        ByteBuffer bufferVU = ByteBuffer.wrap(ret, total, total / 2);

        bufferY.put(data, 0, total);
        for (int i = 0; i < total / 4; i += 1) {
            bufferVU.put(data[i + total + total / 4]);
            bufferVU.put(data[total + i]);
        }

        return ret;
    }



     public static class NV21ToBitmap {
         private RenderScript rs;
         private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
         private Type.Builder yuvType, rgbaType;
         private Allocation in, out;

         private int mWidth, mHeight = 0;
         private Bitmap mCurBitmap = null;

         public NV21ToBitmap(Context context) {
             rs = RenderScript.create(context);
             yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
         }

         public Bitmap getCurBitmap() {
             return mCurBitmap;
         }

         public Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
             if (yuvType == null) {
                 yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
                 in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                 rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
                 out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
             }
             in.copyFrom(nv21);
             yuvToRgbIntrinsic.setInput(in);
             yuvToRgbIntrinsic.forEach(out);

             if (mWidth != width || mHeight != height) {
                 if (mCurBitmap != null) {
                     mCurBitmap.recycle();
                     mCurBitmap = null;
                 }
                 mCurBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                 mWidth = width;
                 mHeight = height;
             }

             out.copyTo(mCurBitmap);
             return mCurBitmap;
         }
     }



}
