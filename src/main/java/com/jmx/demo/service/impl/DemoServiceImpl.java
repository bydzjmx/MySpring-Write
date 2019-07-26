package com.jmx.demo.service.impl;

import com.jmx.demo.service.DemoService;

public class DemoServiceImpl implements DemoService {
    @Override
    public String query(String name) {
        return "My name is "+name;
    }
}
