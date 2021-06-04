package org.hzero.starter.keyencrypt.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-11-29.
 */
public interface IEncryptionService {

    /**
     * 只是为了兼容旧代码，不应该在使用该变量
     */
    String WORKFLOW_TABLE = "8ac987c65ffa4974bfd9460058468706";

    String IGNORE_DECRYPT = "8ac987c65ffa4974bfd9460058468706";

    String NO_TOKEN = "NO_TOKEN";

    /**
     * Decrypt an encrypted JSON object. Contains salt and iv as fields
     *
     * @param value JSON data
     * @return Decrypted byte array
     */
    String decrypt(String value, String tableName);

    String decrypt(String value, String tableName, String accessToken);


    /**
     * Encrypted a string as a byte array and encode using base 64
     *
     * @param tableName tableName
     * @param id        Byte array to be encrypted
     * @return Encrypted data
     */
    String encrypt(String id, String tableName);

    String encrypt(String id, String tableName, String accessToken);

    Object decrypt(final JsonParser parser, final JsonDeserializer<?> deserializer, final DeserializationContext context, JavaType javaType, Encrypt encrypt);

    void setObjectMapper(ObjectMapper objectMapper);

    boolean isCipher(String content);
}
