package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.data.property.ColorAttribute;
import ch.alchemists.jbmesh.data.property.FloatTupleAttribute;
import ch.alchemists.jbmesh.data.property.Vec2Attribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VertexBufferUtils {
    private static final Map<String, VertexBuffer.Type> EXPORT_ATTRIBUTE_MAP;
    static {
        Map<String, VertexBuffer.Type> map = new HashMap<>();

        map.put(BMeshAttribute.Position,     VertexBuffer.Type.Position);
        map.put(BMeshAttribute.Normal,       VertexBuffer.Type.Normal);
        map.put(BMeshAttribute.TexCoord,     VertexBuffer.Type.TexCoord);
        map.put(BMeshAttribute.Color,        VertexBuffer.Type.Color);
        map.put(BMeshAttribute.Index,        VertexBuffer.Type.Index);

        EXPORT_ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    public static VertexBuffer.Type getVertexBufferType(String bmeshAttributeName) {
        return EXPORT_ATTRIBUTE_MAP.get(bmeshAttributeName);
    }


    private static final Map<VertexBuffer.Type, String> IMPORT_ATTRIBUTE_MAP;
    static {
        Map<VertexBuffer.Type, String> map = new HashMap<>();
        for(Map.Entry<String, VertexBuffer.Type> entry : EXPORT_ATTRIBUTE_MAP.entrySet())
            map.put(entry.getValue(), entry.getKey());
        IMPORT_ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    public static String getBMeshAttributeName(VertexBuffer.Type vertexBufferType) {
        return IMPORT_ATTRIBUTE_MAP.get(vertexBufferType);
    }



    public static <E extends Element> BMeshAttribute<E, ?> createBMeshAttribute(VertexBuffer buffer, Class<E> elementType) {
        final int components = buffer.getNumComponents();

        switch(buffer.getBufferType()) {
            case Position:
                assert components == 3;
                return new Vec3Attribute<E>(BMeshAttribute.Position);

            case Normal:
                assert components == 3;
                return new Vec3Attribute<E>(BMeshAttribute.Normal);

            case TexCoord: {
                // TODO: How are algorithms supposed to deal with attributes of different size?
                //       Wrapper attributes that e.g. return z=0 if it only has 2 components?
                //       Expect users to convert attributes?
                if(components == 2)
                    return new Vec2Attribute<E>(BMeshAttribute.TexCoord);
                else if(components == 3)
                    return new Vec3Attribute<E>(BMeshAttribute.TexCoord);
                else
                    return new FloatTupleAttribute<E>(BMeshAttribute.TexCoord, components);
            }

            case Color:
                assert components == 4;
                return new ColorAttribute<>(BMeshAttribute.Color);
        }

        throw new UnsupportedOperationException("VertexBuffer type '" + buffer.getBufferType().name() + "' not supported.");
    }

    @SuppressWarnings("unchecked")
    public static <E extends Element, TArray> void setData(BMeshData<E> bmeshData, VertexBuffer buffer, BMeshAttribute<E, TArray> attribute) {
        Object array = VertexBufferUtils.getArray(buffer);

        //System.out.println("Adding attribute '" + attribute.name + "' with " + Array.getLength(array) + " elements, components = " + attribute.numComponents);
        bmeshData.addAttribute(attribute, (TArray) array);
    }


    public static Object getArray(VertexBuffer buffer) {
        switch(buffer.getFormat()) {
            case Float:
                return getFloatArray((FloatBuffer) buffer.getData());

            case Double:
                return getFloatArray((DoubleBuffer) buffer.getData());

            case Short:
            case UnsignedShort:
                return getShortArray((ShortBuffer) buffer.getData());

            case Int:
            case UnsignedInt:
                return getIntArray((IntBuffer) buffer.getData());

            case Half: // TODO: These are actually shorts. And actually floats. Convert with FastMath.convertHalfToFloat(short)
            case Byte:
            case UnsignedByte:
                return getByteArray((ByteBuffer) buffer.getData());
        }

        throw new UnsupportedOperationException("VertexBuffer format '" + buffer.getFormat().name() + "' not supported.");
    }


    public static int[] getIntArray(IntBuffer buffer) {
        buffer.clear();
        int[] array = new int[buffer.limit()];
        buffer.get(array);
        return array;
    }

    public static int[] getIntArray(ShortBuffer buffer) {
        buffer.clear();
        int[] array = new int[buffer.limit()];
        for(int i=0; i<array.length; ++i)
            array[i] = buffer.get();
        return array;
    }


    public static short[] getShortArray(ShortBuffer buffer) {
        buffer.clear();
        short[] array = new short[buffer.limit()];
        buffer.get(array);
        return array;
    }

    public static short[] getShortArray(IntBuffer buffer) {
        buffer.clear();
        short[] array = new short[buffer.limit()];
        for(int i=0; i<array.length; ++i)
            array[i] = (short) buffer.get();
        return array;
    }


    public static byte[] getByteArray(ByteBuffer buffer) {
        buffer.clear();
        byte[] array = new byte[buffer.limit()];
        buffer.get(array);
        return array;
    }


    public static float[] getFloatArray(FloatBuffer buffer) {
        buffer.clear();
        float[] array = new float[buffer.limit()];
        buffer.get(array);
        return array;
    }

    public static float[] getFloatArray(DoubleBuffer buffer) {
        buffer.clear();
        float[] array = new float[buffer.limit()];
        for(int i=0; i<array.length; ++i)
            array[i] = (float) buffer.get();
        return array;
    }


    public static double[] getDoubleArray(DoubleBuffer buffer) {
        buffer.clear();
        double[] array = new double[buffer.limit()];
        buffer.get(array);
        return array;
    }

    public static double[] getDoubleArray(FloatBuffer buffer) {
        buffer.clear();
        double[] array = new double[buffer.limit()];
        for(int i=0; i<array.length; ++i)
            array[i] = buffer.get();
        return array;
    }


    public static DoubleBuffer createDoubleBuffer(double... data) {
        DoubleBuffer buff = BufferUtils.createByteBuffer(data.length * 8).asDoubleBuffer();
        buff.put(data);
        buff.flip();
        return buff;
    }

    public static LongBuffer createLongBuffer(long... data) {
        LongBuffer buff = BufferUtils.createByteBuffer(data.length * 8).asLongBuffer();
        buff.put(data);
        buff.flip();
        return buff;
    }
}
