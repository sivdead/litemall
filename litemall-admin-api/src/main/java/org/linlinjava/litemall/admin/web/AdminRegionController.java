package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.admin.vo.RegionVo;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallRegion;
import org.linlinjava.litemall.db.service.LitemallRegionService;
import org.linlinjava.litemall.db.util.DtoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/admin/region")
@Validated
public class AdminRegionController {
    private final Log logger = LogFactory.getLog(AdminRegionController.class);

    @Autowired
    private LitemallRegionService regionService;

    @GetMapping("/clist")
    public Object clist(@NotNull Integer id) {
        List<LitemallRegion> regionList = regionService.queryByPid(id);
        return ResponseUtil.okList(regionList);
    }

    @GetMapping("/list")
    public Object list() {
        List<LitemallRegion> litemallRegions = regionService.queryByRootId(0);
        List<RegionVo> regionVoList = treeFy(litemallRegions);
        return ResponseUtil.okList(regionVoList);
    }

    public List<RegionVo> treeFy(List<LitemallRegion> litemallRegions) {
        if (litemallRegions == null || litemallRegions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, RegionVo> regionMap = new HashMap<>();
        List<RegionVo> rootRegions = DtoUtil.copyList(litemallRegions, RegionVo.class, e -> regionMap.put(e.getId(), e));
        int preSize = rootRegions.size();
        int currentSize = -1;
        while (preSize != currentSize) {
            preSize = rootRegions.size();
            List<RegionVo> grouped = rootRegions.stream()
                    .filter(e -> e.getPid() != null && regionMap.get(e.getPid()) != null)
                    .collect(groupingBy(e -> regionMap.get(e.getPid())))
                    .entrySet()
                    .stream()
                    .map(e -> {
                        e.getKey().setChildren(e.getValue());
                        return e.getKey();
                    }).collect(toList());
            if (grouped.isEmpty()) {
                return rootRegions;
            }
            rootRegions = grouped;
            currentSize = rootRegions.size();
        }
        return rootRegions;
    }
}
