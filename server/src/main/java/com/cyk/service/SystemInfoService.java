package com.cyk.service;

import com.cyk.model.TSystemInfo;

public interface SystemInfoService {

    TSystemInfo getSystemInfo();

    int updateSystemInfo(TSystemInfo systemInfo);
}
