package com.cyk.service;

import com.cyk.model.TUser;
import com.cyk.query.UserQuery;
import com.cyk.result.R;
import com.github.pagehelper.PageInfo;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {
    PageInfo<TUser> getUserByPage(Integer current);

    TUser getUserById(Integer id);

    int saveUser(UserQuery userQuery);

    int deleteById(Integer id);

    int updateUser(UserQuery userQuery);

    int batchDelUserId(List ids);

    List<TUser> getOwnerList();

    void updateLastLoginTime(TUser tUser);

    int updateProfile(TUser user);

    int changePassword(Integer userId, String encodedPassword);
}
