package persion.bleg.dockerdemo.core.demo.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import persion.bleg.dockerdemo.base.BlegException;
import persion.bleg.dockerdemo.base.IResult;
import persion.bleg.dockerdemo.base.Result;
import persion.bleg.dockerdemo.core.demo.entity.User;
import persion.bleg.dockerdemo.core.demo.service.UserService;
import persion.bleg.dockerdemo.encryptbody.annotation.DecryptBody;

import java.util.List;

import static persion.bleg.dockerdemo.constants.DefalutConstant.DEFAULT_API_PREFIX;

/**
 * @author shiyuquan
 * @since 2019/12/23 2:32 下午
 */
@Api(tags = "测试用户接口")
@RestController
@RequestMapping(DEFAULT_API_PREFIX + "/user")
public class UserController {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @ApiOperation(value = "列表查询")
    @GetMapping(value = "/users")
    public IResult<List<User>> selectUser() {
        return new Result<List<User>>().success(userService.selectUser());
    }

    @ApiOperation(value = "根据名称查询")
    @GetMapping(value = "/user/{name}")
    public IResult<User> selectByName(@PathVariable("name") String name) {
        return new Result<User>().success(userService.selectByName(name));
    }

    @DecryptBody
    @ApiOperation(value = "新增")
    @PostMapping(value = "/user")
    public IResult<Boolean> add(@RequestBody User user) {
        return new Result<Boolean>().success(userService.add(user));
    }

    @ApiOperation(value = "用户上传图片")
    @PostMapping(value = "/{id}/uploadImage")
    public IResult<Boolean> addImage(@PathVariable("id") String id,
                                     @RequestParam("file") MultipartFile file) {
        return new Result<Boolean>().success(userService.addImage(id, file));
    }

    @ApiOperation(value = "test")
    @GetMapping(value = "/test")
    public IResult<User> test() {
        throw new BlegException(500, "ssdsd");
    }

    @ApiOperation(value = "test2")
    @PostMapping(value = "/test2")
    public String test2(@RequestParam List<String> ids) {
        return ids.toString();
    }

    public static void main(String[] args) {

        System.err.println("in");
        int a = 1;
        switch (a) {
            case 1:
                System.err.println("1");
            case 2:
                System.err.println("2");
            default:
                System.err.println("def");
        }
        System.err.println("out");
    }

}
