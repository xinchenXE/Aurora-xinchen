-- ============================================================================
-- 队列项 006：操作日志/异常日志保留 90 天清理任务登记
-- 日期：2026-09-30
-- 拍板口径：t_operation_log 与 t_exception_log 统一保留 90 天
--           （CommonConstant.LOG_RETENTION_DAYS = 90）
-- 幂等：重复执行不会产生重复行（按 job_name 判存）
-- 依赖：AuroraQuartz.clearExpiredLogs()（本队列项新增）
-- ============================================================================

-- 登记清理任务：每日 03:00 执行（避开既有任务：00:00 统计访问量 + 清 job 日志、01:00 清 redis 访客）
-- job_group 必须与既有任务一致（「默认」）；漏写会落 DEFAULT 组，管理端操作必失败（ISS-026）。
INSERT INTO `t_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_time`, `update_time`)
SELECT '清理过期操作/异常日志', '默认', 'auroraQuartz.clearExpiredLogs', '0 0 3 * * ?', 3, 1, 1, NOW(), NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `t_job` WHERE `invoke_target` = 'auroraQuartz.clearExpiredLogs'
);

-- 核对（应返回 1 行，job_group 为「默认」）
-- SELECT id, job_name, job_group, invoke_target, cron_expression, status FROM `t_job`
--  WHERE invoke_target = 'auroraQuartz.clearExpiredLogs';
