package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.data.property.*;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VertexBufferUtils {
    static final Map<String, VertexBuffer.Type> EXPORT_ATTRIBUTE_MAP;
    static {
        Map<String, VertexBuffer.Type> map = new HashMap<>();

        map.put(BMeshAttribute.Position,        VertexBuffer.Type.Position);
        map.put(BMeshAttribute.Normal,          VertexBuffer.Type.Normal);
        map.put(BMeshAttribute.Tangent,         VertexBuffer.Type.Tangent);
        map.put(BMeshAttribute.Binormal,        VertexBuffer.Type.Binormal);
        map.put(BMeshAttribute.Color,           VertexBuffer.Type.Color);
        map.put(BMeshAttribute.Size,            VertexBuffer.Type.Size);
        map.put(BMeshAttribute.Index,           VertexBuffer.Type.Index);
        map.put(BMeshAttribute.InstanceData,    VertexBuffer.Type.InstanceData);

        map.put(BMeshAttribute.TexCoord,        VertexBuffer.Type.TexCoord);
        map.put(BMeshAttribute.TexCoord2,       VertexBuffer.Type.TexCoord2);
        map.put(BMeshAttribute.TexCoord3,       VertexBuffer.Type.TexCoord3);
        map.put(BMeshAttribute.TexCoord4,       VertexBuffer.Type.TexCoord4);
        map.put(BMeshAttribute.TexCoord5,       VertexBuffer.Type.TexCoord5);
        map.put(BMeshAttribute.TexCoord6,       VertexBuffer.Type.TexCoord6);
        map.put(BMeshAttribute.TexCoord7,       VertexBuffer.Type.TexCoord7);
        map.put(BMeshAttribute.TexCoord8,       VertexBuffer.Type.TexCoord8);

        map.put(BMeshAttribute.BindPosePosition,VertexBuffer.Type.BindPosePosition);
        map.put(BMeshAttribute.BindPoseNormal,  VertexBuffer.Type.BindPoseNormal);
        map.put(BMeshAttribute.BoneWeight,      VertexBuffer.Type.BoneWeight);
        map.put(BMeshAttribute.BoneIndex,       VertexBuffer.Type.BoneIndex);
        map.put(BMeshAttribute.BindPoseTangent, VertexBuffer.Type.BindPoseTangent);
        map.put(BMeshAttribute.HWBoneWeight,    VertexBuffer.Type.HWBoneWeight);
        map.put(BMeshAttribute.HWBoneIndex,     VertexBuffer.Type.HWBoneIndex);

        map.put(BMeshAttribute.MorphTarget0,    VertexBuffer.Type.MorphTarget0);
        map.put(BMeshAttribute.MorphTarget1,    VertexBuffer.Type.MorphTarget1);
        map.put(BMeshAttribute.MorphTarget2,    VertexBuffer.Type.MorphTarget2);
        map.put(BMeshAttribute.MorphTarget3,    VertexBuffer.Type.MorphTarget3);
        map.put(BMeshAttribute.MorphTarget4,    VertexBuffer.Type.MorphTarget4);
        map.put(BMeshAttribute.MorphTarget5,    VertexBuffer.Type.MorphTarget5);
        map.put(BMeshAttribute.MorphTarget6,    VertexBuffer.Type.MorphTarget6);
        map.put(BMeshAttribute.MorphTarget7,    VertexBuffer.Type.MorphTarget7);
        map.put(BMeshAttribute.MorphTarget8,    VertexBuffer.Type.MorphTarget8);
        map.put(BMeshAttribute.MorphTarget9,    VertexBuffer.Type.MorphTarget9);
        map.put(BMeshAttribute.MorphTarget10,   VertexBuffer.Type.MorphTarget10);
        map.put(BMeshAttribute.MorphTarget11,   VertexBuffer.Type.MorphTarget11);
        map.put(BMeshAttribute.MorphTarget12,   VertexBuffer.Type.MorphTarget12);
        map.put(BMeshAttribute.MorphTarget13,   VertexBuffer.Type.MorphTarget13);

        EXPORT_ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    public static VertexBuffer.Type getVertexBufferType(String bmeshAttributeName) {
        VertexBuffer.Type type = EXPORT_ATTRIBUTE_MAP.get(bmeshAttributeName);
        if(type == null)
            throw new IllegalArgumentException("Unrecognized BMeshAttribute name: " + bmeshAttributeName);
        return type;
    }


    static final Map<VertexBuffer.Type, String> IMPORT_ATTRIBUTE_MAP;
    static {
        // Invert mapping
        Map<VertexBuffer.Type, String> map = new HashMap<>();
        for(Map.Entry<String, VertexBuffer.Type> entry : EXPORT_ATTRIBUTE_MAP.entrySet())
            map.put(entry.getValue(), entry.getKey());
        IMPORT_ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    public static String getBMeshAttributeName(VertexBuffer.Type vertexBufferType) {
        String attrName = IMPORT_ATTRIBUTE_MAP.get(vertexBufferType);
        if(attrName == null)
            throw new IllegalArgumentException("Unrecognized VertexBuffer type: " + vertexBufferType.name());
        return attrName;
    }



    public static <E extends Element> BMeshAttribute<E, ?> createBMeshAttribute(VertexBuffer buffer, Class<E> elementType) {
        return createBMeshAttribute(buffer.getBufferType(), buffer.getNumComponents(), elementType);
    }

    public static <E extends Element> BMeshAttribute<E, ?> createBMeshAttribute(VertexBuffer.Type bufferType, int components, Class<E> elementType) {
        String name = getBMeshAttributeName(bufferType);

        switch(bufferType) {
            case Position:
            case Normal:
            case Tangent:
            case Binormal:
            case BindPosePosition:
            case BindPoseNormal:
            case BoneWeight:
            case BindPoseTangent:
            case HWBoneWeight:
            case InstanceData:
            case TexCoord:
            case TexCoord2:
            case TexCoord3:
            case TexCoord4:
            case TexCoord5:
            case TexCoord6:
            case TexCoord7:
            case TexCoord8:
            case MorphTarget0:
            case MorphTarget1:
            case MorphTarget2:
            case MorphTarget3:
            case MorphTarget4:
            case MorphTarget5:
            case MorphTarget6:
            case MorphTarget7:
            case MorphTarget8:
            case MorphTarget9:
            case MorphTarget10:
            case MorphTarget11:
            case MorphTarget12:
            case MorphTarget13:
                return createFloatTupleAttribute(name, components);

            case Color:
                if(components == 4)
                    return new ColorAttribute<>(name);
                else
                    return createFloatTupleAttribute(name, components);

            case Size:
                if(components == 1)
                    return new FloatAttribute<>(name);
                else
                    createFloatTupleAttribute(name, components);

            case BoneIndex:
            case HWBoneIndex:
                if(components != 1)
                    break;
                return new ByteAttribute<>(name);

            case Index:
                return null;
        }

        //throw new UnsupportedOperationException("VertexBuffer type '" + bufferType.name() + "' not supported.");
        return null;
    }

    private static <E extends Element> FloatTupleAttribute<E> createFloatTupleAttribute(String name, int components) {
        // TODO: How are algorithms supposed to deal with attributes of different size?
        //       Wrapper attributes that e.g. return z=0 if it only has 2 components?
        //       Expect users to convert attributes?
        if(components == 2)
            return new Vec2Attribute<E>(name);
        else if(components == 3)
            return new Vec3Attribute<E>(name);
        else
            return new FloatTupleAttribute<E>(name, components);
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


    public static DoubleBuffer createDoubleBuffer(int size) {
        return BufferUtils.createByteBuffer(size * 8).asDoubleBuffer();
    }

    public static DoubleBuffer createDoubleBuffer(double... data) {
        DoubleBuffer buff = BufferUtils.createByteBuffer(data.length * 8).asDoubleBuffer();
        buff.put(data);
        buff.flip();
        return buff;
    }

    public static LongBuffer createLongBuffer(int size) {
        return BufferUtils.createByteBuffer(size * 8).asLongBuffer();
    }

    public static LongBuffer createLongBuffer(long... data) {
        LongBuffer buff = BufferUtils.createByteBuffer(data.length * 8).asLongBuffer();
        buff.put(data);
        buff.flip();
        return buff;
    }
}
