package vplugins.ballPlugin;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

import static vplugins.ballPlugin.GLUtil.setFlipY;

/**
 * Created by Administrator on 2017/11/3.
 */

public class OESTO2DTool {
    private int mFrameBuffer = -1;
    private int mTexture2d = -1;
    private ByteBuffer snapByteBuffer= null;
    private GLShaderProgram mShaderProgram;
    private FloatBuffer mMasterVertexBuffer; // 顶点坐标
    private FloatBuffer mGLTextureBuffer;    // 纹理坐标
    private int mSnapIndex = 0;
    private String mSnapShotPath = Environment.getExternalStorageDirectory().getPath() + "/YYImage";
    private String mFileNamePrefix = "snap";
    private int mQuality = 50;
    private static final String TAG = "OSTO2DTool";


    public static final float TEXTURE_ROTATED_90[] = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
    };
    public static final float TEXTURE_ROTATED_180[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
    };
    public static final float TEXTURE_ROTATED_270[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    public final static String vertexShader =
            "attribute vec4 aPosition;          \n" +
            "attribute vec4 aTextureCoord;      \n" +
            "varying vec2 vTexCoord;            \n" +
            "void main()                        \n" +
            "{                                  \n" +
            "    gl_Position = aPosition;       \n" +
            "    vTexCoord = aTextureCoord.xy;  \n" +
            "}";

    public static String fragmentShader =
            "#extension GL_OES_EGL_image_external : require                     \n"+
            "precision mediump float;                                           \n" +
            "varying vec2 vTexCoord;                                            \n" +
            "uniform samplerExternalOES uTexture0;                              \n" +
            "void main()                                                        \n" +
            "{                                                                  \n" +
            "    vec4 color = texture2D(uTexture0, vTexCoord);                  \n" +
            "    gl_FragColor = color; //vec4(color.y, color.y, color.y, 1.0);  \n" +
            "}";

    private final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    public OESTO2DTool() {
        int framebuffer[] = new int[1];
        GLES20.glGenFramebuffers(1, framebuffer, 0);
        mFrameBuffer = framebuffer[0];

        mShaderProgram = new GLShaderProgram();
        mShaderProgram.setProgram(vertexShader, fragmentShader);
        mMasterVertexBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mMasterVertexBuffer.put(CUBE).position(0);
        FloatBuffer GLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.FULL_RECTANGLE_TEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        GLTextureBuffer.put(TEXTURE_ROTATED_90).position(0);

        mGLTextureBuffer = setFlipY(GLTextureBuffer);

        if(null != mSnapShotPath) {
            File file = new File(mSnapShotPath);
            if (!file.exists() || !file.isDirectory()) {
                if (!file.mkdirs()) {
                    Log.e(TAG, "mkdirs " + mSnapShotPath + " failed !");
                }
            }
        }
    }

    public void release() {
        if (mFrameBuffer != -1) {
            int framebuffer[] = new int[1];
            framebuffer[0] = mFrameBuffer;
            GLES20.glDeleteFramebuffers(1, framebuffer, 0);
            mFrameBuffer = -1;
        }

        if (mShaderProgram != null) {
            mShaderProgram.destory();
            mShaderProgram = null;
        }

        if (mMasterVertexBuffer != null) {
            mMasterVertexBuffer.clear();
            mMasterVertexBuffer = null;
        }

        if (mGLTextureBuffer != null) {
            mGLTextureBuffer.clear();
            mGLTextureBuffer = null;
        }

        if (mTexture2d != -1) {
            int texture[] = new int[1];
            texture[0] = mTexture2d;
            GLES20.glDeleteTextures(1, texture,0);
            mTexture2d = -1;
        }

        if (snapByteBuffer != null) {
            snapByteBuffer.clear();
            snapByteBuffer = null;
        }
    }
    public void saveOESTextureToJPEG(int TextureId_OES, int width, int height) {
        // 1. set View Port and clear color


        // 2. generate a 2D texture and bind it to a Framebuffer
        if (mTexture2d == -1) {
            mTexture2d = GLUtil.genTexture(GLES20.GL_TEXTURE_2D, width, height);
        }

        if (snapByteBuffer == null) {
            snapByteBuffer = ByteBuffer.allocate(width * height * 4); // ARGB color format for bitmap
            snapByteBuffer.order(ByteOrder.nativeOrder());
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexture2d, 0);
        GLES20.glViewport(0,0,width,height);
        GLES20.glClearColor(0,0,0,0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        checkGlError("glBindFramebuffer glFramebufferTexture2D");
        // 3. sampling from OES texture and draw it to a 2D texture
        mShaderProgram.useProgram();
        mShaderProgram.setUniformTexture("uTexture0", 0, TextureId_OES, GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mMasterVertexBuffer.position(0);
        mGLTextureBuffer.position(0);
        mShaderProgram.setVertexAttribPointer("aPosition", 2, GLES20.GL_FLOAT, false, 0, mMasterVertexBuffer);
        mShaderProgram.setVertexAttribPointer("aTextureCoord", 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        checkGlError("useProgram");
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
        if (snapByteBuffer != null) {
            snapByteBuffer.clear();
            snapByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, snapByteBuffer);
            checkGlError("glReadPixels");
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(snapByteBuffer);
            saveToFile(bitmap);
        }

        // 4. de init
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        checkGlError("glBindTexture 0");
    }

    private void saveToFile(final Bitmap bmp) {
        Thread taskTreadk = new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream out = null;
                mSnapIndex++;
                if (mSnapIndex % 100 == 1) {
                    String indexStr = String.format("%03d", mSnapIndex);
                    String FilePath = mSnapShotPath + File.separator + mFileNamePrefix + indexStr + ".jpg";
                    try {
                        out = new FileOutputStream(FilePath);
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, String.format(Locale.getDefault(), "%s not found, exception:%s", FilePath, e.toString()));
                    }
                    if (out == null) {
                        return;
                    }

                    bmp.compress(Bitmap.CompressFormat.JPEG, mQuality, out);

                    try {
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "save to file exception:" + e.toString());
                    } finally {
                        bmp.recycle();
                    }
                }
            }
        });

        taskTreadk.start();
    }

    public int ConvertOES(int TextureId_OES, int width, int height)
    {
        // 1. set View Port and clear color
        GLES20.glViewport(0,0,width,height);
        GLES20.glClearColor(0,0,0,0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // 2. generate a 2D texture and bind it to a Framebuffer
        int texture_2d = GLUtil.genTexture(GLES20.GL_TEXTURE_2D, width, height);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture_2d, 0);

        // 3. sampling from OES texture and draw it to a 2D texture
        mShaderProgram.useProgram();
        mShaderProgram.setUniformTexture("uTexture0", 0, TextureId_OES, GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mMasterVertexBuffer.position(0);
        mGLTextureBuffer.position(0);
        mShaderProgram.setVertexAttribPointer("aPosition", 2, GLES20.GL_FLOAT, false, 0, mMasterVertexBuffer);
        mShaderProgram.setVertexAttribPointer("aTextureCoord", 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glFlush();
        // 4. de init
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return texture_2d;
    }

    public int ConvertOES(int TextureId_OES, int width, int height, int texture_2d)
    {
        // 1. set View Port and clear color
        GLES20.glViewport(0,0,width,height);
        GLES20.glClearColor(0,0,0,0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // 2. bind it to a Framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture_2d, 0);

        // 3. sampling from OES texture and draw it to a 2D texture
        mShaderProgram.useProgram();
        mShaderProgram.setUniformTexture("uTexture0", 0, TextureId_OES, GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mMasterVertexBuffer.position(0);
        mGLTextureBuffer.position(0);
        mShaderProgram.setVertexAttribPointer("aPosition", 2, GLES20.GL_FLOAT, false, 0, mMasterVertexBuffer);
        mShaderProgram.setVertexAttribPointer("aTextureCoord", 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glFlush();
        // 4. de init
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return texture_2d;
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }

}
