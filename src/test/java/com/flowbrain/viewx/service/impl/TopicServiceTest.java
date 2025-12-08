package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.service.TopicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 话题服务测试类
 */
@SpringBootTest
public class TopicServiceTest {

    @Autowired
    private TopicService topicService;

    @Test
    public void testExtractTopicsFromText() {
        // 测试中文话题
        String text1 = "这是一个关于#编程的视频 #Java #SpringBoot";
        Set<String> topics1 = topicService.extractTopicsFromText(text1);
        assertEquals(3, topics1.size());
        assertTrue(topics1.contains("编程"));
        assertTrue(topics1.contains("Java"));
        assertTrue(topics1.contains("SpringBoot"));

        // 测试英文话题
        String text2 = "Learn #programming with #Java and #Python";
        Set<String> topics2 = topicService.extractTopicsFromText(text2);
        assertEquals(3, topics2.size());
        assertTrue(topics2.contains("programming"));
        assertTrue(topics2.contains("Java"));
        assertTrue(topics2.contains("Python"));

        // 测试混合话题
        String text3 = "#前端开发 #Frontend #Vue3 #TypeScript";
        Set<String> topics3 = topicService.extractTopicsFromText(text3);
        assertEquals(4, topics3.size());
        assertTrue(topics3.contains("前端开发"));
        assertTrue(topics3.contains("Frontend"));
        assertTrue(topics3.contains("Vue3"));
        assertTrue(topics3.contains("TypeScript"));

        // 测试空文本
        String text4 = "";
        Set<String> topics4 = topicService.extractTopicsFromText(text4);
        assertEquals(0, topics4.size());

        // 测试null
        Set<String> topics5 = topicService.extractTopicsFromText(null);
        assertEquals(0, topics5.size());

        // 测试没有话题的文本
        String text6 = "这是一个普通的文本，没有任何话题标签";
        Set<String> topics6 = topicService.extractTopicsFromText(text6);
        assertEquals(0, topics6.size());

        // 测试重复话题（应该去重）
        String text7 = "#Java #Java #编程 #Java";
        Set<String> topics7 = topicService.extractTopicsFromText(text7);
        assertEquals(2, topics7.size());
        assertTrue(topics7.contains("Java"));
        assertTrue(topics7.contains("编程"));

        // 测试带数字和下划线的话题
        String text8 = "#Web3_0 #AI_2024 #5G技术";
        Set<String> topics8 = topicService.extractTopicsFromText(text8);
        assertEquals(3, topics8.size());
        assertTrue(topics8.contains("Web3_0"));
        assertTrue(topics8.contains("AI_2024"));
        assertTrue(topics8.contains("5G技术"));
    }

    @Test
    public void testExtractTopicsFromTitleAndDescription() {
        // 模拟视频标题和描述
        String title = "学习Java编程 #Java #编程教程";
        String description = "这是一个关于#SpringBoot的完整教程，适合#初学者学习。\n" +
                "涵盖了#Web开发的各个方面。";

        Set<String> topics = topicService.extractTopicsFromText(title);
        topics.addAll(topicService.extractTopicsFromText(description));

        // 应该提取到5个不同的话题
        assertEquals(5, topics.size());
        assertTrue(topics.contains("Java"));
        assertTrue(topics.contains("编程教程"));
        assertTrue(topics.contains("SpringBoot"));
        assertTrue(topics.contains("初学者"));
        assertTrue(topics.contains("Web开发"));
    }
}
