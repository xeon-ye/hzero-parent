package org.hzero.boot.oauth.policy;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.choerodon.core.exception.CommonException;

import org.hzero.boot.oauth.domain.entity.BasePasswordPolicy;
import org.hzero.boot.oauth.domain.entity.BaseUser;
import org.hzero.boot.oauth.domain.repository.BasePasswordPolicyRepository;
import org.hzero.boot.oauth.domain.service.PasswordErrorTimesService;
import org.hzero.boot.oauth.strategy.PasswordStrategy;
import org.hzero.boot.oauth.strategy.PasswordStrategyStore;

@Component
public class PasswordPolicyManager {

    private static final String ERROR_EMPTY = "error.password.null";

    @Value("${hzero.oauth.enable-always-captcha:false}")
    private boolean alwaysEnableCaptcha;

    private final PasswordStrategyStore passwordStrategyStore;
    private final PasswordErrorTimesService passwordErrorTimesService;
    private final BasePasswordPolicyRepository basePasswordPolicyRepository;

    public PasswordPolicyManager(PasswordStrategyStore passwordStrategyStore,
                                 PasswordErrorTimesService passwordErrorTimesService,
                                 BasePasswordPolicyRepository basePasswordPolicyRepository) {
        this.passwordStrategyStore = passwordStrategyStore;
        this.passwordErrorTimesService = passwordErrorTimesService;
        this.basePasswordPolicyRepository = basePasswordPolicyRepository;
    }

    public void passwordValidate(String password, Long tenantId) {
        passwordValidate(password, tenantId, null);
    }

    public void passwordValidate(String password, Long tenantId, BaseUser baseUser) {
        if (password == null) {
            throw new CommonException(ERROR_EMPTY);
        }
        BasePasswordPolicy passwordPolicy = basePasswordPolicyRepository.selectPasswordPolicy(tenantId);
        baseUser = Optional.ofNullable(baseUser).orElse(new BaseUser(null, null, tenantId));
        PasswordPolicyMap passwordPolicyMap = PasswordPolicyMap.parse(passwordPolicy);
        if (passwordPolicyMap.isEnablePassword()) {
            for (PasswordStrategy p : getPasswordProviders(passwordPolicyMap, passwordStrategyStore)) {
                p.validate(passwordPolicyMap, baseUser, password);
            }
        }
    }

    /**
     * ???????????????????????????????????????
     * @param user ????????????
     * @return true - ???
     */
    public boolean isNeedCaptcha(BaseUser user) {
        if (alwaysEnableCaptcha) {
            return true;
        }
        if (user == null) {
            return false;
        }
        if (user.getLocked() != null && user.getLocked()) {
            return false;
        }
        BasePasswordPolicy passwordPolicy = basePasswordPolicyRepository.selectPasswordPolicy(user.getOrganizationId());
        // ?????????????????????????????????????????????????????????????????????
        if (passwordPolicy == null || !passwordPolicy.getEnableSecurity()) {
            return false;
        }

        boolean enableCaptcha = Optional.ofNullable(passwordPolicy.getEnableCaptcha()).orElse(false);
        long maxCaptchaTime = Optional.ofNullable(passwordPolicy.getMaxCheckCaptcha()).orElse(0);
        long loginErrorTime = passwordErrorTimesService.getErrorTimes(user.getId());

        return enableCaptcha && maxCaptchaTime >= 0 && loginErrorTime >= maxCaptchaTime;
    }

    public boolean isNeedCaptcha(Long tenantId) {
        if (alwaysEnableCaptcha) {
            return true;
        }
        BasePasswordPolicy passwordPolicy = basePasswordPolicyRepository.selectPasswordPolicy(tenantId);
        // ?????????????????????????????????????????????????????????????????????
        if (passwordPolicy == null || !passwordPolicy.getEnableSecurity()) {
            return false;
        }

        return Optional.ofNullable(passwordPolicy.getEnableCaptcha()).orElse(false);
    }

    private List<PasswordStrategy> getPasswordProviders(PasswordPolicyMap policy, PasswordStrategyStore store) {
        LinkedList<PasswordStrategy> list = new LinkedList<>();
        for (String id : policy.getPasswordPolicies()) {
            PasswordStrategy provider = store.getProvider(id);
            if (provider != null) {
                list.add(provider);
            }
        }
        return list;
    }
}
