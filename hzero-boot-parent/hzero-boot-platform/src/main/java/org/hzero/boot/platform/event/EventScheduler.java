package org.hzero.boot.platform.event;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.hzero.boot.platform.event.handler.EventHandlerBean;
import org.hzero.boot.platform.event.handler.EventHandlerHolder;
import org.hzero.boot.platform.event.helper.AsyncScheduleHelper;
import org.hzero.boot.platform.event.helper.RequestHelper;
import org.hzero.boot.platform.event.helper.RuleMatcher;
import org.hzero.boot.platform.event.repository.EventRuleRepository;
import org.hzero.boot.platform.event.vo.ApiParam;
import org.hzero.boot.platform.event.vo.EventParam;
import org.hzero.boot.platform.event.vo.EventRuleVO;
import org.hzero.boot.platform.event.vo.MethodParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 事件调度核心类，事件调度入口。<br/>
 * 被调度的方法自己保证事务一致性，调度器不处理事务。<br/>
 * 根据事件规则顺序，匹配条件的规则会被执行，支持方法调用和API调用，支持同步和异步调用。<br/>
 *
 * @author jiangzhou.bo@hand-china.com 2018/06/13 10:29
 */
public class EventScheduler {

    @Autowired
    private EventRuleRepository eventRuleRepository;
    @Autowired
    private RuleMatcher ruleMatcher;
    @Autowired
    private EventHandlerHolder eventHandlerHolder;
    @Autowired
    private RequestHelper requestHelper;
    @Autowired
    private AsyncScheduleHelper asyncScheduleHelper;

    private LocalVariableTableParameterNameDiscoverer discoverer;

    private static final Logger LOGGER = LoggerFactory.getLogger(EventScheduler.class);

    public EventScheduler() {
        this.discoverer = new LocalVariableTableParameterNameDiscoverer();
    }

    /**
     * 根据事件编码查询事件规则，按规则顺序，根据 condition 判断规则是否通过，通过则调用规则的方法或API。
     *
     * @param eventCode 事件编码
     * @param condition 规则匹配条件
     * @param params 方法调用或API调用的参数，如果调用方法没有参数，则为空。<br/>
     * 
     *        <pre>
     *          如果是方法调用，使用 {@link MethodParam} 封装参数，保持key和参数名称一致即可。<br/>
     *          如果是API调用，使用 {@link ApiParam} 封装参数。<br/>
     *        </pre>
     * 
     * @return 如果某个同步调用需要返回调用结果则返回，异步调用不会返回。如果有多个同步调用需要返回结果，则按顺序返回最后一个调用结果
     * @throws Exception 调度过程或执行方法时发生错误
     */
    @SuppressWarnings("rawtypes")
    @Transactional
    public Object scheduler(String eventCode, Map<String, Object> condition, EventParam... params) throws Exception {
        Assert.notNull(eventCode, "eventCode must not be null.");
        Assert.notEmpty(condition, "condition must not be empty.");
        // 根据事件编码查找事件规则
        List<EventRuleVO> eventRuleList = eventRuleRepository.findByEventCode(eventCode);
        if (CollectionUtils.isEmpty(eventRuleList)) {
            LOGGER.warn(">>>>> 事件编码[{}]没有查询到事件规则", eventCode);
            return null;
        }
        LOGGER.info(">>>>> 事件调度开始. 事件编码[{}]", eventCode);

        // 按序号从小到大排序
        eventRuleList.sort(Comparator.comparingInt(EventRuleVO::getOrderSeq));

        boolean executed = false;
        Object result = null;
        for (EventRuleVO eventRule : eventRuleList) {
            if (!eventRule.enabled()) {
                LOGGER.info(">>>>> 事件规则{}已禁用", eventRule);
                continue;
            }
            if (!eventRule.checkRulePass(ruleMatcher, condition)) {
                LOGGER.info(">>>>> 事件规则{}不匹配条件{}", eventRule, condition);
                continue;
            }

            // 方法调用
            if (Constants.CallType.METHOD.equals(eventRule.getCallType())) {
                String beanName = eventRule.getBeanName();
                String methodName = eventRule.getMethodName();
                EventHandlerBean eventHandler = eventHandlerHolder.getEventHandlerBean(beanName);
                if (eventHandler == null) {
                    LOGGER.error(">>>>> 没有找到事件处理对象[{}]", beanName);
                    throw new NullPointerException("not find the event handler object");
                }
                Method eventHandlerMethod = eventHandlerHolder.getEventHandlerMethod(beanName, methodName);

                if (eventHandlerMethod == null) {
                    LOGGER.error(">>>>> 没有找到事件处理方法[{}]", methodName);
                    throw new NullPointerException("not find the event handler method");
                }
                eventHandlerMethod.setAccessible(true);
                executed = true;
                Object[] methodArgs = getMethodArgs(eventHandlerMethod, params);
                // 同步调用
                if (eventRule.syncCall()) {
                    LOGGER.debug(">>>>> 同步方法调用");
                    Object resultTmp = eventHandlerMethod.invoke(eventHandler, methodArgs);
                    if (eventRule.enableResult()) {
                        result = resultTmp;
                    }
                }
                // 异步调用
                else {
                    LOGGER.debug(">>>>> 异步方法调用");
                    asyncScheduleHelper.asyncMethodSchedule(eventHandler, eventHandlerMethod, methodArgs);
                }
            }
            // API调用
            else if (Constants.CallType.API.equals(eventRule.getCallType())) {
                String apiUrl = eventRule.getApiUrl();
                HttpMethod apiMethod = eventRule.httpMethod();
                executed = true;
                ApiParam apiParam = getApiArgs(params);
                // 同步调用
                if (eventRule.syncCall()) {
                    LOGGER.debug(">>>>> 同步API调用");
                    ResponseEntity responseEntity = requestHelper.request(apiUrl, apiMethod, apiParam);
                    if (eventRule.enableResult()) {
                        result = responseEntity.getBody();
                    }
                }
                // 异步调用
                else {
                    LOGGER.debug(">>>>> 异步API调用");
                    asyncScheduleHelper.asyncApiSchedule(apiUrl, apiMethod, apiParam, requestHelper);
                }
            }
        }

        if (!executed) {
            LOGGER.warn("事件规则匹配都不匹配，没有调度发生");
        }
        LOGGER.info(">>>>> 事件调度结束. 事件编码[{}]", eventCode);
        return result;
    }

    private Object[] getMethodArgs(Method method, EventParam... params) {
        String[] parameterNames = discoverer.getParameterNames(method);
        Object[] args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        if (parameterNames.length > 0) {
            args = new Object[parameterNames.length];
            MethodParam methodParam = null;
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof MethodParam) {
                    methodParam = (MethodParam) params[i];
                    break;
                }
            }
            if (MapUtils.isEmpty(methodParam)) {
                LOGGER.error(">>>>> 调用方法参数[{}]与MethodParam[{}]不匹配", parameterNames, methodParam);
                throw new IllegalArgumentException("调用方法参数与MethodParam不匹配");
            }
            for (int i = 0; i < parameterNames.length; i++) {
                args[i] = methodParam.get(parameterNames[i]);
            }
        }
        return args;
    }

    private ApiParam getApiArgs(EventParam... params) {
        ApiParam apiParam = null;
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof ApiParam) {
                apiParam = (ApiParam) params[i];
                return apiParam;
            }
        }
        return new ApiParam();
    }

}
