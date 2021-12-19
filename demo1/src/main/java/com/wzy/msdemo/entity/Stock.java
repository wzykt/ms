package com.wzy.msdemo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@ToString
public class Stock {

    private Integer id;
    private String name;
    private Integer count;
    private Integer sale;
    private Integer version;
}
