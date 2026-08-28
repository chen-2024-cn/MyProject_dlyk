package com.cyk.web;

import com.cyk.model.TUser;
import com.cyk.query.UserQuery;
import com.cyk.result.R;
import com.cyk.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Delete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Resource
    private UserService userService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private com.cyk.service.RedisService redisService;


    /**
     * 获取登录人信息
     *
     * 【核心修复】不再直接回吐登录时打入 JWT 的旧快照（authentication.getPrincipal()），
     * 而是实时查库拿最新资料 + 角色/权限，保证用户编辑后立即可见。
     * 同时最近登录时间改为单列精准更新，绝不再用旧的 JWT 快照整表回写（会覆盖刚编辑的数据）。
     *
     * @param authentication
     * @return
     */
    @GetMapping("/api/login/info")
    public R LoginInfo(Authentication authentication) {
        TUser jwtUser = ((TUser) authentication.getPrincipal());
        // 仅更新最近登录时间（单列更新，避免整表覆盖）
        userService.updateLastLoginTime(jwtUser);
        // 实时查库返回最新的完整用户信息（含密码脱敏）
        TUser latest = userService.getUserInfo(jwtUser.getId());
        return R.OK(latest);
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
        // 使用占位符输出关键业务参数；绝不打印整个对象（含密码等敏感字段）
        log.info("新增用户请求，登录账号：{}，用户姓名：{}", userQuery.getLoginAct(), userQuery.getName());

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
        log.info("修改用户请求，目标用户ID：{}，登录账号：{}", userQuery.getId(), userQuery.getLoginAct());
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
        log.info("用户[{}]编辑个人资料结果：{}", currentUser.getLoginAct(), i == 1 ? "成功" : "失败");
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PutMapping("/api/profile/password")
    public R changePassword(Authentication authentication, @RequestBody java.util.Map<String, String> params) {
        // 从数据库实时查出当前用户（含最新密码哈希）做校验与更新：
        // 若用 JWT 快照里的旧哈希校验，同一会话内连续修改密码时第二次起会校验失败；
        // 且 getUserInfo 会把 loginPwd置空（展示脱敏用），所以这里必须用带完整字段的实时查询。
        TUser currentUser = userService.getUserById(((TUser) authentication.getPrincipal()).getId());
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (currentUser == null) {
            return R.FAIL("当前用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, currentUser.getLoginPwd())) {
            log.warn("用户[{}]修改密码失败：原密码校验不通过", currentUser.getLoginAct());
            return R.FAIL("原密码错误");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        int i = userService.changePassword(currentUser.getId(), encodedPassword);
        log.info("用户[{}]修改密码：{}", currentUser.getLoginAct(), i == 1 ? "成功" : "失败");
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

