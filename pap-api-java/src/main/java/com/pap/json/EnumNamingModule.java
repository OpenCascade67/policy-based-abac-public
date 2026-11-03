package com.pap.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.function.Function;

/**
 * Writes enums as PascalCase (API) or camelCase (data files) and reads them leniently:
 * case-insensitive names or ordinals, as the .NET implementation accepts.
 */
public class EnumNamingModule extends SimpleModule {

    public enum Style { PASCAL, CAMEL }

    public EnumNamingModule(Style style) {
        super("EnumNamingModule-" + style);
        Function<Enum<?>, String> naming = style == Style.PASCAL ? EnumNames::pascal : EnumNames::camel;

        setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType,
                                                          BeanDescription beanDesc, JsonSerializer<?> serializer) {
                return new NamedEnumSerializer(naming);
            }
        });
        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
                                                              BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                return new LenientEnumDeserializer(type.getRawClass());
            }
        });
    }

    private static final class NamedEnumSerializer extends StdSerializer<Enum<?>> {
        private final transient Function<Enum<?>, String> naming;

        @SuppressWarnings({"unchecked", "rawtypes"})
        NamedEnumSerializer(Function<Enum<?>, String> naming) {
            super((Class) Enum.class);
            this.naming = naming;
        }

        @Override
        public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(naming.apply(value));
        }
    }

    private static final class LenientEnumDeserializer extends StdDeserializer<Enum<?>> {

        LenientEnumDeserializer(Class<?> type) {
            super(type);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Class<? extends Enum> type = (Class<? extends Enum>) handledType();
            JsonToken token = p.currentToken();
            if (token == JsonToken.VALUE_STRING || token == JsonToken.VALUE_NUMBER_INT) {
                Enum<?> value = EnumNames.parse(type, p.getText());
                if (value != null) return value;
            }
            return (Enum<?>) ctxt.handleWeirdStringValue(type, p.getText(),
                    "The JSON value could not be converted to %s", type.getSimpleName());
        }
    }
}
