package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.manager.RedisManager;
import com.cyk.mapper.TPermissionMapper;
import com.cyk.mapper.TRoleMapper;
import com.cyk.mapper.TUserMapper;
import com.cyk.model.TActivityRemark;
import com.cyk.model.TPermission;
import com.cyk.model.TRole;
import com.cyk.model.TUser;
import com.cyk.query.BaseQuery;
import com.cyk.query.UserQuery;
import com.cyk.result.R;
import com.cyk.service.UserService;
import com.cyk.service.RedisService;
import com.cyk.util.CacheUtils;
import com.cyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.util.BeanUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private TUserMapper tUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private TRoleMapper tRoleMapper;

    @Resource
    private RedisManager redisManager;

    @Resource
    private RedisService redisService;

    @Resource
    private TPermissionMapper tPermissionMapper;
    /**
     * 登录查询
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername( String username) throws UsernameNotFoundException {
        // 先检查 Redis 锁定状态，锁定则抛出 LockedException
        String lockKey = "dlyk:login:lock:" + username;
        if (redisService.hasKey(lockKey)) {
            throw new LockedException("该账号因连续多次密码输入错误已被锁定，请在大约 30 分钟后再试！");
        }

        TUser tUser = tUserMapper.selectByLoginAct(username);
        System.out.println("UserServiceImpl类：" + tUser);
        if (tUser == null) {
            throw new UsernameNotFoundException("登录账号不存在");
        }
        //查询用户角色
        List<TRole> tRoles = tRoleMapper.selectByUserId(tUser.getId());
        //转换为字符串
        List<String> list = new ArrayList<>();
        tRoles.forEach(tRole -> {
            list.add(tRole.getRole());
        });
        tUser.setRoleList(list);//设置用户角色

        //查询用户的菜单权限
        List<TPermission> menuPermissionList = tPermissionMapper.selectMenuPermissionByUserId(tUser.getId());
        tUser.setMenuPermissionList(menuPermissionList);

        //查询用户功能权限
        List<TPermission> buttonPermissionsList = tPermissionMapper.selectButtonPermissionByUserId(tUser.getId());
        List<String> stringList = new ArrayList<>();
        for (TPermission tPermission : buttonPermissionsList) {
            stringList.add(tPermission.getCode());//权限标识符
        }
        tUser.setPermissionList(stringList);
        return tUser;
    }

    @Override
    public PageInfo<TUser> getUserByPage(Integer current) {
        //设置pageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        //查询
        List<TUser> list = tUserMapper.selectUserByPage(BaseQuery.builder().build());
        //封装分页数组到PageInfo
        PageInfo<TUser> info = new PageInfo<>(list);
        return info;
    }

    @Override
    public TUser getUserById(Integer id) {
        TUser tUser = tUserMapper.selectDetailById(id);
        return tUser;
    }

    @Override
    public int saveUser(UserQuery userQuery) {

        TUser tUser = new TUser();
        //将UserQuery数据放进tUser里面
        BeanUtils.copyProperties(userQuery, tUser);

        tUser.setLoginPwd(passwordEncoder.encode(userQuery.getLoginPwd()));//密码加密
        tUser.setCreateTime(new Date());//创建时间
        //创建人（获取当前用户的id，用jwt获取）
        int loginId = JWTUtils.parseUserFromJWT(userQuery.getToken()).getId();
        tUser.setCreateBy(loginId);

        return tUserMapper.insertSelective(tUser);
    }

    @Override
    public int deleteById(Integer id) {
        return tUserMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int updateUser(UserQuery userQuery) {
        TUser tUser = new TUser();
        BeanUtils.copyProperties(userQuery, tUser);

        //查看修改的值里面有没有密码
        if (StringUtils.hasText(userQuery.getLoginPwd())) {
            //有密码
            tUser.setLoginPwd(passwordEncoder.encode(userQuery.getLoginPwd()));//密码加密
        }
        tUser.setEditTime(new Date());//编辑时间
        Integer loginId = JWTUtils.parseUserFromJWT(userQuery.getToken()).getId();
        tUser.setEditBy(loginId);//编辑人
        int i = tUserMapper.updateByPrimaryKeySelective(tUser);
        return i;
    }

    @Override
    public int batchDelUserId(List ids) {
        int i = tUserMapper.deleteByIds(ids);
        return i;
    }

    @Override
    public List<TUser> getOwnerList() {
        //从redis中查找
        //redis没有就从数据库查找放到redis里面
        return CacheUtils.getCacheData(() -> {
            //生产，从redis查询
            return (List<TUser>)redisManager.getListValue(Constants.REDIS_OWNER_KEY, TUser.class);
        }, () -> {
            //生产，从数据库查询
            return (List<TUser>)tUserMapper.selectByOwner();
        }, (t) -> {
            //消费，把数据放入redis
            redisManager.setValue(Constants.REDIS_OWNER_KEY, t);
        });
    }

    @Override
    public void updateLastLoginTime(TUser tUser) {
        tUser.setLastLoginTime(new Date());
        tUserMapper.updateByPrimaryKeySelective(tUser);
    }

    @Override
    public int updateProfile(TUser user) {
        user.setEditTime(new Date());
        return tUserMapper.updateByPrimaryKeySelective(user);
    }

    @Override
    public int changePassword(Integer userId, String encodedPassword) {
        TUser user = new TUser();
        user.setId(userId);
        user.setLoginPwd(encodedPassword);
        return tUserMapper.updateByPrimaryKeySelective(user);
    }
}
