package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.exception.BusinessException;
import com.cyk.mapper.*;
import com.cyk.model.*;
import com.cyk.query.TranQuery;
import com.cyk.query.TranRemarkQuery;
import com.cyk.service.RedisService;
import com.cyk.service.TranService;
import com.cyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TranServiceImpl implements TranService {

    @Resource
    private TTranMapper tTranMapper;

    @Resource
    private RedisService redisService;

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
    public PageInfo<TTran> getTranByPage(Integer current, TranQuery query) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 多条件动态筛选（query 为 null 时等价于全量分页）
        List<TTran> list = tTranMapper.selectTranByPage(query);
        return new PageInfo<>(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 插交易 + 插阶段历史两步必须同生同死
    public int saveTran(TranQuery tranQuery) {
        TTran tTran = new TTran();
        BeanUtils.copyProperties(tranQuery, tTran);

        // Generate tranNo: TR + yyyyMMdd + 4-digit sequence（Redis 原子序列，见 generateTranNo）
        tTran.setTranNo(generateTranNo("TR" + new SimpleDateFormat("yyyyMMdd").format(new Date())));

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

    /**
     * 生成交易流水号：TR + yyyyMMdd + 4 位当日序列（如 TR202609080001）。
     *
     * <p><b>并发修复</b>：旧实现「SELECT MAX(tran_no) 再 +1」是典型的
     * <i>read-then-write</i> 竞态——两个并发请求可能读到同一 MAX 值，生成重复流水号。
     * 现改用 Redis {@code INCR} 原子自增：单命令完成「读取并递增」，多实例/多线程下天然不重号。</p>
     *
     * <p><b>冷启动对齐</b>：Redis 序列 key 可能因过期/重启而缺失，直接 INCR 会从 1 开始，
     * 与 DB 已有当日流水号冲突。故 key 不存在时，先用 DB 当日最大序列号回填 Redis 再自增，
     * 保证「Redis 丢失也能从数据库真相自愈」——与本项目 AI 付费墙缓存一致的 SSOT 思路。</p>
     *
     * <p><b>降级兜底</b>：若 Redis 整体不可用（连接异常），回退到原 DB MAX+1 方式，
     * 保证交易创建这一核心写操作不因缓存故障而不可用（可用性优先，记 warn 日志）。</p>
     *
     * @param datePrefix 形如 TR20260908
     * @return 完整流水号
     */
    private String generateTranNo(String datePrefix) {
        String seqKey = Constants.REDIS_TRAN_NO_SEQ_KEY + datePrefix;
        try {
            // 冷启动/过期后 key 不存在：用 DB 当日最大值回填，避免序列从 1 重启造成重号
            if (!Boolean.TRUE.equals(redisService.hasKey(seqKey))) {
                int dbSeq = readMaxSeqFromDb(datePrefix);
                // SETNX + EX：并发回填互不覆盖，首个写入者定初值（与 AI 付费墙回填同一防竞态范式）
                redisService.setValueIfAbsent(seqKey, String.valueOf(dbSeq),
                        Constants.TRAN_NO_SEQ_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            // INCR 原子自增，返回自增后的当日序号
            Long seq = redisService.incr(seqKey);
            if (seq != null) {
                return datePrefix + String.format("%04d", seq);
            }
        } catch (Exception e) {
            // Redis 不可用时不能阻断核心交易创建，降级到 DB 方式并告警
            log.warn("交易流水号 Redis 序列不可用，降级为 DB 方式生成 | datePrefix={}, err={}",
                    datePrefix, e.getMessage());
        }
        // 降级/兜底：DB MAX+1（并发下有重号概率，属可接受的可用性折中）
        return datePrefix + String.format("%04d", readMaxSeqFromDb(datePrefix) + 1);
    }

    /**
     * 从数据库读取当日已有流水号的最大序号，用于 Redis 序列冷启动对齐。
     * 解析失败（历史脏数据/格式不符）时返回 0，视为当日无流水。
     */
    private int readMaxSeqFromDb(String datePrefix) {
        String maxTranNo = tTranMapper.selectMaxTranNoByDate(datePrefix);
        // 流水号定长 14 位（TR20260908 0001），序号位于第 10 位之后
        if (maxTranNo != null && maxTranNo.length() >= 12) {
            try {
                return Integer.parseInt(maxTranNo.substring(10));
            } catch (NumberFormatException ignore) {
                log.warn("交易流水号解析异常，按 0 处理 | maxTranNo={}", maxTranNo);
            }
        }
        return 0;
    }

    @Override
    public TTran getTranById(Integer id) {
        return tTranMapper.selectById(id);
    }

    @Override
    public int updateTran(TranQuery tranQuery) {
        TTran tTran = new TTran();
        BeanUtils.copyProperties(tranQuery, tTran);
        // 【安全声明落地】流水号是交易的业务主键（系统按 Redis 原子序列生成），
        // 绝不允许由编辑接口传入修改；TranQuery 新增 tranNo 字段仅为列表检索服务，
        // 此处显式置空确保 copyProperties 不会把外传值带进 UPDATE 语句。
        tTran.setTranNo(null);
        Integer userId = JWTUtils.parseUserFromJWT(tranQuery.getToken()).getId();
        tTran.setEditBy(userId);
        tTran.setEditTime(new Date());
        return tTranMapper.updateByPrimaryKeySelective(tTran);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 删备注 + 删历史 + 删交易三步原子，避免孤儿记录
    public int deleteTran(Integer id) {
        tTranRemarkMapper.deleteByTranId(id);
        tTranHistoryMapper.deleteByTranId(id);
        return tTranMapper.deleteByPrimaryKey(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 整批删除原子回滚，中途失败不留半截脏数据
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
    @Transactional(rollbackFor = Exception.class) // 改交易阶段 + 记历史两步原子
    public void changeStage(Integer tranId, Integer stage, BigDecimal money, Date expectedDate, String token) {
        TTran tTran = tTranMapper.selectByPrimaryKey(tranId);
        if (tTran == null) {
            throw new BusinessException("交易不存在");
        }

        // Validate stage adjacency
        TDicValue currentStage = tDicValueMapper.selectByPrimaryKey(tTran.getStage());
        TDicValue targetStage = tDicValueMapper.selectByPrimaryKey(stage);
        if (currentStage == null || targetStage == null) {
            throw new BusinessException("阶段数据异常");
        }
        if (Math.abs(currentStage.getOrder() - targetStage.getOrder()) != 1) {
            throw new BusinessException("只能变更为相邻阶段");
        }

        // 【业务规则：终态禁止再推进】
        // 「05付款成交」「06丢失关闭」是流程的两个终局（order 6/7 为字典最大值两位）：
        //   ① 当前非终态 → 向前推进放行（含“推进至成交”这一正常终局动作）；
        //   ② 当前已处终态、方向向前 → 拒绝——已成交的单不应再被改为“丢失关闭”
        //      （两者是互斥终局结果，已成交后再标丢失属业务错误）；
        //   ③ 方向回退不受此限制（纠错/重开丢失单），由既有「相邻阶段」校验承接。
        // 注：相邻校验 |Δorder|=1 已保证向前推进只可能是 order+1，无需另拦跨级跳跃。
        boolean forward = targetStage.getOrder() > currentStage.getOrder();
        if (forward && isTerminalStage(currentStage.getOrder())) {
            throw new BusinessException("交易已处于终态「" + currentStage.getTypeValue() + "」，不能再向前推进；如需纠错请使用回退");
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

    /**
     * 判断阶段 order 是否为流程终态。
     *
     * <p>终态＝字典 stage 中 order 最大的 {@link Constants#TRAN_TERMINAL_STAGE_DEPTH} 个值
     * （当前数据：6=05付款成交、7=06丢失关闭）。之所以用「最大值前 N 位」而非写死 6/7：
     * 字典由管理员在「字典数据」页可维护，若未来新增阶段（如 order=8 售后回访），
     * 终态语义应随字典自动漂移。</p>
     *
     * <p>实现：复用既有 {@code selectByTypeCode("stage")} 实时查询（阶段字典仅 6 行，
     * 开销可忽略；不引入缓存，字典改动即时生效）。</p>
     */
    private boolean isTerminalStage(Integer order) {
        if (order == null) {
            return false;
        }
        List<TDicValue> stages = tDicValueMapper.selectByTypeCode("stage");
        if (stages == null || stages.isEmpty()) {
            return false;
        }
        List<Integer> orders = stages.stream()
                .map(TDicValue::getOrder)
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.reverseOrder())
                .limit(Constants.TRAN_TERMINAL_STAGE_DEPTH)
                .toList();
        return orders.contains(order);
    }

    @Override
    public List<TTranHistory> getHistoryByTranId(Integer tranId) {
        return tTranHistoryMapper.selectByTranId(tranId);
    }

    @Override
    public PageInfo<TTran> getTransByCustomerId(Integer current, Integer customerId) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 客户详情页只看该客户的交易，复用同一条件对象（仅锁定 customerId）
        TranQuery query = new TranQuery();
        query.setCustomerId(customerId);
        List<TTran> list = tTranMapper.selectTranByPage(query);
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
        // 客户下拉需要全量数据，故传 null（selectCustomerPage 已兼容 null：退化为无筛选查询）
        List<TCustomer> customers = tCustomerMapper.selectCustomerPage(null);
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
