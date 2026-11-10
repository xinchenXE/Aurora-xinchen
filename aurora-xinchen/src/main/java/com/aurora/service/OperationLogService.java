package com.aurora.service;

import com.aurora.model.dto.OperationLogDTO;
import com.aurora.entity.OperationLog;
import com.aurora.model.vo.ConditionVO;
import com.aurora.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.spring.service.IService;

public interface OperationLogService extends IService<OperationLog> {

    PageResultDTO<OperationLogDTO> listOperationLogs(ConditionVO conditionVO);

    /**
     * 清理超过保留期（{@code CommonConstant.LOG_RETENTION_DAYS}）的操作日志，返回删除行数。
     * 队列项 006 拍板口径：保留 90 天。
     */
    int cleanExpiredLogs();

}
