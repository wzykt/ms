package com.wzy.msdemo.dao;

import com.wzy.msdemo.entity.Stock;

public interface StockDAO {

    Stock checkStock(Integer id);

    int updateSaleById(Stock stock);

}
