package com.cyk.manager;

import com.cyk.mapper.TClueMapper;
import com.cyk.mapper.TCustomerMapper;
import com.cyk.model.TActivityRemark;
import com.cyk.model.TClue;
import com.cyk.model.TCustomer;
import com.cyk.query.CustomerQuery;
import com.cyk.util.JWTUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
public class CustomerManager {

    @Resource
    private TCustomerMapper tCustomerMapper;

    @Resource
    private TClueMapper tClueMapper;

    @Transactional(rollbackFor = Exception.class)
    public Boolean convertCustomer(CustomerQuery customerQuery) {
        //1、验证该线索是否已经转过客户，转过了就不能再转了
        TClue tClue = tClueMapper.selectByPrimaryKey(customerQuery.getClueId());
        if (tClue.getState() == -1) {
            throw new RuntimeException("该线索已经转过客户，不能再转了.");
        }

        //2、向客户表插入一条数据
        TCustomer tCustomer = new TCustomer();
        //把CustomerQuery对象里面的属性数据复制到TCustomer对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(customerQuery, tCustomer);
        tCustomer.setCreateTime(new Date()); //创建时间
        //登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(customerQuery.getToken()).getId();
        tCustomer.setCreateBy(loginUserId); //创建人
        int insert = tCustomerMapper.insertSelective(tCustomer);

        //3、把线索表的数据状态改为-1（已转客户）
        TClue clue = new TClue();
        clue.setId(customerQuery.getClueId());
        clue.setState(-1);
        int update = tClueMapper.updateByPrimaryKeySelective(clue);

        return insert >= 1 && update >= 1;
    }
}
