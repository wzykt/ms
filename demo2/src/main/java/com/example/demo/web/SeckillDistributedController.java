package com.example.demo.web;

import com.example.demo.common.redis.RedisUtil;
import com.example.demo.entity.Result;
import com.example.demo.queue.redis.RedisSender;
import com.example.demo.service.ISeckillDistributedService;
import com.example.demo.service.ISeckillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Api(tags = "分布式秒杀")
@RestController
@RequestMapping("/seckillDistributed")
public class SeckillDistributedController {
    private final static Logger LOGGER = LoggerFactory.getLogger(SeckillDistributedController.class);

    private static int corePoolSize = Runtime.getRuntime().availableProcessors();
    //调整队列数 拒绝服务
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, corePoolSize + 1, 10l, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000));

    @Autowired
    private ISeckillService seckillService;
    @Autowired
    private ISeckillDistributedService seckillDistributedService;
    @Autowired
    private RedisSender redisSender;

    @Autowired
    private RedisUtil redisUtil;

    @ApiOperation(value = "秒杀一(Redisson分布式锁)")
    @PostMapping("/startRedisLock")
    public Result startRedisLock(long seckillId) {
        seckillService.deleteSeckill(seckillId);
        final long killId = seckillId;
        LOGGER.info("开始秒杀一");
        for (int i = 0; i < 1000; i++) {
            final long userId = i;
            Runnable task = () -> {
                Result result = seckillDistributedService.startSeckilRedisLock(killId, userId);
                LOGGER.info("用户:{}{}", userId, result.get("msg"));
            };
            executor.execute(task);
        }
        try {
            Thread.sleep(15000);
            Long seckillCount = seckillService.getSeckillCount(seckillId);
            LOGGER.info("一共秒杀出{}件商品", seckillCount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.ok();
    }

    @ApiOperation(value = "秒杀二(zookeeper分布式锁)")
    @PostMapping("/startZkLock")
    public Result startZkLock(long seckillId) {
        seckillService.deleteSeckill(seckillId);
        final long killId = seckillId;
        LOGGER.info("开始秒杀二");
        for (int i = 0; i < 10000; i++) {
            final long userId = i;
            Runnable task = () -> {
                Result result = seckillDistributedService.startSeckilZksLock(killId, userId);
                LOGGER.info("用户:{}{}", userId, result.get("msg"));
            };
            executor.execute(task);
        }
        try {
            Thread.sleep(10000);
            Long seckillCount = seckillService.getSeckillCount(seckillId);
            LOGGER.info("一共秒杀出{}件商品", seckillCount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.ok();
    }

    @ApiOperation(value = "秒杀三(Redis分布式队列-订阅监听)")
    @PostMapping("/startRedisQueue")
    public Result startRedisQueue(long seckillId) {
        redisUtil.cacheValue(seckillId + "", null);//秒杀结束
        seckillService.deleteSeckill(seckillId);
        final long killId = seckillId;
        LOGGER.info("开始秒杀三");
        for (int i = 0; i < 1000; i++) {
            final long userId = i;
            Runnable task = () -> {
                if (redisUtil.getValue(killId + "") == null) {
                    //思考如何返回给用户信息ws
                    redisSender.sendChannelMess("seckill", killId + ";" + userId);
                } else {
                    //秒杀结束
                }
            };
            executor.execute(task);
        }
        try {
            Thread.sleep(10000);
            redisUtil.cacheValue(killId + "", null);
            Long seckillCount = seckillService.getSeckillCount(seckillId);
            LOGGER.info("一共秒杀出{}件商品", seckillCount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.ok();
    }

    @ApiOperation(value = "秒杀六(Redis原子递减)")
    @PostMapping("/startRedisCount")
    public Result startRedisCount(long secKillId) {
        /**
         * 还原数据
         */
        seckillService.deleteSeckill(secKillId);
        int count = 10000;
        /**
         初始化商品个数
         */
        redisUtil.cacheValue(secKillId + "-num", 100);
        final long killId = secKillId;
        LOGGER.info("开始秒杀六");
        for (int i = 0; i < count; i++) {
            final long userId = i;
            Runnable task = () -> {
                /**
                 * 原子递减
                 */
                long number = redisUtil.decr(secKillId + "-num", 1);
                if (number >= 0) {
                    seckillService.startSeckilDBOCC(secKillId, userId,1);
                    LOGGER.info("用户:{}秒杀商品成功", userId);
                } else {
                    LOGGER.info("用户:{}秒杀商品失败", userId);
                }
            };
            executor.execute(task);
        }
        try {
            Thread.sleep(10000);
            redisUtil.cacheValue(killId + "", null);
            Long secKillCount = seckillService.getSeckillCount(secKillId);
            LOGGER.info("一共秒杀出{}件商品", secKillCount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.ok();
    }
}
