package com.smart.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.entiy.MyOrder;

public interface MyOrderRepository extends JpaRepository<MyOrder, Long> {

	public MyOrder findByOrderId(String orderId);
}
