package com.dlyk.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示控制器 - 提供简单的打招呼接口
 */
@RestController
public class DemoController {

    @GetMapping("/greet")
    public String greet(@RequestParam(value = "name", defaultValue = "旅客") String name) {
        return "您好，" + name + "！欢迎乘坐 DLYK 旅途管理系统，祝您旅途愉快。";
    }
}
