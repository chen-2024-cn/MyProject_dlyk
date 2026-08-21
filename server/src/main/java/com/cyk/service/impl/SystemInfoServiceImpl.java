package com.cyk.service.impl;

import com.cyk.mapper.TSystemInfoMapper;
import com.cyk.model.TSystemInfo;
import com.cyk.model.TUser;
import com.cyk.service.SystemInfoService;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SystemInfoServiceImpl implements SystemInfoService {

    @Resource
    private TSystemInfoMapper tSystemInfoMapper;

    @Override
    public TSystemInfo getSystemInfo() {
        return tSystemInfoMapper.selectFirst();
    }

    @Override
    public int updateSystemInfo(TSystemInfo systemInfo) {
        systemInfo.setEditTime(new Date());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TUser) {
            TUser currentUser = (TUser) authentication.getPrincipal();
            systemInfo.setEditBy(currentUser.getId());
        }
        return tSystemInfoMapper.updateByPrimaryKeySelective(systemInfo);
    }
}
