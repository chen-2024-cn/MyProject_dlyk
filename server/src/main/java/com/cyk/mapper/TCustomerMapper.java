package com.cyk.mapper;

import com.cyk.model.TCustomer;
import com.cyk.query.CustomerQuery;
import com.cyk.result.NameValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TCustomerMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TCustomer record);

    int insertSelective(TCustomer record);

    TCustomer selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TCustomer record);

    int updateByPrimaryKey(TCustomer record);

    /**
     * 客户分页查询（支持多条件动态筛选）。
     * <p>【签名变更】旧版无入参，客户模块无法检索。现接收 {@link CustomerQuery} 动态条件。</p>
     * <p>【调用方兼容】{@code TranServiceImpl.getCustomerOptions()} 需全量客户下拉，传 {@code null} 即可
     * （XML 内所有 {@code <if>} 均以 {@code query != null} 打头，传 null 退化为无筛选查询）。</p>
     *
     * @param query 筛选条件，可为 null
     */
    List<TCustomer> selectCustomerPage(@Param("query") CustomerQuery query);

    int updateClueIdToNullByClueId(Integer clueId);

    List<TCustomer> selectCustomerByExcel(List<String> idList);

    TCustomer selectCustomerById(Integer id);

    int selectByCount();

    List<NameValue> selectCustomerByMonth();
}