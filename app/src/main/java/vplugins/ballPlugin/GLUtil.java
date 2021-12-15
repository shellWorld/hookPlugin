package vplugins.ballPlugin;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GLUtil {
    private final static String TAG = "GLUtil";

    private static final int SIZEOF_FLOAT = 4;

    public static int genTexture(int target, int width, int height) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(target, textures[0]);
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(target, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        return textures[0];
    }

    public static int genTextureWithBitmap(Bitmap bitmap) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        return textures[0];
    }


    public static float[] rotateTextureMatrix(float[] var0, float var1) {
        float[] var2 = new float[16];
        Matrix.setRotateM(var2, 0, var1, 0.0F, 0.0F, -1.0F);
        adjustOrigin(var2);
        return multiplyMatrices(var0, var2);
    }

    private static void adjustOrigin(float[] var0) {
        var0[12] -= 0.5F * (var0[0] + var0[4]);
        var0[13] -= 0.5F * (var0[1] + var0[5]);
        var0[12] += 0.5F;
        var0[13] += 0.5F;
    }
    public static float[] multiplyMatrices(float[] var0, float[] var1) {
        float[] var2 = new float[16];
        Matrix.multiplyMM(var2, 0, var0, 0, var1, 0);
        return var2;
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    //TODO. 应该把坐标系的这些常用操作封装起来.
    public static FloatBuffer setFlipX(final FloatBuffer texCoordArray) {
        float[] textureCords = new float[]{
                texCoordArray.get(2), texCoordArray.get(3),
                texCoordArray.get(0), texCoordArray.get(1),
                texCoordArray.get(6), texCoordArray.get(7),
                texCoordArray.get(4), texCoordArray.get(5)
        };
        return createFloatBuffer(textureCords);
    }

    public static FloatBuffer setFlipY(final FloatBuffer texCoordArray) {
        float[] textureCords = new float[]{
                texCoordArray.get(4), texCoordArray.get(5),
                texCoordArray.get(6), texCoordArray.get(7),
                texCoordArray.get(0), texCoordArray.get(1),
                texCoordArray.get(2), texCoordArray.get(3),
        };
        return createFloatBuffer(textureCords);
    }

}
