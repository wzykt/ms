package com.wzy.msdemo.service;

import com.wzy.msdemo.dao.OrderDAO;
import com.wzy.msdemo.dao.StockDAO;
import com.wzy.msdemo.entity.Order;
import com.wzy.msdemo.entity.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private StockDAO stockDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public int kill(Integer id) {
//        //根据商品id校验库存
//        Stock stock = stockDAO.checkStock(id);
//        if (stock.getSale().equals(stock.getCount())) {
//            throw new RuntimeException("库存不足");
//        }
//        //扣除库存
//        stock.setSale(stock.getSale() + 1);
//        stockDAO.updateSaleById(stock);
//        //创建订单
//        Order order = new Order();
//        order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
//        orderDAO.createOrder(order);
//        return order.getId();

        //校验Redis中的秒杀商品是否为超时
        if (!redisTemplate.hasKey("kill" + id)) {
            throw new RuntimeException("不在抢购时间内");
        }

        //校验库存
        Stock stock = checkStock(id);
        //扣除库存
        updateSale(stock);
        //创建订单
        return createOrder(stock);
    }

    /**
     * 代码改造
     */

    //校验库存
    private Stock checkStock(Integer id) {
        Stock stock = stockDAO.checkStock(id);
        if (stock.getSale().equals(stock.getCount())) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    //扣除库存
    private void updateSale(Stock stock) {
        //使用乐观锁，就不在代码层面做自增操作了，而是在sql层面做id自增和version自增
        //stock.setSale(stock.getSale() + 1);
        //更新操作通过id 和 version字段判断
        int updateRows = stockDAO.updateSaleById(stock);
        if (updateRows == 0) {
            throw new RuntimeException("采购失败，请重试");
        }
    }

    //创建订单
    private Integer createOrder(Stock stock) {
        Order order = new Order();
        order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
        orderDAO.createOrder(order);
        return order.getId();
    }
}
