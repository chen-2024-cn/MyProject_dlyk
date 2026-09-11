package com.cyk.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 客户查询/转化参数对象。
 *
 * <p><b>双重用途（新增字段前务必评估）</b>：本类同时服务于两个场景：</p>
 * <ol>
 *   <li><b>线索转客户</b>：{@code CustomerManager.convertCustomer} 中经
 *       {@code BeanUtils.copyProperties(query, tCustomer)} 拷贝到 {@link com.cyk.model.TCustomer} 落库；</li>
 *   <li><b>客户列表筛选</b>：{@code selectCustomerPage} 的动态 SQL 条件载体。</li>
 * </ol>
 *
 * <p>本次为场景 2 新增的筛选字段（fullName / phone / ownerId / activityId / source / intentionState）
 * 在 {@link com.cyk.model.TCustomer} 中<b>均不存在同名属性</b>（客户表本身不冗余存储这些列，
 * 它们分别落在关联的 t_clue / t_user 上），因此 BeanUtils 会自动跳过，
 * 不会污染转化写入语句——这是复用同一 Query 对象的安全性前提。</p>
 */
@Data
public class CustomerQuery extends BaseQuery {

    private Integer clueId;

    private Integer product;

    private String description;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextContactTime;

    // ==================================================================
    // 以下为「客户列表筛选」专用条件（全部可选，不参与转化写入）
    // ==================================================================

    /** 客户姓名（来自关联线索 t_clue.full_name），模糊匹配 */
    private String fullName;

    /** 手机号（来自关联线索 t_clue.phone），模糊匹配 */
    private String phone;

    /** 负责人 ID（来自关联线索 t_clue.owner_id），等值匹配 */
    private Integer ownerId;

    /** 所属市场活动 ID（来自关联线索 t_clue.activity_id），等值匹配 */
    private Integer activityId;

    /** 线索来源字典 ID（承自线索），等值匹配 */
    private Integer source;

    /** 意向状态字典 ID（承自线索），等值匹配 */
    private Integer intentionState;
}
