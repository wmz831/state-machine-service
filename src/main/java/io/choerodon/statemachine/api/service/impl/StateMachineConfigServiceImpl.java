package io.choerodon.statemachine.api.service.impl;

import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.service.BaseServiceImpl;
import io.choerodon.statemachine.api.dto.StateMachineConfigDTO;
import io.choerodon.statemachine.api.service.StateMachineConfigService;
import io.choerodon.statemachine.app.assembler.StateMachineConfigAssembler;
import io.choerodon.statemachine.domain.StateMachineConfig;
import io.choerodon.statemachine.domain.StateMachineConfigDraft;
import io.choerodon.statemachine.infra.enums.ConfigType;
import io.choerodon.statemachine.infra.mapper.StateMachineConfigDraftMapper;
import io.choerodon.statemachine.infra.mapper.StateMachineConfigMapper;
import io.choerodon.statemachine.infra.utils.EnumUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author peng.jiang, dinghuang123@gmail.com
 */
@Component
@Transactional(rollbackFor = Exception.class)
public class StateMachineConfigServiceImpl extends BaseServiceImpl<StateMachineConfigDraft> implements StateMachineConfigService {

    @Autowired
    private StateMachineConfigDraftMapper configDraftMapper;
    @Autowired
    private StateMachineConfigMapper configDeployMapper;
    @Autowired
    private StateMachineConfigAssembler stateMachineConfigAssembler;

    private ModelMapper modelMapper = new ModelMapper();

    @Override
    public StateMachineConfigDTO create(Long organizationId, Long stateMachineId, Long transformId, StateMachineConfigDTO configDTO) {
        if (!EnumUtil.contain(ConfigType.class, configDTO.getType())) {
            throw new CommonException("error.status.type.illegal");
        }
        configDTO.setTransformId(transformId);
        configDTO.setOrganizationId(organizationId);
        StateMachineConfigDraft config = modelMapper.map(configDTO, StateMachineConfigDraft.class);
        config.setStateMachineId(stateMachineId);
        int isInsert = configDraftMapper.insert(config);
        if (isInsert != 1) {
            throw new CommonException("error.stateMachineConfig.create");
        }
        config = configDraftMapper.queryById(organizationId, config.getId());
        return modelMapper.map(config, StateMachineConfigDTO.class);
    }

    @Override
    public Boolean delete(Long organizationId, Long configId) {
        StateMachineConfigDraft config = new StateMachineConfigDraft();
        config.setId(configId);
        config.setOrganizationId(organizationId);
        int isDelete = configDraftMapper.delete(config);
        if (isDelete != 1) {
            throw new CommonException("error.stateMachineConfig.delete");
        }
        return true;
    }

    @Override
    public List<StateMachineConfigDTO> queryByTransformId(Long organizationId, Long transformId, String type, Boolean isDraft) {
        if (!EnumUtil.contain(ConfigType.class, type)) {
            throw new CommonException("error.status.type.illegal");
        }

        List<StateMachineConfigDTO> configDTOS;
        if (isDraft) {
            List<StateMachineConfigDraft> configs = configDraftMapper.queryWithCodeInfo(organizationId, transformId, type);
            configDTOS = stateMachineConfigAssembler.toTargetList(configs, StateMachineConfigDTO.class);
        } else {
            List<StateMachineConfig> configs = configDeployMapper.queryWithCodeInfo(organizationId, transformId, type);
            configDTOS = stateMachineConfigAssembler.toTargetList(configs, StateMachineConfigDTO.class);
        }
        return configDTOS;
    }
}
