package persion.bleg.dockerdemo.core.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
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
@Slf4j
@Api(tags = "测试用户接口")
@RestController
@RequestMapping(DEFAULT_API_PREFIX + "/user")
public class UserController {

    // private static final Logger log = LoggerFactory.getLogger("AsyncLogger");

    @Autowired
    private RestTemplate restTemplate;

    private final UserService userService;

    public UserController(UserService userService) {this.userService = userService;}

    /**
     * 查询列表
     *
     * @return {@link List} {@link User}
     */
    @ApiOperation(value = "列表查询")
    @GetMapping(value = "/list")
    public IResult<List<User>> selectUserList() {
        return new Result<List<User>>().success(userService.selectUserList());
    }

    /**
     * 分页查询
     *
     * @param page 页数
     * @param size 页大小
     * @return {@link IPage} {@link User}
     */
    @ApiOperation(value = "分页查询")
    @GetMapping(value = "/page")
    public IResult<IPage<User>> selectUserPage(@RequestParam Integer page, @RequestParam Integer size) {
        return new Result<IPage<User>>().success(userService.selectUserPage(page, size));
    }

    /**
     * 根据id查询
     *
     * @param id 主键
     * @return {@link User}
     */
    @ApiOperation(value = "根据名称查询")
    @GetMapping(value = "/{id}")
    public IResult<User> selectById(@PathVariable("id") Integer id) {
        return new Result<User>().success(userService.selectById(id));
    }

    /**
     * 新增对象
     *
     * @param user user
     * @return {@link Boolean}
     */
    @DecryptBody
    @ApiOperation(value = "新增")
    @PostMapping(value = "")
    public IResult<Boolean> add(@RequestBody User user) {
        return new Result<Boolean>().success(userService.addUser(user));
    }

    /**
     * 修改对象
     *
     * @param user user
     * @return {@link Boolean}
     */
    @ApiOperation(value = "修改")
    @PutMapping(value = "")
    public IResult<Boolean> updateById(@RequestBody User user) {
        return new Result<Boolean>().success(userService.updateUserById(user));
    }

    /**
     * 根据id删除
     *
     * @param id 主键
     * @return {@link Boolean}
     */
    @ApiOperation(value = "根据名称查询")
    @DeleteMapping(value = "/{id}")
    public IResult<Boolean> deleteById(@PathVariable("id") Integer id) {
        return new Result<Boolean>().success(userService.deleteById(id));
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
        log.info("usc - ddd");
        throw new BlegException(500, "ssdsd");
    }

    @ApiOperation(value = "test2")
    @PostMapping(value = "/test2")
    public String test2(@RequestBody String data) {
        return data;
    }

}
