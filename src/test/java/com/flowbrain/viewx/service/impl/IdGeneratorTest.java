package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.util.IdGenerator;

public class IdGeneratorTest {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(IdGenerator.nextId());
        }
    }
}
