package com.cyk.service.impl;

import com.alibaba.excel.EasyExcel;
import com.cyk.config.UploadDataListener;
import com.cyk.constants.Constants;
import com.cyk.mapper.TClueMapper;
import com.cyk.mapper.TClueRemarkMapper;
import com.cyk.mapper.TCustomerMapper;
import com.cyk.mapper.TDicValueMapper;
import com.cyk.mapper.TUserMapper;
import com.cyk.model.*;
import com.cyk.query.ClueQuery;
import com.cyk.query.ClueRemarkQuery;
import com.cyk.service.ClueService;
import com.cyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

@Service
public class ClueServiceImpl implements ClueService {

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private TClueRemarkMapper tClueRemarkMapper;

    @Resource
    private TUserMapper tUserMapper;

    @Resource
    private TCustomerMapper tCustomerMapper;

    @Resource
    private TDicValueMapper tDicValueMapper;
    @Override
    public PageInfo<TClue> getClueByPage(Integer current) {

        //设置pageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        //查询
        List<TClue> list = tClueMapper.selectClueByPage(current);
        //封装分页数组到PageInfo
        PageInfo<TClue> info = new PageInfo<>(list);
        return info;

    }

    @Override
    public void importExcel(InputStream inputStream, String token) {
        EasyExcel.read(inputStream, TClue.class, new UploadDataListener(tClueMapper, token))
                .sheet()
                .doRead();
    }

    @Override
    public boolean checkPhone(String phone) {
       return tClueMapper.selectByPhone(phone) > 0 ? false : true;

    }

    @Override
    public int saveClue(ClueQuery clueQuery) {
        int i = tClueMapper.selectByPhone(clueQuery.getPhone());
        if (i > 0) {//手机号已存在
            throw new RuntimeException("手机号已存在");
        }

        TClue tClue = new TClue();

        BeanUtils.copyProperties(clueQuery, tClue);
        Integer id = JWTUtils.parseUserFromJWT(clueQuery.getToken()).getId();

        tClue.setCreateBy(id);
        tClue.setCreateTime(new Date());
        return tClueMapper.insertSelective(tClue);
    }

    @Override
    public TClue getClueById(Integer id) {
        TClue tClue = tClueMapper.selectById(id);
        return tClue;
    }

    @Override
    public int updateClue(ClueQuery clueQuery) {
        TClue tClue = new TClue();

        BeanUtils.copyProperties(clueQuery, tClue);
        Integer id = JWTUtils.parseUserFromJWT(clueQuery.getToken()).getId();

        tClue.setEditBy(id);
        tClue.setEditTime(new Date());
        int i = tClueMapper.updateByPrimaryKeySelective(tClue);
        return i;
    }

    @Override
    public PageInfo<TClueRemark> getClueRemarkByPage(Integer current, Integer clueId) {
        //设置pageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        //查询
        List<TClueRemark> list = tClueRemarkMapper.selectClueRemarkByPage(current, clueId);
        //封装分页数组到PageInfo
        PageInfo<TClueRemark> info = new PageInfo<>(list);
        return info;
    }

    @Override
    public int addClueRemark(ClueRemarkQuery clueRemarkQuery) {
        TClueRemark tClueRemark = new TClueRemark();

        tClueRemark.setClueId(clueRemarkQuery.getClueId());
        String token = clueRemarkQuery.getToken();
        int id = JWTUtils.parseUserFromJWT(token).getId();
        tClueRemark.setCreateBy(id);
        tClueRemark.setCreateTime(new Date());
        tClueRemark.setNoteContent(clueRemarkQuery.getNoteContent());
        tClueRemark.setNoteWay(clueRemarkQuery.getNoteWay());

        TDicValue dicValue = tDicValueMapper.selectByPrimaryKey(clueRemarkQuery.getNoteWay());
        tClueRemark.setNoteWayName(dicValue != null ? dicValue.getTypeValue() : null);
        tClueRemark.setCreateByName(tUserMapper.selectByPrimaryKey(id).getName());

        int i = tClueRemarkMapper.insertSelective(tClueRemark);
        return i;
    }

    @Override
    public int updateClueRemark(ClueRemarkQuery clueRemarkQuery) {
        TClueRemark tClueRemark = new TClueRemark();

        tClueRemark.setId(clueRemarkQuery.getId());
        tClueRemark.setClueId(clueRemarkQuery.getClueId());
        String token = clueRemarkQuery.getToken();
        int UserId = JWTUtils.parseUserFromJWT(token).getId();
        tClueRemark.setEditBy(UserId);
        tClueRemark.setNoteContent(clueRemarkQuery.getNoteContent());
        tClueRemark.setNoteWay(clueRemarkQuery.getNoteWay());


        tClueRemark.setEditByName(tUserMapper.selectByPrimaryKey(UserId).getName());
        TDicValue dicValue = tDicValueMapper.selectByPrimaryKey(clueRemarkQuery.getNoteWay());
        tClueRemark.setNoteWayName(dicValue != null ? dicValue.getTypeValue() : null);

        int i = tClueRemarkMapper.updateByPrimaryKeySelective(tClueRemark);
        return i;
    }

    @Override
    public int deleteClueRemark(Integer id) {
        int i = tClueRemarkMapper.deleteByPrimaryKey(id);
        return i;
    }

    @Override
    public int deleteClue(Integer id) {
        tCustomerMapper.updateClueIdToNullByClueId(id);
        int i = tClueMapper.deleteByPrimaryKey(id);
        return i;
    }

    @Override
    public int deleteClueBatch(List<Integer> ids) {
        int count = 0;
        for (Integer id : ids) {
            tCustomerMapper.updateClueIdToNullByClueId(id);  // 解除客户关联
            count += tClueMapper.deleteByPrimaryKey(id);      // 删除线索
        }
        return count;
    }

}
