package com.cyk.web;

import com.cyk.model.TUser;
import com.cyk.query.UserQuery;
import com.cyk.result.R;
import com.cyk.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Delete;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private com.cyk.service.RedisService redisService;


    /**
     * 获取登录人信息
     *
     * @param authentication
     * @return
     */
    @GetMapping("/api/login/info")
    public R LoginInfo(Authentication authentication) {
        TUser tUser = ((TUser) authentication.getPrincipal());
        userService.updateLastLoginTime(tUser);
        return R.OK(tUser);
    }

    /**
     * 1. 外部公开注册接口 (已在 SecurityConfig 排除拦截)
     */
    @PostMapping("/api/register")
    public R register(@RequestBody UserQuery userQuery) {
        if (!StringUtils.hasText(userQuery.getLoginAct())) {
            return R.FAIL("用户名不能为空");
        }
        try {
            int result = userService.register(userQuery);
            return result == 1 ? R.OK("注册成功，快去登录吧！") : R.FAIL("注册失败，请稍后重试");
        } catch (IllegalArgumentException e) {
            return R.FAIL(e.getMessage());
        }
    }

    /**
     * 免登录
     *
     * @return
     */
    @GetMapping("/api/login/free")
    public R freeLogin() {
        return R.OK();
    }

    /**
     * 用户分页查询
     * @param currentPage
     * @return
     */
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/api/users")
    public R userPage(@RequestParam(value = "current", required = false) Integer currentPage) {
        PageInfo<TUser> pageInfo = userService.getUserByPage(currentPage);
        return R.OK(pageInfo);
    }

    @PreAuthorize("hasAuthority('user:view')")
    @GetMapping("api/user/{id}")
    public R userDetail(@PathVariable("id") Integer id) {
        TUser t = userService.getUserById(id);
        return R.OK(t);
    }

    @PreAuthorize("hasAuthority('user:add')")
    @PostMapping("/api/user")
    public R addUser(@ModelAttribute UserQuery userQuery, @RequestHeader("Authorization") String token/*从请求头拿到当前用户的信息*/) {
        System.out.println("添加的用户信息:" + userQuery);

        userQuery.setToken(token);
        int res = userService.saveUser(userQuery);
        return res == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/api/user/{id}")
    public R deleteUser(@PathVariable("id") Integer id) {
        return userService.deleteById(id) >= 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('user:edit')")
    @PutMapping("/api/user")
    public R editUser(@RequestBody UserQuery userQuery, @RequestHeader("Authorization") String token/*从请求头拿到当前用户的信息*/) {
        System.out.println("修改的用户信息:" + userQuery);
        userQuery.setToken(token);
        return userService.updateUser(userQuery) == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/api/user")
    public R deleteArr(@RequestParam("ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        return userService.batchDelUserId(idList) >= idList.size() ? R.OK() : R.FAIL();
    }

    @GetMapping("/api/owner")
    public R activityOwner() {
        List<TUser> list = userService.getOwnerList();
        return R.OK(list);
    }

    /**
     * 角色下拉列表（用户管理页新增/编辑用户时分配角色用）
     */
    @PreAuthorize("hasAnyAuthority('user:add','user:edit')")
    @GetMapping("/api/roles")
    public R roleList() {
        return R.OK(userService.getRoleList());
    }

    @PutMapping("/api/profile")
    public R updateProfile(Authentication authentication, @RequestBody UserQuery query) {
        TUser currentUser = (TUser) authentication.getPrincipal();
        TUser updateUser = new TUser();
        updateUser.setId(currentUser.getId());
        updateUser.setName(query.getName());
        updateUser.setPhone(query.getPhone());
        updateUser.setEmail(query.getEmail());
        int i = userService.updateProfile(updateUser);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PutMapping("/api/profile/password")
    public R changePassword(Authentication authentication, @RequestBody java.util.Map<String, String> params) {
        TUser currentUser = (TUser) authentication.getPrincipal();
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (!passwordEncoder.matches(oldPassword, currentUser.getLoginPwd())) {
            return R.FAIL("原密码错误");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        int i = userService.changePassword(currentUser.getId(), encodedPassword);
        return i == 1 ? R.OK() : R.FAIL();
    }

    /**
     * 3. 忘记密码：获取并发送安全核验验证码 (已在 SecurityConfig 及 Filter 放行)
     * 【重构升级：Controller保持精简，业务归于Service，面向接口开发】
     */
    @GetMapping("/api/password/reset/code")
    public R sendResetCode(@RequestParam("loginAct") String loginAct, @RequestParam("email") String email) {
        if (!StringUtils.hasText(loginAct) || !StringUtils.hasText(email)) {
            return R.FAIL("用户名和安全邮箱参数不可为空哦。");
        }
        // 直接调用 Service 层的高内聚核心实现，控制器只负责流通信及分发
        return userService.generateResetCode(loginAct, email);
    }

    /**
     * 4. 忘记密码：核验验证码并执行重置新暗号 (已在 SecurityConfig 放行)
     * 【重构升级：Controller保持精简，错误计数防爆破与事务安全在 ServiceServiceImpl 实施闭环】
     */
    @PutMapping("/api/password/reset")
    public R resetPassword(@RequestBody java.util.Map<String, String> params) {
        String loginAct = params.get("loginAct");
        String code = params.get("code");
        String newPassword = params.get("newPassword");

        if (!StringUtils.hasText(loginAct) || !StringUtils.hasText(code) || !StringUtils.hasText(newPassword)) {
            return R.FAIL("必填校验项录入残漏，修改拒绝！");
        }

        // 一行代码调用 Service 层并响应，控制器保持极尽纯净干炼
        return userService.executeResetPassword(loginAct, code, newPassword);
    }
}

