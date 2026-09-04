package com.cyk.service.impl;

import com.alibaba.excel.util.BooleanUtils;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.util.BeanUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
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

    @Resource
    private com.cyk.mapper.TUserRoleMapper tUserRoleMapper;
    /**
     * 登录查询
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername( String username) throws UsernameNotFoundException {
        // 核心优化：结合用户深度意见，将登录锁定 Key 提升入 Constants 集中管理，清除最后一处“硬编码魔法值”
        String lockKey = Constants.REDIS_LOGIN_LOCK_KEY + username;
        if (redisService.hasKey(lockKey)) {
            throw new LockedException("该账号因连续多次密码输入错误已被锁定，请在大约 30 分钟后再试！");
        }

        TUser tUser = tUserMapper.selectByLoginAct(username);
        // 只记录关键登录账号标识，不输出整个用户对象（避免密码哈希等敏感字段落日志）
        log.debug("登录加载用户信息，账号：{}", username);
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
    public int register(UserQuery userQuery) {
        //判断用户名是否为空
        String name = userQuery.getName();
        if (name == null || name.trim().isEmpty()) {
            return 0; // Name is empty, return 0 to indicate failure
        }

        //判断用户是否存在
        TUser exitUser = tUserMapper.selectByLoginAct(userQuery.getLoginAct());
        if (exitUser != null) {
            throw new IllegalArgumentException("该用户已存在"); // User already exists, return 0 to indicate failure
        }

        TUser tUser = new TUser();
        BeanUtils.copyProperties(userQuery, tUser);

        //密码通过PasswordEncoder加密后存入
        tUser.setLoginPwd(passwordEncoder.encode(userQuery.getLoginPwd()));

        // 4. 初始化用户状态
        tUser.setAccountNoExpired(1);       // 1-账户没过期
        tUser.setCredentialsNoExpired(1);   // 1-密码没过期
        tUser.setAccountNoLocked(1);        // 1-没锁定
        tUser.setAccountEnabled(1);         // 1-启用状态
        tUser.setCreateTime(new Date());

        int rows = tUserMapper.insertSelective(tUser);
        // 注册仅创建账号，不自动绑定任何业务角色（角色由管理员审核后在用户管理页面分配），
        // 避免匿名用户注册即获得线索录入、客户导出等业务权限
        if (rows == 1) {
            evictOwnerCache(); // 用户集合变更 → 失效负责人下拉缓存，避免前端看到旧名单
        }
        return rows;
    }

    @Override
    public R generateResetCode(String loginAct, String email) {
        // 1. 账号与绑定的安全邮箱关联性一致校验
        TUser user = tUserMapper.selectByLoginAct(loginAct);
        boolean matched = (user != null && user.getEmail() != null && user.getEmail().equals(email));
        if (!matched) {
            return R.FAIL("账号不存在或与绑定的安全邮箱不匹配。");
        }

        // 2. 接口单账号防刷流控限制：1分钟内限发一次采用常量提取
        String limitKey = Constants.REDIS_RESET_PWD_LIMIT_KEY + loginAct;
        if (redisService.hasKey(limitKey)) {
            return R.FAIL("发送验证码太频繁了，请稍候 60 秒后再试！");
        }

        // 3. 生成 6 位纯数字安全验证码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        // 4. 将核验验证码存入 Redis 并分别设置 5分钟 与 60秒 生存期限（原子写入，常量统一管理有效期）
        String codeKey = Constants.REDIS_RESET_PWD_CODE_KEY + loginAct;
        redisService.setValue(codeKey, code, Constants.RESET_PWD_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisService.setValue(limitKey, "sent", Constants.RESET_PWD_LIMIT_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 5. 本地联调验证码降级打印（本地无 SMTP 依赖，验证码从后台日志复制；用带占位符的 SLF4J 输出）
        log.info("[CRM 业务防线：重置密码验证码] 账号: {} | 绑定邮箱: {} | 本地虚拟核验码: {} (该验证码在5分钟内有效)",
                loginAct, email, code);

        return R.OK("核验验证码已发出，请注意查看（本地联调可在终端/后台直接复制）");
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    @Override
    public R executeResetPassword(String loginAct, String code, String newPassword) {
        String codeKey = Constants.REDIS_RESET_PWD_CODE_KEY + loginAct;
        String cachedCode = (String) redisService.getValue(codeKey);

        if (cachedCode == null) {
            return R.FAIL("验证码不存在或早已失效，请重新生成并获取");
        }

        // 错误尝试计数器——一击熔断防爆破
        String failKey = Constants.REDIS_RESET_PWD_FAIL_KEY + loginAct;
        Long maxFailCount = Constants.RESET_PWD_MAX_FAIL_COUNT;
        if (!cachedCode.equals(code.trim())) {
            Long currentFailCount = 1L;
            if (redisService.hasKey(failKey)) {
                currentFailCount = Long.parseLong(String.valueOf(redisService.getValue(failKey))) + 1L;
            }
            // 错误计数器与验证码同寿命，原子写入
            redisService.setValue(failKey, String.valueOf(currentFailCount), Constants.RESET_PWD_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

            // 达到上限次数，直接熔断
            if (currentFailCount >= maxFailCount) {
                redisService.delete(codeKey); // 强行物理抹除验证码，彻底废除防爆破
                redisService.delete(failKey);
                return R.FAIL("该验证码连续验证失败次数已达 " + maxFailCount + " 次上限，当前验证码已被废止！请重新获取验证码。");
            }
            return R.FAIL("您输入的验证码有误，还可尝试 " + (maxFailCount - currentFailCount) + " 次！");
        }

        // 验证彻底通过，重写实体密码落盘
        TUser user = tUserMapper.selectByLoginAct(loginAct);
        if (user == null) {
            return R.FAIL("重置动作受重组保护，暂无法找到指定要重载的账号数据对象- admin。");
        }

        // BCrypt 慢哈希写入
        user.setLoginPwd(passwordEncoder.encode(newPassword));
        user.setEditTime(new Date());
        int rows = tUserMapper.updateByPrimaryKeySelective(user);

        if (rows == 1) {
            // 核对一键清扫
            redisService.delete(codeKey);
            redisService.delete(failKey);
            return R.OK("恭喜您已在 Service 事务保护下成功重设密码！");
        }

        return R.FAIL("数据持久化事务回滚，更替失败！");
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
        //回显该用户当前的角色ID列表，供编辑页角色多选框回填
        if (tUser != null) {
            List<Integer> roleIds = tUserRoleMapper.selectRoleIdsByUserId(id);
            tUser.setRoleIds(roleIds);
            if (roleIds == null || roleIds.isEmpty()) {
                tUser.setRoleList(new ArrayList<>());
            } else {
                tUser.setRoleList(tRoleMapper.selectRoleNamesByIds(roleIds));
            }
        }
        return tUser;
    }

    @Transactional(rollbackFor = Exception.class)
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

        int rows = tUserMapper.insertSelective(tUser);
        //新增用户时由管理员显式分配角色（注册接口不自动绑定任何业务角色）
        if (rows == 1 && tUser.getId() != null) {
            assignRoles(tUser.getId(), userQuery.getRoleIds());
            evictOwnerCache(); // 新增用户 → 负责人下拉列表需要立即感知新成员
        }
        return rows;
    }

    /**
     * 分配用户角色：先清空旧绑定，再批量写入新绑定（角色为空时仅清空）
     */
    private void assignRoles(Integer userId, java.util.List<Integer> roleIds) {
        tUserRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            List<com.cyk.model.TUserRole> list = new ArrayList<>();
            for (Integer roleId : roleIds) {
                com.cyk.model.TUserRole ur = new com.cyk.model.TUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            tUserRoleMapper.insertBatch(list);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteById(Integer id) {
        //删除前先清理该用户的角色绑定，避免 t_user_role 外键 ON DELETE RESTRICT 阻止删除
        tUserRoleMapper.deleteByUserId(id);
        //清理被删用户在 Redis 中的登录 token，使其立即失效
        redisService.delete(Constants.REDIS_JWT_KEY + id);
        //清理登录失败计数与账号锁缓存（若有）
        TUser u = tUserMapper.selectByPrimaryKey(id);
        if (u != null && u.getLoginAct() != null) {
            redisService.delete(Constants.REDIS_LOGIN_FAIL_KEY + u.getLoginAct());
            redisService.delete(Constants.REDIS_LOGIN_LOCK_KEY + u.getLoginAct());
        }
        int rows = tUserMapper.deleteByPrimaryKey(id);
        if (rows == 1) {
            evictOwnerCache(); // 用户被删除 → 负责人下拉列表不能再出现此人
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
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
        //前端传了 roleIds 就同步调整角色（未传则不动角色绑定）
        if (i >= 1 && userQuery.getRoleIds() != null) {
            assignRoles(tUser.getId(), userQuery.getRoleIds());
        }
        if (i >= 1) {
            evictOwnerCache(); // 用户信息可能被改名 → 负责人下拉列表需同步刷新
        }
        return i;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchDelUserId(List ids) {
        //批量删除前同样先清理角色绑定，避免外键约束阻止删除
        if (!ids.isEmpty()) {
            tUserRoleMapper.deleteByUserIds(ids);
            //批量清理对应 token，避免被删账号继续有效
            for (Object idObj : ids) {
                redisService.delete(Constants.REDIS_JWT_KEY + idObj);
            }
        }
        int rows = tUserMapper.deleteByIds(ids);
        if (rows > 0) {
            evictOwnerCache(); // 批量删除用户 → 负责人下拉列表同步失效
        }
        return rows;
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
            //消费，把数据放入redis（带 TTL 兜底：即使有人绕过应用直改数据库，缓存也会到期自愈）
            redisManager.setValue(Constants.REDIS_OWNER_KEY, t,
                    Constants.REDIS_OWNER_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
        });
    }

    /**
     * 失效"负责人下拉"缓存：负责人列表是用户表的整体快照，任何用户增/删/改名都无法局部更新，
     * 只能整体删除，后续查询走 Cache-Aside 回源重建（写库成功 → 删缓存）。
     */
    private void evictOwnerCache() {
        redisService.delete(Constants.REDIS_OWNER_KEY);
    }

    @Override
    public List<com.cyk.model.TRole> getRoleList() {
        return tRoleMapper.selectRoleList();
    }

    @Override
    public void updateLastLoginTime(TUser tUser) {
        // 单列精准更新最近登录时间，绝不使用 updateByPrimaryKeySelective 把登录时的 JWT 旧快照整表回写，
        // 否则会覆盖用户在个人中心刚编辑的最新姓名/手机/邮箱（"编辑后不生效"的根因之一）
        tUserMapper.updateLastLoginTimeById(tUser.getId());
    }

    @Override
    public TUser getUserInfo(Integer userId) {
        // 直接查数据库拿"最新"基础资料（关联查出创建人/编辑人姓名，页面"创建专员"回显不丢），
        // 而不是沿用登录时打入 JWT 的旧快照，这样用户在个人中心编辑的姓名/手机/邮箱才能被真实回显
        TUser tUser = tUserMapper.selectDetailById(userId);
        if (tUser == null) {
            return null;
        }
        // 脱敏：密码哈希不允许下发前端
        tUser.setLoginPwd(null);

        // 查询用户角色
        List<TRole> tRoles = tRoleMapper.selectByUserId(tUser.getId());
        List<String> roleNames = new ArrayList<>();
        tRoles.forEach(tRole -> roleNames.add(tRole.getRole()));
        tUser.setRoleList(roleNames);

        // 查询用户的菜单权限
        List<TPermission> menuPermissionList = tPermissionMapper.selectMenuPermissionByUserId(tUser.getId());
        tUser.setMenuPermissionList(menuPermissionList);

        // 查询用户按钮权限
        List<TPermission> buttonPermissionList = tPermissionMapper.selectButtonPermissionByUserId(tUser.getId());
        List<String> permissionCodes = new ArrayList<>();
        for (TPermission tPermission : buttonPermissionList) {
            permissionCodes.add(tPermission.getCode());
        }
        tUser.setPermissionList(permissionCodes);

        return tUser;
    }

    @Override
    public int updateProfile(TUser user) {
        user.setEditTime(new Date());
        int rows = tUserMapper.updateByPrimaryKeySelective(user);
        if (rows == 1) {
            evictOwnerCache(); // 个人中心可能改姓名 → 负责人下拉列表同步刷新（这正是"数据库改了、页面下拉还是旧名字"的典型场景）
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int aiImportUsers(List<com.cyk.result.ai.UserImportRow> rows, Integer operatorId) {
        int imported = 0;
        for (com.cyk.result.ai.UserImportRow row : rows) {
            if (row.getLoginAct() == null || row.getLoginAct().isBlank()) {
                continue; // 必填项缺失的行静默跳过
            }
            String loginAct = row.getLoginAct().trim();
            // 账号已存在则跳过（幂等导入）
            if (tUserMapper.selectByLoginAct(loginAct) != null) {
                continue;
            }
            TUser tUser = new TUser();
            tUser.setLoginAct(loginAct);
            tUser.setLoginPwd(passwordEncoder.encode(
                    (row.getLoginPwd() == null || row.getLoginPwd().isBlank())
                            ? Constants.AI_IMPORT_DEFAULT_PASSWORD
                            : row.getLoginPwd().trim()));
            tUser.setName(row.getName());
            tUser.setPhone(row.getPhone());
            tUser.setEmail(row.getEmail());
            tUser.setAccountNoExpired(1);
            tUser.setCredentialsNoExpired(1);
            tUser.setAccountNoLocked(1);
            tUser.setAccountEnabled(1);
            tUser.setCreateBy(operatorId);
            tUser.setCreateTime(new Date());
            imported += tUserMapper.insertSelective(tUser);
        }
        if (imported > 0) {
            evictOwnerCache(); // AI 批量导入新用户 → 负责人下拉列表需感知
        }
        log.info("AI 批量导入用户完成 | operator={}, imported={}", operatorId, imported);
        return imported;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R aiUpdateUserRolesAndStatus(Integer targetUserId, Integer accountEnabled,
                                        List<Integer> roleIds, Integer operatorId) {
        TUser target = tUserMapper.selectByPrimaryKey(targetUserId);
        if (target == null) {
            return R.FAIL("目标用户不存在，请确认用户ID。");
        }
        TUser update = new TUser();
        update.setId(targetUserId);
        if (accountEnabled != null) {
            update.setAccountEnabled(accountEnabled);
        }
        update.setEditBy(operatorId);
        update.setEditTime(new Date());
        tUserMapper.updateByPrimaryKeySelective(update);

        // 角色变更：复用内部事务逻辑（先删旧绑定再批量写入）
        if (roleIds != null) {
            assignRoles(targetUserId, roleIds);
        }

        // 禁用账号 → 立即清理登录 token，强制下线（安全闭环）
        if (accountEnabled != null && accountEnabled == 0) {
            redisService.delete(Constants.REDIS_JWT_KEY + targetUserId);
        }
        log.info("AI 更新用户权限完成 | operator={}, target={}, enabled={}, roleIds={}",
                operatorId, targetUserId, accountEnabled, roleIds);
        return R.OK();
    }

    @Override
    public int changePassword(Integer userId, String encodedPassword) {
        TUser user = new TUser();
        user.setId(userId);
        user.setLoginPwd(encodedPassword);
        return tUserMapper.updateByPrimaryKeySelective(user);
    }


}
