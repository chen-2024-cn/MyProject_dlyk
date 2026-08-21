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

    @PreAuthorize("hasAuthority('user:update')")
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
}
