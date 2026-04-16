package de.labystudio.viaupdater.util.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;

public class RecordTypeAdapter<T> extends TypeAdapter<T> {

    private final Gson gson;
    private final Class<T> recordClass;
    private final RecordTypeAdapterFactory factory;

    RecordTypeAdapter(Gson gson, Class<T> recordClass, RecordTypeAdapterFactory factory) {
        this.gson = gson;
        this.recordClass = recordClass;
        this.factory = factory;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        this.gson.getDelegateAdapter(this.factory, TypeToken.get(this.recordClass)).write(out, value);
    }

    @Override
    public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        RecordComponent[] components = this.recordClass.getRecordComponents();
        Map<String, Object> values = new HashMap<>();

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            RecordComponent component = this.findComponent(components, name);
            if (component == null) {
                in.skipValue();
                continue;
            }
            TypeAdapter<?> adapter = this.gson.getAdapter(TypeToken.get(component.getGenericType()));
            values.put(component.getName(), adapter.read(in));
        }
        in.endObject();

        try {
            Class<?>[] paramTypes = new Class[components.length];
            Object[] args = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                paramTypes[i] = components[i].getType();
                args[i] = values.get(components[i].getName());
            }
            Constructor<T> constructor = this.recordClass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new IOException("Failed to instantiate record " + this.recordClass.getName(), e);
        }
    }

    private RecordComponent findComponent(RecordComponent[] components, String name) {
        for (RecordComponent component : components) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        return null;
    }
}