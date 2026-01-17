package acs.service.impl;

import acs.service.ProfileFileService;
import acs.domain.Profile;
import acs.domain.TimeFilter;
import acs.repository.ProfileRepository;
import acs.repository.TimeFilterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ProfileFileServiceImpl implements ProfileFileService {

    private final ProfileRepository profileRepository;
    private final TimeFilterRepository timeFilterRepository;
    private final ObjectMapper objectMapper;

    public ProfileFileServiceImpl(ProfileRepository profileRepository,
                                  TimeFilterRepository timeFilterRepository) {
        this.profileRepository = profileRepository;
        this.timeFilterRepository = timeFilterRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public List<Profile> loadProfilesFromJson(String filePath) {
        try {
            CollectionType profileListType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Profile.class);
            List<Profile> profiles = objectMapper.readValue(new File(filePath), profileListType);
            for (Profile profile : profiles) {
                profileRepository.save(profile);
            }
            return profiles;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON profile file: " + filePath, e);
        }
    }

    @Override
    @Transactional
    public List<TimeFilter> loadTimeFiltersFromJson(String filePath) {
        try {
            CollectionType filterListType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, TimeFilter.class);
            List<TimeFilter> filters = objectMapper.readValue(new File(filePath), filterListType);
            for (TimeFilter filter : filters) {
                timeFilterRepository.save(filter);
            }
            return filters;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON time filter file: " + filePath, e);
        }
    }

    @Override
    public boolean validateJsonFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isReadable(path)) {
                return false;
            }
            // 尝试解析JSON以验证格式
            objectMapper.readTree(new File(filePath));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
