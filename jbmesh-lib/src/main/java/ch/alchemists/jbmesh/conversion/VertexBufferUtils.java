package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.data.property.ColorProperty;
import ch.alchemists.jbmesh.data.property.FloatTupleProperty;
import ch.alchemists.jbmesh.data.property.Vec2Property;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.*;

public class VertexBufferUtils {
    public static <E extends Element> BMeshProperty<E, ?> createProperty(VertexBuffer buffer, Class<E> elementType) {
        final int components = buffer.getNumComponents();

        switch(buffer.getBufferType()) {
            case Position:
                assert components == 3;
                return new Vec3Property<E>(Vertex.Position);

            case Normal:
                assert components == 3;
                return new Vec3Property<E>(Vertex.Normal);

            case TexCoord: {
                // TODO: How are algorithms supposed to deal with properties of different size?
                //       Wrapper properties that e.g. return z=0 if it only has 2 components?
                if(components == 2)
                    return new Vec2Property<E>(Vertex.TexCoord);
                else if(components == 3)
                    return new Vec3Property<E>(Vertex.TexCoord);
                else
                    return new FloatTupleProperty<E>(Vertex.TexCoord, components);
            }

            case Color:
                assert components == 4;
                return new ColorProperty<>(Vertex.Color);
        }

        throw new UnsupportedOperationException("VertexBuffer type '" + buffer.getBufferType().name() + "' not supported.");
    }

    public static BMeshProperty<Vertex, ?> createVertexProperty(BMeshData<Vertex> bmeshData, VertexBuffer buffer) {
        BMeshProperty<Vertex, ?> property = createProperty(buffer, Vertex.class);
        setData(bmeshData, buffer, property);
        return property;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Element, TArray> void setData(BMeshData<E> bmeshData, VertexBuffer buffer, BMeshProperty<E, TArray> property) {
        Object array = VertexBufferUtils.getArray(buffer);

        //System.out.println("Adding property '" + property.name + "' with " + Array.getLength(array) + " elements, components = " + property.numComponents);
        bmeshData.addProperty(property, (TArray) array);
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
