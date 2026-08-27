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

    @GetMapping("/api/system/info/public")
    public R getPublicSystemInfo() {
        TSystemInfo info = systemInfoService.getSystemInfo();
        TSystemInfo publicInfo = new TSystemInfo();
        if (info != null) {
            publicInfo.setSystemCode(info.getSystemCode()); // 100% 补全携带被管理员写入的主题编码，攻克公共端拉取主题丢失的重度隐患
            publicInfo.setName(info.getName());
            publicInfo.setVersion(info.getVersion());
            publicInfo.setTitle(info.getTitle());
            publicInfo.setDescription(info.getDescription());
            publicInfo.setLogo(info.getLogo());
            publicInfo.setShortcuticon(info.getShortcuticon());
            publicInfo.setIsopen(info.getIsopen());
            publicInfo.setCloseMsg(info.getCloseMsg());
        } else {
            // 提供默认保底兜底防空指针
            publicInfo.setName("智路 CRM");
            publicInfo.setVersion("v0.1.2");
            publicInfo.setDescription("设计精美，操作便捷");
            publicInfo.setIsopen("true");
        }
        return R.OK(publicInfo);
    }

    @PreAuthorize("hasAuthority('system:edit')")
    @PutMapping("/api/system/info")
    public R updateSystemInfo(@RequestBody TSystemInfo systemInfo) {
        int i = systemInfoService.updateSystemInfo(systemInfo);
        return i == 1 ? R.OK() : R.FAIL();
    }
}
