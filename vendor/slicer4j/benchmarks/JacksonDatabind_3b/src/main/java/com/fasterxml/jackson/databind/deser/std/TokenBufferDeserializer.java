package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * We also want to directly support deserialization of {@link TokenBuffer}.
 *<p>
 * Note that we use scalar deserializer base just because we claim
 * to be of scalar for type information inclusion purposes; actual
 * underlying content can be of any (Object, Array, scalar) type.
 *<p>
 * Since 2.3, another important thing is that possible native ids
 * (type id, object id) should be properly copied even when converting
 * with {@link TokenBuffer}. Such ids are supported if (and only if!)
 * source {@link JsonParser} supports them.
 */
@JacksonStdImpl
public class TokenBufferDeserializer extends StdScalarDeserializer<TokenBuffer> {
    private static final long serialVersionUID = 1L;
    
    public TokenBufferDeserializer() { super(TokenBuffer.class); }

    @Override
    public TokenBuffer deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return createBufferInstance(jp).deserialize(jp, ctxt);
    }

    protected TokenBuffer createBufferInstance(JsonParser jp) {
        return new TokenBuffer(jp);
    }
}