package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.mapper.*;
import com.cyk.model.*;
import com.cyk.query.TranQuery;
import com.cyk.query.TranRemarkQuery;
import com.cyk.service.TranService;
import com.cyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class TranServiceImpl implements TranService {

    @Resource
    private TTranMapper tTranMapper;

    @Resource
    private TTranHistoryMapper tTranHistoryMapper;

    @Resource
    private TTranRemarkMapper tTranRemarkMapper;

    @Resource
    private TDicValueMapper tDicValueMapper;

    @Resource
    private TUserMapper tUserMapper;

    @Resource
    private TCustomerMapper tCustomerMapper;

    @Override
    public PageInfo<TTran> getTranByPage(Integer current, Integer customerId, BigDecimal money) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        List<TTran> list = tTranMapper.selectTranByPage(customerId, money);
        return new PageInfo<>(list);
    }

    @Override
    public int saveTran(TranQuery tranQuery) {
        TTran tTran = new TTran();
        BeanUtils.copyProperties(tranQuery, tTran);

        // Generate tranNo: TR + yyyyMMdd + 4-digit sequence
        String datePrefix = "TR" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        String maxTranNo = tTranMapper.selectMaxTranNoByDate(datePrefix);
        int seq = 1;
        if (maxTranNo != null && maxTranNo.length() >= 12) {
            seq = Integer.parseInt(maxTranNo.substring(10)) + 1;
        }
        tTran.setTranNo(datePrefix + String.format("%04d", seq));

        Integer userId = JWTUtils.parseUserFromJWT(tranQuery.getToken()).getId();
        tTran.setCreateBy(userId);
        tTran.setCreateTime(new Date());
        tTran.setStage(12); // Default: "01创建交易" dic_value id = 12

        int result = tTranMapper.insertSelective(tTran);

        // Record initial stage history
        TTranHistory history = new TTranHistory();
        history.setTranId(tTran.getId());
        history.setStage(tTran.getStage());
        history.setMoney(tTran.getMoney());
        history.setExpectedDate(tTran.getExpectedDate());
        history.setCreateTime(new Date());
        history.setCreateBy(userId);
        tTranHistoryMapper.insertSelective(history);

        return result;
    }

    @Override
    public TTran getTranById(Integer id) {
        return tTranMapper.selectById(id);
    }

    @Override
    public int updateTran(TranQuery tranQuery) {
        TTran tTran = new TTran();
        BeanUtils.copyProperties(tranQuery, tTran);
        Integer userId = JWTUtils.parseUserFromJWT(tranQuery.getToken()).getId();
        tTran.setEditBy(userId);
        tTran.setEditTime(new Date());
        return tTranMapper.updateByPrimaryKeySelective(tTran);
    }

    @Override
    public int deleteTran(Integer id) {
        tTranRemarkMapper.deleteByTranId(id);
        tTranHistoryMapper.deleteByTranId(id);
        return tTranMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int deleteTranBatch(List<Integer> ids) {
        int count = 0;
        for (Integer id : ids) {
            tTranRemarkMapper.deleteByTranId(id);
            tTranHistoryMapper.deleteByTranId(id);
            count += tTranMapper.deleteByPrimaryKey(id);
        }
        return count;
    }

    @Override
    public void changeStage(Integer tranId, Integer stage, BigDecimal money, Date expectedDate, String token) {
        TTran tTran = tTranMapper.selectByPrimaryKey(tranId);
        if (tTran == null) {
            throw new RuntimeException("交易不存在");
        }

        // Validate stage adjacency
        TDicValue currentStage = tDicValueMapper.selectByPrimaryKey(tTran.getStage());
        TDicValue targetStage = tDicValueMapper.selectByPrimaryKey(stage);
        if (currentStage == null || targetStage == null) {
            throw new RuntimeException("阶段数据异常");
        }
        if (Math.abs(currentStage.getOrder() - targetStage.getOrder()) != 1) {
            throw new RuntimeException("只能变更为相邻阶段");
        }

        Integer userId = JWTUtils.parseUserFromJWT(token).getId();

        // Update transaction
        tTran.setStage(stage);
        if (money != null) {
            tTran.setMoney(money);
        }
        if (expectedDate != null) {
            tTran.setExpectedDate(expectedDate);
        }
        tTran.setEditTime(new Date());
        tTran.setEditBy(userId);
        tTranMapper.updateByPrimaryKeySelective(tTran);

        // Record history
        TTranHistory history = new TTranHistory();
        history.setTranId(tranId);
        history.setStage(stage);
        history.setMoney(tTran.getMoney());
        history.setExpectedDate(tTran.getExpectedDate());
        history.setCreateTime(new Date());
        history.setCreateBy(userId);
        tTranHistoryMapper.insertSelective(history);
    }

    @Override
    public List<TTranHistory> getHistoryByTranId(Integer tranId) {
        return tTranHistoryMapper.selectByTranId(tranId);
    }

    @Override
    public PageInfo<TTran> getTransByCustomerId(Integer current, Integer customerId) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        List<TTran> list = tTranMapper.selectTranByPage(customerId, null);
        return new PageInfo<>(list);
    }

    @Override
    public PageInfo<TTranRemark> getRemarkByPage(Integer current, Integer tranId) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        List<TTranRemark> list = tTranRemarkMapper.selectTranRemarkByPage(tranId);
        return new PageInfo<>(list);
    }

    @Override
    public int addRemark(TranRemarkQuery remarkQuery) {
        TTranRemark remark = new TTranRemark();
        remark.setTranId(remarkQuery.getTranId());
        remark.setNoteContent(remarkQuery.getNoteContent());
        remark.setNoteWay(remarkQuery.getNoteWay());

        String token = remarkQuery.getToken();
        int userId = JWTUtils.parseUserFromJWT(token).getId();
        remark.setCreateBy(userId);
        remark.setCreateTime(new Date());

        TDicValue dicValue = tDicValueMapper.selectByPrimaryKey(remarkQuery.getNoteWay());
        remark.setNoteWayName(dicValue != null ? dicValue.getTypeValue() : null);
        remark.setCreateByName(tUserMapper.selectByPrimaryKey(userId).getName());

        return tTranRemarkMapper.insertSelective(remark);
    }

    @Override
    public int updateRemark(TranRemarkQuery remarkQuery) {
        TTranRemark remark = new TTranRemark();
        remark.setId(remarkQuery.getId());
        remark.setTranId(remarkQuery.getTranId());
        remark.setNoteContent(remarkQuery.getNoteContent());
        remark.setNoteWay(remarkQuery.getNoteWay());

        String token = remarkQuery.getToken();
        int userId = JWTUtils.parseUserFromJWT(token).getId();
        remark.setEditBy(userId);
        remark.setEditTime(new Date());

        remark.setEditByName(tUserMapper.selectByPrimaryKey(userId).getName());
        TDicValue dicValue = tDicValueMapper.selectByPrimaryKey(remarkQuery.getNoteWay());
        remark.setNoteWayName(dicValue != null ? dicValue.getTypeValue() : null);

        return tTranRemarkMapper.updateByPrimaryKeySelective(remark);
    }

    @Override
    public int deleteRemark(Integer id) {
        return tTranRemarkMapper.deleteByPrimaryKey(id);
    }

    @Override
    public List<Map<String, Object>> getCustomerOptions() {
        List<TCustomer> customers = tCustomerMapper.selectCustomerPage();
        List<Map<String, Object>> result = new ArrayList<>();
        for (TCustomer c : customers) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getClueDO() != null ? c.getClueDO().getFullName() : "Customer#" + c.getId());
            result.add(map);
        }
        return result;
    }
}
