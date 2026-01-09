package acs.service.impl;

import acs.service.GroupFileService;
import acs.domain.Group;
import acs.domain.Resource;
import acs.repository.GroupRepository;
import acs.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupFileServiceImpl implements GroupFileService {

    private final GroupRepository groupRepository;
    private final ResourceRepository resourceRepository;

    @Autowired
    public GroupFileServiceImpl(GroupRepository groupRepository, ResourceRepository resourceRepository) {
        this.groupRepository = groupRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional
    public List<Group> loadGroupsFromFile(String filePath) {
        Map<String, List<String>> mapping = loadGroupResourceMapping(filePath);
        List<Group> groups = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
            String groupId = entry.getKey();
            List<String> resourceIds = entry.getValue();

            // 查找或创建组
            Group group = groupRepository.findById(groupId)
                .orElse(new Group(groupId, "Group " + groupId)); // 默认名称
            groupRepository.save(group);

            // 关联资源
            List<Resource> resources = new ArrayList<>();
            for (String resourceId : resourceIds) {
                resourceRepository.findById(resourceId).ifPresent(resources::add);
            }
            group.getResources().addAll(resources);
            groupRepository.save(group);
            groups.add(group);
        }
        return groups;
    }

    @Override
    public Map<String, List<String>> loadGroupResourceMapping(String filePath) {
        Map<String, List<String>> mapping = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // 跳过空行和注释
                }
                String[] parts = line.split(":");
                if (parts.length < 3) {
                    throw new IllegalArgumentException("无效的行格式: " + line);
                }
                String groupId = parts[0].trim();
                String groupName = parts[1].trim();
                String[] resourceArray = parts[2].split(",");
                List<String> resourceIds = new ArrayList<>();
                for (String res : resourceArray) {
                    resourceIds.add(res.trim());
                }
                mapping.put(groupId, resourceIds);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取组文件失败: " + filePath, e);
        }
        return mapping;
    }

    @Override
    public boolean validateGroupFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isReadable(path)) {
                return false;
            }
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(":");
                if (parts.length < 3) {
                    return false;
                }
                // 验证资源ID列表非空
                String[] resourceArray = parts[2].split(",");
                if (resourceArray.length == 0) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}