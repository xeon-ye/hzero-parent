package org.hzero.starter.keyencrypt.mvc;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.hzero.starter.keyencrypt.core.EncryptContext;
import org.hzero.starter.keyencrypt.core.IEncryptionService;
import org.hzero.starter.keyencrypt.util.EncryptUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xiangyu.qi01@hand-china.com on 2020-02-09.
 */
public class EncryptServletModelAttributeMethodProcessor extends ServletModelAttributeMethodProcessor {

    @Autowired
    IEncryptionService encryptionService;

    public EncryptServletModelAttributeMethodProcessor() {
        super(false);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return EncryptContext.isEncrypt()
                && parameter.hasParameterAnnotation(Encrypt.class)
                && !BeanUtils.isSimpleProperty(parameter.getParameterType());
    }


    /**
     * This implementation downcasts {@link WebDataBinder} to
     * {@link ServletRequestDataBinder} before binding.
     */
    @Override
    protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
        ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
        Assert.state(servletRequest != null, "No ServletRequest");
        ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;

        Field[] fields = FieldUtils.getFieldsWithAnnotation(binder.getTarget().getClass(), Encrypt.class);
        if (ArrayUtils.isNotEmpty(fields)) {
            ParameterRequestWrapper requestWrapper = new ParameterRequestWrapper(servletRequest);
            String attr = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
            Map<String, String> uriVars = (Map<String, String>) servletRequest.getAttribute(attr);
            for (Field field : fields) {
                Encrypt encrypt = field.getAnnotation(Encrypt.class);
                //??????
                String encryptId = requestWrapper.getParameter(field.getName());
                if (StringUtils.isNotEmpty(encryptId)) {
                    if (Collection.class.isAssignableFrom(field.getType()) || field.getType().isArray()) {
                        requestWrapper.addParameter(field.getName(), Arrays.stream(encryptId.split(","))
                                .map(item -> EncryptUtils.ignoreValue(encrypt, item) ? item : encryptionService.decrypt(item, encrypt.value()))
                                .collect(Collectors.joining(",")));
                    } else {
                        encryptId = EncryptUtils.ignoreValue(encrypt, encryptId) ? encryptId : encryptionService.decrypt(encryptId, encrypt.value());
                        requestWrapper.addParameter(field.getName(), encryptId);
                    }
                } else if (uriVars != null && uriVars.containsKey(field.getName())) {
                    encryptId = uriVars.get(field.getName());
                    if (encryptId != null && (Collection.class.isAssignableFrom(field.getType()) || field.getType().isArray())) {
                        uriVars.put(field.getName(), Arrays.stream(encryptId.split(","))
                                .map(item -> EncryptUtils.ignoreValue(encrypt, item) ? item : encryptionService.decrypt(item, encrypt.value()))
                                .collect(Collectors.joining(",")));
                    } else {
                        encryptId = EncryptUtils.ignoreValue(encrypt, encryptId) ? encryptId : encryptionService.decrypt(encryptId, encrypt.value());
                        uriVars.put(field.getName(), encryptId);
                    }
                }
            }
            servletBinder.bind(requestWrapper);
        } else {
            servletBinder.bind(servletRequest);
        }
    }

    public class ParameterRequestWrapper extends ServletRequestWrapper {

        private Map<String, String[]> params = new HashMap<>();

        @SuppressWarnings("unchecked")
        public ParameterRequestWrapper(ServletRequest request) {
            // ???request???????????????????????????????????????????????????????????????????????????????????????????????????????????????new???????????????
            super(request);
            //?????????????????????????????????Map???????????????request????????????
            this.params.putAll(request.getParameterMap());
        }

        public ParameterRequestWrapper(ServletRequest request, Map<String, Object> extendParams) {
            this(request);
            addAllParameters(extendParams);//????????????????????????????????????
        }

        @Override
        public String getParameter(String name) {//??????getParameter?????????????????????????????????map??????
            String[] values = params.get(name);
            if (values == null || values.length == 0) {
                return null;
            }
            return values[0];
        }

        @Override
        public String[] getParameterValues(String name) {//??????
            return params.get(name);
        }

        public void addAllParameters(Map<String, Object> otherParams) {//??????????????????
            for (Map.Entry<String, Object> entry : otherParams.entrySet()) {
                addParameter(entry.getKey(), entry.getValue());
            }
        }


        public void addParameter(String name, Object value) {//????????????
            if (value != null) {
                if (value instanceof String[]) {
                    params.put(name, (String[]) value);
                } else if (value instanceof String) {
                    params.put(name, new String[]{(String) value});
                } else {
                    params.put(name, new String[]{String.valueOf(value)});
                }
            }
        }
    }


}
