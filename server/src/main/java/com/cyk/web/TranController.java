package com.cyk.web;

import com.cyk.model.TTran;
import com.cyk.model.TTranHistory;
import com.cyk.model.TTranRemark;
import com.cyk.query.TranQuery;
import com.cyk.query.TranRemarkQuery;
import com.cyk.result.R;
import com.cyk.service.TranService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TranController {

    @Resource
    private TranService tranService;

    // 【安全修复】客户下拉含全量客户姓名，仅新建/编辑交易页面需要，收敛到对应权限
    @PreAuthorize("hasAnyAuthority('tran:add','tran:edit')")
    @GetMapping("/api/customer/options")
    public R customerOptions() {
        return R.OK(tranService.getCustomerOptions());
    }

    /**
     * 交易分页查询（支持多条件筛选）。
     *
     * <p>【功能补强】旧版只允许传 customerId 与 money，且 money 为等值匹配（几乎无法命中），
     * 销售最关心的「按阶段看管道」完全做不到。现接收 TranQuery：</p>
     * <ul>
     *   <li>{@code customerId} —— 按客户筛选（客户详情页复用）</li>
     *   <li>{@code stage}     —— <b>按交易阶段筛选（新增核心能力）</b></li>
     *   <li>{@code tranNo}    —— 流水号模糊搜索（新增）</li>
     *   <li>{@code money}     —— 金额下限（语义由「等于」修正为「不低于」，与活动模块 cost>=budget 对齐）</li>
     * </ul>
     *
     * @param current   页码
     * @param tranQuery 筛选条件（字段全部可选，不传即全量分页）
     */
    @PreAuthorize("hasAuthority('tran:list')")
    @GetMapping("/api/trans")
    public R tranPage(@RequestParam(value = "current", required = false) Integer current,
                      TranQuery tranQuery) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TTran> pageInfo = tranService.getTranByPage(current, tranQuery);
        return R.OK(pageInfo);
    }

    @PreAuthorize("hasAuthority('tran:add')")
    @PostMapping("/api/trans")
    public R addTran(TranQuery tranQuery, @RequestHeader("Authorization") String token) {
        tranQuery.setToken(token);
        int i = tranService.saveTran(tranQuery);
        return i > 0 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('tran:edit')")
    @PutMapping("/api/trans")
    public R editTran(@RequestBody TranQuery tranQuery, @RequestHeader("Authorization") String token) {
        tranQuery.setToken(token);
        int i = tranService.updateTran(tranQuery);
        return i > 0 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('tran:view')")
    @GetMapping("/api/tran/{id}")
    public R loadTran(@PathVariable("id") Integer id) {
        TTran tran = tranService.getTranById(id);
        return R.OK(tran);
    }


    @PreAuthorize("hasAuthority('tran:delete')")
    @DeleteMapping("/api/tran/{id}")
    public R deleteTran(@PathVariable("id") Integer id) {
        int i = tranService.deleteTran(id);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('tran:delete')")
    @DeleteMapping("/api/tran/batch")
    public R deleteTranBatch(@RequestParam("ids") String ids) {
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        int i = tranService.deleteTranBatch(idList);
        return i == idList.size() ? R.OK() : R.FAIL();
    }

    // 【安全修复】阶段推进/回退是交易生命周期的核心写操作（直接影响成交金额统计），
    // 旧版任意登录用户可篡改任意交易阶段；收敛到 tran:edit。
    // 同时把业务语义失败（非相邻阶段等）改为 BusinessException 通道，不再用裸 RuntimeException。
    @PreAuthorize("hasAuthority('tran:edit')")
    @PostMapping("/api/tran/stage")
    public R changeStage(@RequestParam("tranId") Integer tranId,
                         @RequestParam("stage") Integer stage,
                         @RequestParam(value = "money", required = false) java.math.BigDecimal money,
                         // 【实测 bug 修复】前端 el-date-picker 的 value-format="YYYY-MM-DD HH:mm:ss"，
                         // 传的是 yyyy-MM-dd HH:mm:ss 字符串；旧版 @RequestParam Date 无 @DateTimeFormat
                         // 时 Spring 无法绑定，抛 MethodArgumentTypeMismatchException → 500（已实测复现）。
                         // 此处与前端 value-format 严格对齐，保证阶段变更时的预计成交日期能正确落库。
                         @RequestParam(value = "expectedDate", required = false)
                         @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                         java.util.Date expectedDate,
                         @RequestHeader("Authorization") String token) {
        tranService.changeStage(tranId, stage, money, expectedDate, token);
        return R.OK();
    }

    @PreAuthorize("hasAuthority('tran:view')")
    @GetMapping("/api/tran/{tranId}/history")
    public R getHistory(@PathVariable("tranId") Integer tranId) {
        List<TTranHistory> list = tranService.getHistoryByTranId(tranId);
        return R.OK(list);
    }

    @PreAuthorize("hasAuthority('tran:list')")
    @GetMapping("/api/customer/{customerId}/trans")
    public R getTransByCustomer(@PathVariable("customerId") Integer customerId,
                                @RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TTran> pageInfo = tranService.getTransByCustomerId(current, customerId);
        return R.OK(pageInfo);
    }

    // 【安全修复】交易跟进记录读取与查看详情同级，收敛到 tran:view
    @PreAuthorize("hasAuthority('tran:view')")
    @GetMapping("/api/tran/remark")
    public R getRemark(@RequestParam(value = "current", required = false) Integer current,
                       @RequestParam(value = "tranId") Integer tranId) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TTranRemark> pageInfo = tranService.getRemarkByPage(current, tranId);
        return R.OK(pageInfo);
    }

    @PreAuthorize("hasAuthority('tran:edit')")
    @PostMapping("/api/tran/remark")
    public R addRemark(TranRemarkQuery remarkQuery, @RequestHeader("Authorization") String token) {
        remarkQuery.setToken(token);
        int i = tranService.addRemark(remarkQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('tran:edit')")
    @PutMapping("/api/tran/remark")
    public R editRemark(@RequestBody TranRemarkQuery remarkQuery, @RequestHeader("Authorization") String token) {
        remarkQuery.setToken(token);
        int i = tranService.updateRemark(remarkQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('tran:delete')")
    @DeleteMapping("/api/tran/remark/{id}")
    public R deleteRemark(@PathVariable("id") Integer id) {
        int i = tranService.deleteRemark(id);
        return i == 1 ? R.OK() : R.FAIL();
    }
}
