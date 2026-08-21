package com.cyk.web;

import com.cyk.model.TSystemInfo;
import com.cyk.result.R;
import com.cyk.service.SystemInfoService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class SystemInfoController {

    @Resource
    private SystemInfoService systemInfoService;

    @PreAuthorize("hasAuthority('system:view')")
    @GetMapping("/api/system/info")
    public R getSystemInfo() {
        TSystemInfo info = systemInfoService.getSystemInfo();
        return R.OK(info);
    }

    @PreAuthorize("hasAuthority('system:edit')")
    @PutMapping("/api/system/info")
    public R updateSystemInfo(@RequestBody TSystemInfo systemInfo) {
        int i = systemInfoService.updateSystemInfo(systemInfo);
        return i == 1 ? R.OK() : R.FAIL();
    }
}
