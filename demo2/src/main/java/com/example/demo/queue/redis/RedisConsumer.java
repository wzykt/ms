package com.example.demo.queue.redis;


import com.example.demo.common.enums.SeckillStatEnum;
import com.example.demo.common.redis.RedisUtil;
import com.example.demo.entity.Result;
import com.example.demo.service.ISeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消费者
 */
@Service
public class RedisConsumer {

    private Logger LOGGER = LoggerFactory.getLogger(RedisConsumer.class);

    @Autowired
    private ISeckillService seckillService;
    @Autowired
    private RedisUtil redisUtil;

    public void receiveMessage(String message) {
        Thread th = Thread.currentThread();
        System.out.println("Tread name:" + th.getName());
        //收到通道的消息之后执行秒杀操作(超卖)
        String[] array = message.split(";");
        if (redisUtil.getValue(array[0]) == null) {//control层已经判断了，其实这里不需要再判断了
            Result result = seckillService.startSeckilDBPCC_TWO(Long.parseLong(array[0]), Long.parseLong(array[1]));
            if (result.equals(Result.ok(SeckillStatEnum.SUCCESS))) {
                LOGGER.info("用户{}秒杀成功", array[1]);
                //WebSocketServer.sendInfo("秒杀成功",array[0]);//推送给前台
            } else {
                LOGGER.info("用户{}秒杀失败", array[1]);
                //WebSocketServer.sendInfo("秒杀失败",array[0]);//推送给前台
                redisUtil.cacheValue(array[0], "ok");//秒杀结束
            }
        } else {
            LOGGER.info("用户{}秒杀失败", array[1]);
            //WebSocketServer.sendInfo("秒杀失败",array[0]);//推送给前台
        }
    }
}