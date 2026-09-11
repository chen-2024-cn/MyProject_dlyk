package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.exception.BusinessException;
import com.cyk.mapper.TDicTypeMapper;
import com.cyk.mapper.TDicValueMapper;
import com.cyk.model.TDicType;
import com.cyk.model.TDicValue;
import com.cyk.query.DicTypeQuery;
import com.cyk.query.DicValueQuery;
import com.cyk.service.DicTypeService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class DicTypeServiceImpl implements DicTypeService {

    @Resource
    private TDicTypeMapper tDicTypeMapper;

    @Resource
    private TDicValueMapper tDicValueMapper;

    @Override
    public List<TDicType> loadAllDicData() {
        return tDicTypeMapper.selectByAll();
    }

    @Override
    public List<TDicType> getAllTypes() {
        // 目录总览不分页：类型总量有限（十余个），一次性返回保证目录浏览的完整性
        return tDicTypeMapper.selectAllTypes();
    }

    @Override
    public PageInfo<TDicType> getDicTypeByPage(Integer current, DicTypeQuery query) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        List<TDicType> list = tDicTypeMapper.selectDicTypeByPage(query);
        return new PageInfo<>(list);
    }

    @Override
    public int insertDicType(DicTypeQuery query) {
        validateTypeCode(query.getTypeCode(), null);
        TDicType dicType = new TDicType();
        dicType.setTypeCode(query.getTypeCode());
        dicType.setTypeName(query.getTypeName());
        dicType.setRemark(query.getRemark());
        return tDicTypeMapper.insertSelective(dicType);
    }

    @Override
    public int editDicType(DicTypeQuery query) {
        // 编辑时排除自身 id：只改名称不改 code 不应误报重复
        validateTypeCode(query.getTypeCode(), query.getId());
        TDicType dicType = new TDicType();
        dicType.setId(query.getId());
        dicType.setTypeCode(query.getTypeCode());
        dicType.setTypeName(query.getTypeName());
        dicType.setRemark(query.getRemark());
        return tDicTypeMapper.updateByPrimaryKeySelective(dicType);
    }

    /**
     * 字典类型 code 唯一性前置校验。
     *
     * <p>【修复的既有缺陷】t_dic_type.type_code 库层唯一索引存在，但原版直接写库，
     * 冲突时抛 DuplicateKeyException，被全局处理器返回写死的「邮箱或者电话重复」——
     * 与字典场景毫无关系，严重误导用户。现前置查重返回精准提示。</p>
     */
    private void validateTypeCode(String typeCode, Integer excludeId) {
        if (!StringUtils.hasText(typeCode)) {
            throw new BusinessException("字典类型编码不能为空");
        }
        if (tDicTypeMapper.countByTypeCode(typeCode.trim(), excludeId) > 0) {
            throw new BusinessException("字典类型编码「" + typeCode.trim() + "」已存在，请更换");
        }
    }

    @Override
    public int deleteDicTypeById(Integer id) {
        // 【修复的既有缺陷】原版直接删主记录，但该类型下若有字典值，
        // t_dic_value.type_code 外键为 RESTRICT 会抛异常，接口返回不友好的 500（实测已复现）。
        // 现前置统计字典值数量，仍有值时拒绝删除并明确告知，引导用户先清理字典值。
        TDicType type = tDicTypeMapper.selectByPrimaryKey(id);
        if (type == null) {
            return 0;
        }
        int valueCount = tDicValueMapper.countByTypeCode(type.getTypeCode());
        if (valueCount > 0) {
            throw new BusinessException("该字典类型下仍有 " + valueCount + " 个字典值，请先删除后再操作");
        }
        return tDicTypeMapper.deleteByPrimaryKey(id);
    }

    @Override
    public PageInfo<TDicValue> getDicValueByPage(Integer current, DicValueQuery query) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        List<TDicValue> list = tDicValueMapper.selectDicValueByPage(query);
        return new PageInfo<>(list);
    }

    @Override
    public int insertDicValue(DicValueQuery query) {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode(query.getTypeCode());
        dicValue.setTypeValue(query.getTypeValue());
        dicValue.setOrder(query.getOrder());
        dicValue.setRemark(query.getRemark());
        return tDicValueMapper.insertSelective(dicValue);
    }

    @Override
    public int editDicValue(DicValueQuery query) {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(query.getId());
        dicValue.setTypeCode(query.getTypeCode());
        dicValue.setTypeValue(query.getTypeValue());
        dicValue.setOrder(query.getOrder());
        dicValue.setRemark(query.getRemark());
        return tDicValueMapper.updateByPrimaryKeySelective(dicValue);
    }

    @Override
    public int deleteDicValueById(Integer id) {
        // 字典值被多张业务表外键引用（如线索 state/source、交易 stage、备注 note_way 等）。
        // 若该值已被业务数据使用，物理删除会触发外键 RESTRICT。原版直接抛出后走全局处理
        // 变为「数据库操作失败」。现捕获并转为语义明确的提示，避免用户误判为系统故障。
        try {
            return tDicValueMapper.deleteByPrimaryKey(id);
        } catch (DataAccessException e) {
            log.warn("删除字典值失败（可能被业务数据引用） id={}", id, e);
            throw new BusinessException("该字典值已被业务数据引用，无法删除");
        }
    }
}
