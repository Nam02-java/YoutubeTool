package com.example.speech.aiservice.vn.service.repositoryService.novel;

import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.model.repository.novel.NovelRepository;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Service
public class NovelService {
    @Autowired
    private NovelRepository novelRepository;

    public Novel saveNovel(Novel novel) {
        return novelRepository.save(novel);
    }

    public boolean isNovelExistsByLink(String link) {
        return novelRepository.existsByLink(link);
    }


    public boolean isNovelExists(String title) {
        return novelRepository.existsByTitle(title);
    }

    public Novel findByTitle(String title) {
        return novelRepository.findByTitle(title);
    }

    public List<Novel> findAllNovels() {
        return novelRepository.findAll();
    }
}
