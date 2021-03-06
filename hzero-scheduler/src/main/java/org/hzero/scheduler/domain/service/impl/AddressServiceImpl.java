package org.hzero.scheduler.domain.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.scheduler.domain.entity.Executor;
import org.hzero.scheduler.domain.entity.JobInfo;
import org.hzero.scheduler.domain.repository.JobInfoRepository;
import org.hzero.scheduler.domain.repository.JobLogRepository;
import org.hzero.scheduler.domain.service.IAddressService;
import org.hzero.scheduler.infra.redis.JobLock;
import org.hzero.scheduler.infra.util.AddressUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

/**
 * description
 *
 * @author shuangfei.zhu@hand-china.com 2020/03/17 14:13
 */
@Component
public class AddressServiceImpl implements IAddressService {

    private final DiscoveryClient discoveryClient;
    private final JobLogRepository jobLogRepository;
    private final JobInfoRepository jobInfoRepository;

    @Autowired
    public AddressServiceImpl(DiscoveryClient discoveryClient,
                              JobLogRepository jobLogRepository,
                              JobInfoRepository jobInfoRepository) {
        this.discoveryClient = discoveryClient;
        this.jobLogRepository = jobLogRepository;
        this.jobInfoRepository = jobInfoRepository;
    }

    @Override
    public List<String> getAddressList(String serverName) {
        List<ServiceInstance> instanceList = new ArrayList<>();
        if (StringUtils.isNotBlank(serverName)) {
            instanceList = discoveryClient.getInstances(serverName);
        }
        List<String> result = new ArrayList<>();
        instanceList.forEach(item -> result.add(item.getHost() + ":" + item.getPort()));
        return result.stream().sorted(Comparator.comparing(item -> item)).collect(Collectors.toList());
    }

    @Override
    public List<String> getAddressList(Executor executor) {
        List<String> urlList;
        if (Objects.equals(executor.getExecutorType(), BaseConstants.Flag.YES)) {
            // ????????????
            String address = executor.getAddressList();
            urlList = StringUtils.isNotBlank(address) ? AddressUtils.getAddressList(address) : new ArrayList<>();
        } else {
            // ????????????
            urlList = getAddressList(executor.getServerName());
            // ?????????????????????????????????????????????????????????????????????????????????
            if (CollectionUtils.isEmpty(urlList)) {
                List<JobInfo> jobList = jobInfoRepository.select(new JobInfo().setExecutorId(executor.getExecutorId()));
                Date now = new Date();
                for (JobInfo jobInfo : jobList) {
                    // ??????????????????????????????
                    jobLogRepository.updateLogOffline(jobInfo.getJobId(), now);
                    // ???????????????
                    JobLock.clearLock(jobInfo.getJobId());
                }
            }
        }
        return urlList;
    }

    @Override
    public boolean isServerName(String serverName) {
        List<ServiceInstance> instanceList = new ArrayList<>();
        if (StringUtils.isNotBlank(serverName)) {
            instanceList = discoveryClient.getInstances(serverName);
        }
        return CollectionUtils.isNotEmpty(instanceList);
    }
}
