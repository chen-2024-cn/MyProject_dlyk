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

    @GetMapping("/api/customer/options")
    public R customerOptions() {
        return R.OK(tranService.getCustomerOptions());
    }

    @PreAuthorize("hasAuthority('tran:list')")
    @GetMapping("/api/trans")
    public R tranPage(@RequestParam(value = "current", required = false) Integer current,
                      @RequestParam(value = "customerId", required = false) Integer customerId,
                      @RequestParam(value = "money", required = false) java.math.BigDecimal money) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TTran> pageInfo = tranService.getTranByPage(current, customerId, money);
        return R.OK(pageInfo);
    }

    @PostMapping("/api/trans")
    public R addTran(TranQuery tranQuery, @RequestHeader("Authorization") String token) {
        tranQuery.setToken(token);
        int i = tranService.saveTran(tranQuery);
        return i > 0 ? R.OK() : R.FAIL();
    }

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


    @DeleteMapping("/api/tran/{id}")
    public R deleteTran(@PathVariable("id") Integer id) {
        int i = tranService.deleteTran(id);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @DeleteMapping("/api/tran/batch")
    public R deleteTranBatch(@RequestParam("ids") String ids) {
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        int i = tranService.deleteTranBatch(idList);
        return i == idList.size() ? R.OK() : R.FAIL();
    }

    @PostMapping("/api/tran/stage")
    public R changeStage(@RequestParam("tranId") Integer tranId,
                         @RequestParam("stage") Integer stage,
                         @RequestParam(value = "money", required = false) java.math.BigDecimal money,
                         @RequestParam(value = "expectedDate", required = false) java.util.Date expectedDate,
                         @RequestHeader("Authorization") String token) {
        try {
            tranService.changeStage(tranId, stage, money, expectedDate, token);
            return R.OK();
        } catch (RuntimeException e) {
            return R.FAIL(e.getMessage());
        }
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

    @GetMapping("/api/tran/remark")
    public R getRemark(@RequestParam(value = "current", required = false) Integer current,
                       @RequestParam(value = "tranId") Integer tranId) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TTranRemark> pageInfo = tranService.getRemarkByPage(current, tranId);
        return R.OK(pageInfo);
    }

    @PostMapping("/api/tran/remark")
    public R addRemark(TranRemarkQuery remarkQuery, @RequestHeader("Authorization") String token) {
        remarkQuery.setToken(token);
        int i = tranService.addRemark(remarkQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PutMapping("/api/tran/remark")
    public R editRemark(@RequestBody TranRemarkQuery remarkQuery, @RequestHeader("Authorization") String token) {
        remarkQuery.setToken(token);
        int i = tranService.updateRemark(remarkQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @DeleteMapping("/api/tran/remark/{id}")
    public R deleteRemark(@PathVariable("id") Integer id) {
        int i = tranService.deleteRemark(id);
        return i == 1 ? R.OK() : R.FAIL();
    }
}
