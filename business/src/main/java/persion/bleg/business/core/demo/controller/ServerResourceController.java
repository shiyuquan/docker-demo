package persion.bleg.business.core.demo.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static persion.bleg.business.constants.DefalutConstant.DEFAULT_API_PREFIX;

/**
 * 服务器资源控制器
 *
 * @author shiyuquan
 * @since 2020/10/10 5:30 下午
 */
@Api(tags = "测试用户接口")
@RestController
@RequestMapping(DEFAULT_API_PREFIX + "/user")
public class ServerResourceController {}
