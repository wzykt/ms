package com.wzy.msdemo.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.wzy.msdemo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RequestMapping("stock")
@RestController
@Slf4j
public class StockController {

    @Autowired
    private OrderService orderService;

    //创建令牌桶实例
    private RateLimiter rateLimiter = RateLimiter.create(20);

//    @GetMapping("/sale")
//    private String sale(Integer id){
//        //1.没有获取到token，请求一直阻塞，直到获取token
//        //log.info("等待的时间 {}",rateLimiter.acquire());
//        //2.设置一个等待时间，如果在等待的时间内获取到token令牌去处理业务，如果在等待的时间内没有获取到token则抛弃，不进行业务处理
//        if(rateLimiter.tryAcquire(5, TimeUnit.SECONDS)){
//            System.out.println("当前请求被限流，无法调用后续秒杀逻辑");
//        }
//        System.out.println("处理结果------------");
//        return "test";
//    }


//    下单方法
//    @GetMapping("/kill")
//    public String kill(Integer id) {
//        System.out.println("秒杀商品的id : " + id);
//        try {
//            synchronized (this){
//                int orderId = orderService.kill(id);
//                return "秒杀成功，订单id为 :" + String.valueOf(orderId);
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//            return e.getMessage();
//        }
//    }

    //下单方法，使用乐观锁防止超卖
    @GetMapping("/kill")
    public String kill(Integer id) {
        System.out.println("秒杀商品的id : " + id);
        try {
            int orderId = orderService.kill(id);
            return "秒杀成功，订单id为 :" + String.valueOf(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    //开发秒杀方法，乐观锁防止超卖+令牌桶算法限流
    //乐观锁+令牌桶算法可能并不会让库存消耗完全，拿到token的请求可能因为乐观锁而被摒弃
    @GetMapping("/killToken")
    public String killToken(Integer id) {
        System.out.println("秒杀商品的id : " + id);
        //加入令牌桶的限流措施
        if (!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)) {
            log.info("订单被抛弃");
            return "抢购失败，秒杀活动过于火爆";
        }
        try {
            int orderId = orderService.kill(id);
            return "秒杀成功，订单id为 :" + String.valueOf(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

}
