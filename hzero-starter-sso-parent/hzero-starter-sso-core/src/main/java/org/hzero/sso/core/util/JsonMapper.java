package org.hzero.sso.core.util;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JsonMapper {

    public Map<String, Object> convertToMap(String json) {
        return JSON.parseObject(json, Map.class);
    }

    public Map<String, Object> convertToMap(Object obj) {
        return convertToMap(convertToJson(obj));
    }

    public String convertToJson(Object obj) {
        return JSON.toJSONString(obj);
    }

    public List<Map<String, Object>> convertToList(Collection<String> jsons) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String json : jsons) {
            if (json != null) {
                list.add(convertToMap(json));
            }
        }
        return list;
    }
}
