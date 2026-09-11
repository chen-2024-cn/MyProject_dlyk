package com.cyk.mapper;

import com.cyk.model.TActivity;
import com.cyk.model.TClue;
import com.cyk.query.ClueQuery;
import com.cyk.result.NameValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TClueMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TClue record);

    int insertSelective(TClue record);

    TClue selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TClue record);

    int updateByPrimaryKey(TClue record);

    /**
     * 线索分页查询（支持多条件动态筛选）。
     *
     * <p>【签名变更】旧版仅接收 {@code Integer current} 页码，且页码在 Service 层
     * 已由 PageHelper 接管、SQL 内部并不使用它（属无效参数）。现改为接收 {@link com.cyk.query.ClueQuery}
     * 动态条件，使线索模块具备与活动/产品模块一致的检索能力。</p>
     *
     * <p>必须用 {@code @Param("query")} 显式命名：这样 XML 中的 OGNL 表达式可以写成
     * {@code query != null and query.xxx}，当调用方传 {@code null}（内部复用场景）时
     * 也不会因解析空根对象而抛异常。</p>
     *
     * @param query 查询条件，可为 null（等价于无筛选的全量分页）
     */
    List<TClue> selectClueByPage(@Param("query") ClueQuery query);

    void saveClue(List<TClue> cachedDataList);

    boolean existsUserById(Integer ownerId);

    int selectByPhone(String phone);

    TClue selectById(Integer id);

    int selectClueByCount();

    List<NameValue> selectBySource();

    List<NameValue> selectClueByMonth();
}