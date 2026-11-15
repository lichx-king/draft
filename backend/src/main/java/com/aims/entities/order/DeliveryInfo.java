package com.aims.entities.order;

import java.io.Serializable;

public record DeliveryInfo(String customerName, String phone, String email, String address, String province)
        implements Serializable {
}
