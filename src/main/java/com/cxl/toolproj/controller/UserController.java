package com.cxl.toolproj.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.cxl.toolproj.UserMapper.UserMapper;
import com.cxl.toolproj.annotion.AuthToken;
import com.cxl.toolproj.entity.User;
import com.cxl.toolproj.model.ResponseTemplate;
import com.cxl.toolproj.utils.ConstantKit;
import com.cxl.toolproj.utils.Md5TokenGenerator;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/v1/")
public class UserController {
    @Autowired
    Md5TokenGenerator tokenGenerator;
    @Autowired
    UserMapper userMapper;

    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ApiOperation("用户登录接口")
    public ResponseTemplate login(@RequestBody(required = false) JSONObject userInfo){
        String username = userInfo.getString("username");
        String password = userInfo.getString("password");

        List<User> users = userMapper.selectList(new EntityWrapper<User>()
                .eq("username", username)
                .eq("password", password));
        User currentUser = users.get(0);
        com.alibaba.fastjson.JSONObject result = new com.alibaba.fastjson.JSONObject();
        if (currentUser != null) {

            Jedis jedis = new Jedis("localhost", 6379);
            String token = tokenGenerator.generate(username, password);
            jedis.set(username, token);
            jedis.expire(username, ConstantKit.TOKEN_EXPIRE_TIME);
            jedis.set(token, username);
            jedis.expire(token, ConstantKit.TOKEN_EXPIRE_TIME);
            Long currentTime = System.currentTimeMillis();
            jedis.set(token + username, currentTime.toString());

            //用完关闭
            jedis.close();

            result.put("status", "登录成功");
            result.put("token", token);
        } else {
            result.put("status", "登录失败");
        }

        return ResponseTemplate.builder()
                .code(200)
                .message("登录成功")
                .data(result)
                .build();

    }
    @ApiOperation("测试token接口")
    @RequestMapping(value = "test", method = RequestMethod.GET)
    @AuthToken
    public ResponseTemplate test() {
        List<User> user = new User().selectAll();
        return ResponseTemplate.builder()
                .code(200)
                .message("Success")
                .data(user)
                .build();
    }


}
