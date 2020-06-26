package com.hb0730.boot.admin.project.monitor.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.hb0730.boot.admin.commons.utils.PageUtils;
import com.hb0730.boot.admin.commons.utils.bean.BeanUtils;
import com.hb0730.boot.admin.project.monitor.job.mapper.ISystemJobLogMapper;
import com.hb0730.boot.admin.project.monitor.job.model.dto.JobLogExportDTO;
import com.hb0730.boot.admin.project.monitor.job.model.entity.SystemJobEntity;
import com.hb0730.boot.admin.project.monitor.job.model.entity.SystemJobLogEntity;
import com.hb0730.boot.admin.project.monitor.job.model.vo.JobLogParams;
import com.hb0730.boot.admin.project.monitor.job.model.vo.SystemJobLogVO;
import com.hb0730.boot.admin.project.monitor.job.service.ISystemJobLogService;
import com.hb0730.boot.admin.project.monitor.job.service.ISystemJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 定时任务日志  服务实现类
 * </p>
 *
 * @author bing_huang
 * @since 2020-04-07
 */
@Service
public class SystemJobLogServiceImpl extends ServiceImpl<ISystemJobLogMapper, SystemJobLogEntity> implements ISystemJobLogService {
    @Autowired
    private ISystemJobService systemJobService;


    @Override
    public PageInfo<SystemJobLogVO> list(Integer page, Integer pageSize, JobLogParams params) {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 10 : pageSize;
        QueryWrapper<SystemJobLogEntity> queryWrapper = getQuery(params);
        PageHelper.startPage(page, pageSize);
        PageInfo<SystemJobLogEntity> pageInfo = new PageInfo<>(super.list(queryWrapper));
        return PageUtils.toBean(pageInfo, SystemJobLogVO.class);
    }

    @Override
    public List<JobLogExportDTO> export(JobLogParams params) {
        QueryWrapper<SystemJobLogEntity> queryWrapper = getQuery(params);
        List<SystemJobLogEntity> entities = super.list(queryWrapper);
        List<JobLogExportDTO> export = BeanUtils.transformFromInBatch(entities, JobLogExportDTO.class);
        if (!CollectionUtils.isEmpty(export)) {
            Set<Long> jobIds = export.parallelStream().map(JobLogExportDTO::getJobId).collect(Collectors.toSet());
            List<SystemJobEntity> jobEntities = systemJobService.listByIds(jobIds);
            Map<Long, String> map = jobEntities.parallelStream().collect(Collectors.toMap(SystemJobEntity::getId, SystemJobEntity::getNumber));
            return export.parallelStream().filter((jobExport) -> map.containsKey(jobExport.getJobId())).peek((jobExport) -> jobExport.setJobNumber(map.get(jobExport.getJobId()))).collect(Collectors.toList());
        }
        return export;
    }

    /**
     * <p>
     * 组装过滤信息
     * </p>
     *
     * @param params 过滤条件
     * @return 查询条件
     */
    public QueryWrapper<SystemJobLogEntity> getQuery(JobLogParams params) {
        QueryWrapper<SystemJobLogEntity> queryWrapper = new QueryWrapper<>();
        if (Objects.nonNull(params)) {
            if (Objects.nonNull(params.getJobId())) {
                queryWrapper.eq(SystemJobLogEntity.JOB_ID, params.getJobId());
            }
            if (Objects.nonNull(params.getJobName())) {
                QueryWrapper<SystemJobEntity> q1 = new QueryWrapper<>();
                q1.like(SystemJobEntity.NAME, params.getJobName());
                List<SystemJobEntity> list = systemJobService.list(q1);
                if (!CollectionUtils.isEmpty(list)) {
                    Set<Long> jobId = list.parallelStream().map(SystemJobEntity::getId).collect(Collectors.toSet());
                    queryWrapper.in(SystemJobLogEntity.JOB_ID, jobId);
                }
            }
            if (Objects.nonNull(params.getStatus())) {
                queryWrapper.eq(SystemJobLogEntity.STATUS, params.getStatus());
            }
            if (Objects.nonNull(params.getStartTime()) && Objects.nonNull(params.getStopTime())) {
                queryWrapper.between(SystemJobLogEntity.CREATE_TIME, params.getStartTime(), params.getStopTime());
            } else if (Objects.nonNull(params.getStartTime())) {
                queryWrapper.ge(SystemJobLogEntity.CREATE_TIME, params.getStartTime());
            } else if (Objects.nonNull(params.getStopTime())) {
                queryWrapper.le(SystemJobLogEntity.CREATE_TIME, params.getStopTime());
            }
        }
        return queryWrapper;
    }
}
